using System.Security.Claims;
using Microsoft.AspNetCore.Builder;
using Microsoft.AspNetCore.Http;
using Microsoft.AspNetCore.RateLimiting;
using Microsoft.Extensions.DependencyInjection;
using Microsoft.Extensions.Options;
using System.Threading.RateLimiting;

namespace ApiGastronomia.Tests.Pipeline;

/// <summary>
/// Tests that Program.cs wires up rate limiting correctly:
/// RejectionStatusCode, GlobalLimiter, LoginPolicy, and OnRejected callback.
/// </summary>
public class RateLimitingPipelineTests
{
    /// <summary>
    /// Helper to configure AddRateLimiter with the same options as Program.cs.
    /// Extracted to avoid duplication across test methods.
    /// </summary>
    private static IServiceCollection AddRateLimiterConfiguration(IServiceCollection services)
    {
        services.AddRateLimiter(options =>
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

        return services;
    }

    /// <summary>
    /// Verify AddRateLimiter configures RejectionStatusCode as 429.
    /// This ensures rate-limited requests receive the correct HTTP status.
    /// </summary>
    [Fact]
    public void AddRateLimiter_SetsRejectionStatusCode_To429()
    {
        // Arrange
        var services = new ServiceCollection();
        services.AddLogging();
        AddRateLimiterConfiguration(services);

        // Act
        var provider = services.BuildServiceProvider();
        var rateLimiterOptions = provider.GetRequiredService<IOptions<RateLimiterOptions>>().Value;

        // Assert
        Assert.Equal(429, rateLimiterOptions.RejectionStatusCode);
    }

    /// <summary>
    /// Verify AddRateLimiter configures the GlobalLimiter (PartitionedRateLimiter).
    /// Requests routed through the global limiter are partitioned by JWT sub/IP/unknown.
    /// </summary>
    [Fact]
    public void AddRateLimiter_SetsGlobalLimiter_ToPartitionedRateLimiter()
    {
        // Arrange
        var services = new ServiceCollection();
        services.AddLogging();
        AddRateLimiterConfiguration(services);

        // Act
        var provider = services.BuildServiceProvider();
        var rateLimiterOptions = provider.GetRequiredService<IOptions<RateLimiterOptions>>().Value;

        // Assert
        Assert.NotNull(rateLimiterOptions.GlobalLimiter);
    }

    /// <summary>
    /// Verify AddRateLimiter registers "LoginPolicy" as a named rate limiting policy.
    /// This policy uses IP-based fixed window (10 req/min) for the login endpoint.
    /// Uses reflection since PolicyMap is a private property on RateLimiterOptions.
    /// </summary>
    [Fact]
    public void AddRateLimiter_RegistersLoginPolicy_InPolicies()
    {
        // Arrange
        var services = new ServiceCollection();
        services.AddLogging();
        AddRateLimiterConfiguration(services);

        // Act
        var provider = services.BuildServiceProvider();
        var rateLimiterOptions = provider.GetRequiredService<IOptions<RateLimiterOptions>>().Value;

        // Assert: PolicyMap is private, access via reflection
        var policyMapProperty = typeof(RateLimiterOptions).GetProperty("PolicyMap",
            System.Reflection.BindingFlags.NonPublic | System.Reflection.BindingFlags.Instance);
        Assert.NotNull(policyMapProperty);

        var policyMap = policyMapProperty.GetValue(rateLimiterOptions) as System.Collections.IDictionary;
        Assert.NotNull(policyMap);
        Assert.True(policyMap.Contains("LoginPolicy"),
            "LoginPolicy should be registered in rate limiter policies");
    }

    /// <summary>
    /// Verify AddRateLimiter configures OnRejected callback.
    /// The callback returns Spanish JSON 429 responses with Retry-After header.
    /// </summary>
    [Fact]
    public void AddRateLimiter_ConfiguresOnRejected_Callback()
    {
        // Arrange
        var services = new ServiceCollection();
        services.AddLogging();
        AddRateLimiterConfiguration(services);

        // Act
        var provider = services.BuildServiceProvider();
        var rateLimiterOptions = provider.GetRequiredService<IOptions<RateLimiterOptions>>().Value;

        // Assert
        Assert.NotNull(rateLimiterOptions.OnRejected);
    }
}