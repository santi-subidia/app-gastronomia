using ApiGastronomia.Models;
using Microsoft.Extensions.Configuration;

namespace ApiGastronomia.Tests.Models;

public class JwtSettingsTests
{
    [Fact]
    public void JwtSettings_Binds_From_InMemoryConfiguration()
    {
        // Arrange: configuration values that mirror appsettings JwtSettings section
        var inMemorySettings = new Dictionary<string, string?>
        {
            { "JwtSettings:Issuer", "ApiGastronomia" },
            { "JwtSettings:Audience", "ApiGastronomiaClients" },
            { "JwtSettings:SecretKey", "super-secret-key-for-testing-at-least-16-chars" },
            { "JwtSettings:ExpiryMinutes", "480" }
        };

        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(inMemorySettings!)
            .Build();

        // Act: bind configuration to JwtSettings POCO
        var settings = new JwtSettings();
        configuration.GetSection("JwtSettings").Bind(settings);

        // Assert: all properties bound correctly from configuration
        Assert.Equal("ApiGastronomia", settings.Issuer);
        Assert.Equal("ApiGastronomiaClients", settings.Audience);
        Assert.Equal("super-secret-key-for-testing-at-least-16-chars", settings.SecretKey);
        Assert.Equal(480, settings.ExpiryMinutes);
    }

    [Fact]
    public void JwtSettings_Default_ExpiryMinutes_IsReasonable()
    {
        // Arrange: JwtSettings with no custom config should still have a sensible default
        var settings = new JwtSettings();

        // Assert: default expiry should not be zero or negative (would break token generation)
        Assert.True(settings.ExpiryMinutes > 0, "Default ExpiryMinutes should be positive");
    }

    [Fact]
    public void JwtSettings_Binds_Different_Values()
    {
        // Triangulation: verify binding works with different values
        var inMemorySettings = new Dictionary<string, string?>
        {
            { "JwtSettings:Issuer", "MyOtherApi" },
            { "JwtSettings:Audience", "MyOtherClients" },
            { "JwtSettings:SecretKey", "another-secret-key-that-is-long-enough" },
            { "JwtSettings:ExpiryMinutes", "60" }
        };

        var configuration = new ConfigurationBuilder()
            .AddInMemoryCollection(inMemorySettings!)
            .Build();

        var settings = new JwtSettings();
        configuration.GetSection("JwtSettings").Bind(settings);

        Assert.Equal("MyOtherApi", settings.Issuer);
        Assert.Equal("MyOtherClients", settings.Audience);
        Assert.Equal("another-secret-key-that-is-long-enough", settings.SecretKey);
        Assert.Equal(60, settings.ExpiryMinutes);
    }
}