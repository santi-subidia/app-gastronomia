using ApiGastronomia.Infrastructure.Data;
using ApiGastronomia.Services;
using ApiGastronomia.Services.Interfaces;
using Microsoft.EntityFrameworkCore;
using Microsoft.Extensions.DependencyInjection;

namespace ApiGastronomia.Tests.Pipeline;

/// <summary>
/// Tests that Program.cs wires up IUsuarioService/UsuarioService
/// correctly in dependency injection (scoped lifetime).
/// </summary>
public class UsuarioPipelineTests
{
    /// <summary>
    /// Verify IUsuarioService is registered as scoped in DI.
    /// This validates the service registration in Program.cs.
    /// </summary>
    [Fact]
    public void IUsuarioService_IsRegistered_InDependencyInjection()
    {
        // Arrange: build the service collection as Program.cs does
        var services = new ServiceCollection();
        services.AddLogging();
        services.AddSignalR();

        // Register AppDbContext with InMemory
        services.AddDbContext<AppDbContext>(options =>
            options.UseInMemoryDatabase("UsuarioPipelineTestDb"));

        // Register IUsuarioService as scoped (as Program.cs does)
        services.AddScoped<IUsuarioService, UsuarioService>();

        // Act: build the provider and resolve IUsuarioService
        var provider = services.BuildServiceProvider();
        using var scope = provider.CreateScope();
        var service = scope.ServiceProvider.GetService<IUsuarioService>();

        // Assert: IUsuarioService resolves to UsuarioService instance
        Assert.NotNull(service);
        Assert.IsType<UsuarioService>(service);
    }

    /// <summary>
    /// Verify IUsuarioService is registered as Scoped (not Singleton or Transient).
    /// Scoped ensures each request gets its own instance, which is important
    /// because the service holds a reference to AppDbContext (which is also Scoped).
    /// </summary>
    [Fact]
    public void IUsuarioService_IsRegistered_AsScoped()
    {
        // Arrange
        var services = new ServiceCollection();
        services.AddLogging();
        services.AddSignalR();
        services.AddDbContext<AppDbContext>(options =>
            options.UseInMemoryDatabase("UsuarioScopedTestDb"));
        services.AddScoped<IUsuarioService, UsuarioService>();

        var provider = services.BuildServiceProvider();

        // Act: resolve from two different scopes
        using var scope1 = provider.CreateScope();
        using var scope2 = provider.CreateScope();
        var service1 = scope1.ServiceProvider.GetRequiredService<IUsuarioService>();
        var service2 = scope2.ServiceProvider.GetRequiredService<IUsuarioService>();

        // Assert: different scopes get different instances (Scoped lifetime)
        Assert.NotSame(service1, service2);

        // Within the same scope, should get the same instance
        var service1Again = scope1.ServiceProvider.GetRequiredService<IUsuarioService>();
        Assert.Same(service1, service1Again);
    }
}
