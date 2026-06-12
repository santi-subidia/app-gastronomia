using System.Text;
using Microsoft.EntityFrameworkCore;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.IdentityModel.Tokens;
using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Infrastructure.Data.Seeds;
using ApiGastronomia.Models;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Hubs;
using ApiGastronomia.Services.Interfaces;
using Scalar.AspNetCore;

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

builder.Services.AddOpenApi();

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
// 4. SignalR
// =============================================
builder.Services.AddSignalR();

// =============================================
// 4b. Autenticación JWT
// =============================================
var jwtSettings = new JwtSettings();
builder.Configuration.GetSection("JwtSettings").Bind(jwtSettings);

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
    });

// =============================================
// 5. Inyección de Dependencias - Servicios
// =============================================
builder.Services.AddScoped<IPedidoService, PedidoService>();
builder.Services.AddScoped<IAuthService, AuthService>();
builder.Services.AddScoped<IUsuarioService, UsuarioService>();
builder.Services.AddScoped<RoleSeedService>();
builder.Services.AddScoped<UserSeedService>();
builder.Services.AddScoped<MetodoVentaSeedService>();
builder.Services.AddScoped<MetodoPagoSeedService>();
builder.Services.AddScoped<EstadoPedidoSeedService>();

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
    var logger = scope.ServiceProvider.GetRequiredService<ILogger<Program>>();

    logger.LogInformation("Ejecutando seeds...");
    await roleSeeder.SeedAsync();
    await userSeeder.SeedAsync();
    await metodoVentaSeeder.SeedAsync();
    await metodoPagoSeeder.SeedAsync();
    await estadoPedidoSeeder.SeedAsync();
    logger.LogInformation("Seeds completados");
}

// =============================================
// Pipeline HTTP
// =============================================
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.MapScalarApiReference(options =>
    {
        options.Authentication = new ScalarAuthenticationOptions
        {
            PreferredSecurityScheme = "Bearer"
        };
    });
    app.UseCors("DevCors");
}

app.UseHttpsRedirection();
app.UseAuthentication();
app.UseAuthorization();

// Mapeo de controladores REST
app.MapControllers();

// Mapeo del Hub de SignalR
app.MapHub<LogisticaHub>("/hubs/logistica");

app.Run();
