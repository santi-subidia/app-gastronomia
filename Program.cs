using System.Security.Claims;
using System.Text;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.IdentityModel.Tokens;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Infrastructure.Data.Seeds;
using ApiGastronomia.Models;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Hubs;
using ApiGastronomia.Services.Interfaces;
using Scalar.AspNetCore;
using Microsoft.AspNetCore.Authorization;
using Microsoft.OpenApi;
using System.Threading.RateLimiting;

var builder = WebApplication.CreateBuilder(args);

// =============================================
// 1. Controladores + OpenAPI
// =============================================
builder.Services.AddControllers()
    .AddJsonOptions(options =>
    {
        // Evita ciclos de referencia en la serialización JSON
        options.JsonSerializerOptions.ReferenceHandler = System.Text.Json.Serialization.ReferenceHandler.IgnoreCycles;
        options.JsonSerializerOptions.DefaultIgnoreCondition = System.Text.Json.Serialization.JsonIgnoreCondition.WhenWritingNull;
    });

builder.Services.AddOpenApi(options =>
{
    // DocumentTransformer: define el security scheme "Bearer" en components/securitySchemes
    options.AddDocumentTransformer((document, context, ct) =>
    {
        document.Components ??= new OpenApiComponents();
        document.Components.SecuritySchemes ??= new Dictionary<string, IOpenApiSecurityScheme>();
        document.Components.SecuritySchemes["Bearer"] = new OpenApiSecurityScheme
        {
            Type = SecuritySchemeType.Http,
            Scheme = "bearer",
            BearerFormat = "JWT",
            Description = "Token JWT del endpoint /api/auth/login"
        };
        return Task.CompletedTask;
    });

    // OperationTransformer: agrega security requirement solo a endpoints con [Authorize]
    options.AddOperationTransformer((operation, context, ct) =>
    {
        var hasAuthorize = context.Description.ActionDescriptor.EndpointMetadata
            .OfType<AuthorizeAttribute>()
            .Any();

        if (!hasAuthorize) return Task.CompletedTask;

        operation.Security ??= new List<OpenApiSecurityRequirement>();
        operation.Security.Add(new OpenApiSecurityRequirement
        {
            [new OpenApiSecuritySchemeReference("Bearer")] = new List<string>()
        });

        return Task.CompletedTask;
    });
});

// =============================================
// 2. Entity Framework Core -> PostgreSQL
// =============================================
builder.Services.AddDbContext<AppDbContext>(options =>
    options.UseNpgsql(builder.Configuration.GetConnectionString("Postgres"))
);

// =============================================
// 3. Redis -> Caché de posiciones GPS
// =============================================
builder.Services.AddStackExchangeRedisCache(options =>
{
    options.Configuration = builder.Configuration.GetConnectionString("Redis");
    options.InstanceName = "ApiGastronomia_";
});

// También registramos IConnectionMultiplexer para uso avanzado si se necesita
builder.Services.AddSingleton<StackExchange.Redis.IConnectionMultiplexer>(sp =>
{
    var config = builder.Configuration.GetConnectionString("Redis")
        ?? throw new InvalidOperationException("Redis connection string no configurada.");
    return StackExchange.Redis.ConnectionMultiplexer.Connect(config);
});

// =============================================
// 4. SignalR + Redis backplane
// =============================================
var redisConnection = builder.Configuration.GetConnectionString("Redis")
    ?? throw new InvalidOperationException("Redis connection string no configurada.");

builder.Services.AddSignalR()
    .AddStackExchangeRedis(redisConnection, options =>
    {
        options.Configuration.ChannelPrefix = StackExchange.Redis.RedisChannel.Literal("SignalR");
    });

// =============================================
// 4b. Autenticación JWT
// =============================================
var jwtSettings = new JwtSettings();
builder.Configuration.GetSection("JwtSettings").Bind(jwtSettings);
builder.Services.AddSingleton(jwtSettings);

builder.Services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
    .AddJwtBearer(options =>
    {
        options.TokenValidationParameters = new TokenValidationParameters
        {
            ValidateIssuer = true,
            ValidateAudience = true,
            ValidateLifetime = true,
            ValidateIssuerSigningKey = true,
            ValidIssuer = jwtSettings.Issuer,
            ValidAudience = jwtSettings.Audience,
            IssuerSigningKey = new SymmetricSecurityKey(
                Encoding.UTF8.GetBytes(jwtSettings.SecretKey))
        };

        // SignalR envía el JWT como query string ?access_token= porque
        // los navegadores no soportan headers personalizados en WebSocket handshake
        options.Events = new JwtBearerEvents
        {
            OnMessageReceived = context =>
            {
                var accessToken = context.Request.Query["access_token"];
                var path = context.HttpContext.Request.Path;
                if (!string.IsNullOrEmpty(accessToken) && path.StartsWithSegments("/hubs"))
                {
                    context.Token = accessToken;
                }
                return Task.CompletedTask;
            },

            // Evita redirect 302 en rutas SignalR — devuelve 401 directo
            // sin interferir con el comportamiento de los controllers REST
            OnChallenge = context =>
            {
                if (context.Request.Path.StartsWithSegments("/hubs"))
                {
                    context.HandleResponse();
                    context.Response.StatusCode = 401;
                    context.Response.ContentType = "application/json";
                }
                return Task.CompletedTask;
            }
        };
    });

// =============================================
// 4c. Rate Limiting — protección contra abuso
// =============================================
builder.Services.AddRateLimiter(options =>
{
    options.RejectionStatusCode = 429;

    options.GlobalLimiter = PartitionedRateLimiter.Create<HttpContext, string>(ctx =>
    {
        if (HttpMethods.IsOptions(ctx.Request.Method))
            return RateLimitPartition.GetNoLimiter("OPTIONS");

        var key = ctx.User.FindFirstValue("sub")
            ?? ctx.Connection.RemoteIpAddress?.ToString()
            ?? "unknown";

        return RateLimitPartition.GetSlidingWindowLimiter(key, _ =>
            new SlidingWindowRateLimiterOptions
            {
                PermitLimit = 100,
                Window = TimeSpan.FromMinutes(1),
                SegmentsPerWindow = 2,
                QueueProcessingOrder = QueueProcessingOrder.OldestFirst,
                QueueLimit = 0
            });
    });

    options.AddPolicy<string>("LoginPolicy", ctx =>
    {
        if (HttpMethods.IsOptions(ctx.Request.Method))
            return RateLimitPartition.GetNoLimiter("OPTIONS");

        var key = ctx.Connection.RemoteIpAddress?.ToString() ?? "unknown";
        return RateLimitPartition.GetFixedWindowLimiter(key, _ =>
            new FixedWindowRateLimiterOptions
            {
                PermitLimit = 10,
                Window = TimeSpan.FromMinutes(1),
                QueueProcessingOrder = QueueProcessingOrder.OldestFirst,
                QueueLimit = 0
            });
    });

    options.OnRejected = async (context, cancellationToken) =>
    {
        context.HttpContext.Response.StatusCode = 429;
        context.HttpContext.Response.ContentType = "application/json";
        if (context.Lease.TryGetMetadata(MetadataName.RetryAfter, out var retryAfter))
        {
            context.HttpContext.Response.Headers["Retry-After"] = retryAfter.TotalSeconds.ToString("F0");
        }
        var message = System.Text.Json.JsonSerializer.Serialize(new
        {
            Mensaje = $"Demasiadas solicitudes. Intente nuevamente en {retryAfter.TotalSeconds:F0} segundos."
        });
        await context.HttpContext.Response.WriteAsync(message, cancellationToken);
    };
});

// =============================================
// 5. Inyección de Dependencias - Servicios
// =============================================
builder.Services.AddScoped<IPedidoService, PedidoService>();
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IUsuarioService, UsuarioService>();
builder.Services.AddScoped<IProductoService, ProductoService>();
builder.Services.AddScoped<IConfiguracionService, ConfiguracionService>();
builder.Services.AddScoped<IDemoraService, DemoraService>();
builder.Services.AddScoped<ICajaService, CajaService>();
builder.Services.AddHttpContextAccessor();
builder.Services.AddScoped<RoleSeedService>();
builder.Services.AddScoped<UserSeedService>();
builder.Services.AddScoped<MetodoVentaSeedService>();
builder.Services.AddScoped<MetodoPagoSeedService>();
builder.Services.AddScoped<EstadoPedidoSeedService>();
builder.Services.AddScoped<ProductoSeedService>();

// =============================================
// 6. CORS (abierto para desarrollo; restringir en producción)
// =============================================
builder.Services.AddCors(options =>
{
    options.AddPolicy("DevCors", policy =>
    {
        policy.AllowAnyOrigin()
              .AllowAnyMethod()
              .AllowAnyHeader();
    });

    options.AddPolicy("SignalRCors", policy =>
    {
        policy.SetIsOriginAllowed(_ => true)
              .AllowAnyMethod()
              .AllowAnyHeader()
              .AllowCredentials();
    });
});

var app = builder.Build();

// =============================================
// Seed de datos (solo cuando Database:RunSeeds = true)
// =============================================
if (app.Configuration.GetValue<bool>("Database:RunSeeds"))
{
    using var scope = app.Services.CreateScope();
    var roleSeeder = scope.ServiceProvider.GetRequiredService<RoleSeedService>();
    var userSeeder = scope.ServiceProvider.GetRequiredService<UserSeedService>();
    var metodoVentaSeeder = scope.ServiceProvider.GetRequiredService<MetodoVentaSeedService>();
    var metodoPagoSeeder = scope.ServiceProvider.GetRequiredService<MetodoPagoSeedService>();
    var estadoPedidoSeeder = scope.ServiceProvider.GetRequiredService<EstadoPedidoSeedService>();
    var productoSeeder = scope.ServiceProvider.GetRequiredService<ProductoSeedService>();
    var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();

    logger.LogInformation("Ejecutando seeds...");
    await roleSeeder.SeedAsync();
    await userSeeder.SeedAsync();
    await metodoVentaSeeder.SeedAsync();
    await metodoPagoSeeder.SeedAsync();
    await estadoPedidoSeeder.SeedAsync();
    await productoSeeder.SeedAsync();
    logger.LogInformation("Seeds completados");
}

// =============================================
// Pipeline HTTP
// =============================================
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    // Scalar: API fluida con persistencia del token JWT
    //   - AddPreferredSecuritySchemes: preselecciona "Bearer" en la UI
    //   - EnablePersistentAuthentication: guarda el token en localStorage
    //     Lo pegás UNA VEZ y se reusa en todos los endpoints y sesiones
    app.MapScalarApiReference(options => options
        .AddPreferredSecuritySchemes("Bearer")
        .EnablePersistentAuthentication()
    );
    app.UseCors("DevCors");
}

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();
app.UseRateLimiter();

// Mapeo de controladores REST
app.MapControllers();

// Mapeo del Hub de SignalR
app.MapHub<LogisticaHub>("/hubs/logistica");

app.Run();

// Expone Program para WebApplicationFactory en tests de integración
public partial class Program { }
