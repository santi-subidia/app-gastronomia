using Microsoft.EntityFrameworkCore;
using ApiGastronomia.Infrastructure.Data;
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
// 5. Inyección de Dependencias - Servicios
// =============================================
builder.Services.AddScoped<IPedidoService, PedidoService>();

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
// Pipeline HTTP
// =============================================
if (app.Environment.IsDevelopment())
{
    app.MapOpenApi();
    app.MapScalarApiReference();
    app.UseCors("DevCors");
}

app.UseHttpsRedirection();
app.UseAuthorization();

// Mapeo de controladores REST
app.MapControllers();

// Mapeo del Hub de SignalR
app.MapHub<LogisticaHub>("/hubs/logistica");

app.Run();
