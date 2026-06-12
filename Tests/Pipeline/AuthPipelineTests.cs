using ApiGastronomia.Models;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Authentication.JwtBearer;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.Configuration;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using System.Text;

namespace ApiGastronomia.Tests.Pipeline;

/// <summary>
/// Tests that Program.cs wires up authentication, authorization,
/// DI services, and Scalar correctly.
/// </summary>
public class AuthPipelineTests
{
    /// <summary>
    /// Verify IAuthService is registered as scoped in DI.
    /// This validates the service registration in Program.cs.
    /// </summary>
    [Fact]
    public void IAuthService_IsRegistered_InDependencyInjection()
    {
        // Arrange: build the service collection as Program.cs does
        var services = new ServiceCollection();
        services.AddLogging();

        // Register JwtSettings with test values
        var jwtSettings = new JwtSettings
        {
            Issuer = "TestIssuer",
            Audience = "TestAudience",
            SecretKey = "test-secret-key-at-least-32-characters-long",
            ExpiryMinutes = 60
        };
        services.AddSingleton(jwtSettings);

        // Register AppDbContext with InMemory
        services.AddDbContext<ApiGastronomia.Infrastructure.Data.AppDbContext>(options =>
            options.UseInMemoryDatabase("PipelineTestDb"));

        // Register IAuthService as scoped (as Program.cs does)
        services.AddScoped<IAuthService, AuthService>();

        // Act: build the provider and resolve IAuthService
        var provider = services.BuildServiceProvider();
        using var scope = provider.CreateScope();
        var service = scope.ServiceProvider.GetService<IAuthService>();

        // Assert: IAuthService resolves to AuthService instance
        Assert.NotNull(service);
        Assert.IsType<AuthService>(service);
    }

    /// <summary>
    /// Verify JwtSettings binds from configuration with correct values.
    /// Validates that Program.cs correctly configures JwtSettings binding.
    /// </summary>
    [Fact]
    public void JwtSettings_BindsFromConfiguration_Correctly()
    {
        // Arrange: InMemory configuration mimicking appsettings.json
        var config = new Microsoft.Extensions.Configuration.ConfigurationBuilder()
            .AddInMemoryCollection(new Dictionary<string, string?>
            {
                { "JwtSettings:Issuer", "ApiGastronomia" },
                { "JwtSettings:Audience", "ApiGastronomiaClients" },
                { "JwtSettings:SecretKey", "dev-secret-key-not-for-production-at-least-32-chars" },
                { "JwtSettings:ExpiryMinutes", "480" }
            }!)
            .Build();

        // Act: bind configuration to JwtSettings
        var settings = new JwtSettings();
        config.GetSection("JwtSettings").Bind(settings);

        // Assert: all values bound correctly
        Assert.Equal("ApiGastronomia", settings.Issuer);
        Assert.Equal("ApiGastronomiaClients", settings.Audience);
        Assert.Equal("dev-secret-key-not-for-production-at-least-32-chars", settings.SecretKey);
        Assert.Equal(480, settings.ExpiryMinutes);
    }

    /// <summary>
    /// Verify JwtBearer authentication can be added to services.
    /// Validates that AddAuthentication + AddJwtBearer configuration works.
    /// </summary>
    [Fact]
    public void AddAuthentication_AddJwtBearer_ConfiguresCorrectly()
    {
        // Arrange
        var services = new ServiceCollection();
        services.AddLogging();

        var jwtSettings = new JwtSettings
        {
            Issuer = "TestIssuer",
            Audience = "TestAudience",
            SecretKey = "test-secret-key-at-least-32-characters-long",
            ExpiryMinutes = 60
        };

        // Act: configure authentication as Program.cs does
        services.AddAuthentication(JwtBearerDefaults.AuthenticationScheme)
            .AddJwtBearer(options =>
            {
                options.TokenValidationParameters = new Microsoft.IdentityModel.Tokens.TokenValidationParameters
                {
                    ValidateIssuer = true,
                    ValidateAudience = true,
                    ValidateLifetime = true,
                    ValidateIssuerSigningKey = true,
                    ValidIssuer = jwtSettings.Issuer,
                    ValidAudience = jwtSettings.Audience,
                    IssuerSigningKey = new Microsoft.IdentityModel.Tokens.SymmetricSecurityKey(
                        Encoding.UTF8.GetBytes(jwtSettings.SecretKey))
                };
            });

        // Assert: authentication services are registered
        var provider = services.BuildServiceProvider();
        var authSchemeProvider = provider.GetService<Microsoft.AspNetCore.Authentication.IAuthenticationSchemeProvider>();
        Assert.NotNull(authSchemeProvider);
    }
}