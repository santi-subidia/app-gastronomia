using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Tests.Domain.DTOs;

public class UsuarioDTOsTests
{
    // ---- LoginResponse ----

    [Fact]
    public void LoginResponse_Stores_All_Properties()
    {
        // Arrange
        var expiresAt = DateTime.UtcNow.AddHours(8);

        // Act
        var response = new LoginResponse(
            Id: 1,
            UsuarioNombre: "admin",
            RolId: 1,
            RolNombre: "Admin",
            Token: "jwt-token-here",
            ExpiraEn: expiresAt
        );

        // Assert: every property carries the value passed to the constructor
        Assert.Equal(1, response.Id);
        Assert.Equal("admin", response.UsuarioNombre);
        Assert.Equal(1, response.RolId);
        Assert.Equal("Admin", response.RolNombre);
        Assert.Equal("jwt-token-here", response.Token);
        Assert.Equal(expiresAt, response.ExpiraEn);
    }

    [Fact]
    public void LoginResponse_With_Different_Values()
    {
        // Triangulation: verify with different values
        var expiresAt = DateTime.UtcNow.AddMinutes(30);

        var response = new LoginResponse(
            Id: 42,
            UsuarioNombre: "cocinero1",
            RolId: 2,
            RolNombre: "Cocinero",
            Token: "eyJhbGciOiJIUzI1NiJ9...",
            ExpiraEn: expiresAt
        );

        Assert.Equal(42, response.Id);
        Assert.Equal("cocinero1", response.UsuarioNombre);
        Assert.Equal(2, response.RolId);
        Assert.Equal("Cocinero", response.RolNombre);
        Assert.Equal("eyJhbGciOiJIUzI1NiJ9...", response.Token);
        Assert.Equal(expiresAt, response.ExpiraEn);
    }

    // ---- UsuarioResponse ----

    [Fact]
    public void UsuarioResponse_Stores_All_Properties()
    {
        // Act
        var response = new UsuarioResponse(
            Id: 5,
            UsuarioNombre: "repartidor1",
            RolId: 3,
            RolNombre: "Repartidor",
            Disponible: true,
            Activo: true
        );

        // Assert: every property carries the value passed to the constructor
        Assert.Equal(5, response.Id);
        Assert.Equal("repartidor1", response.UsuarioNombre);
        Assert.Equal(3, response.RolId);
        Assert.Equal("Repartidor", response.RolNombre);
        Assert.True(response.Disponible);
        Assert.True(response.Activo);
    }

    [Fact]
    public void UsuarioResponse_With_Inactive_User()
    {
        // Triangulation: inactive user has Activo=false, Disponible=false
        var response = new UsuarioResponse(
            Id: 10,
            UsuarioNombre: "deleted_user",
            RolId: 4,
            RolNombre: "Cajero",
            Disponible: false,
            Activo: false
        );

        Assert.Equal(10, response.Id);
        Assert.Equal("deleted_user", response.UsuarioNombre);
        Assert.Equal(4, response.RolId);
        Assert.Equal("Cajero", response.RolNombre);
        Assert.False(response.Disponible);
        Assert.False(response.Activo);
    }

    // ---- Record equality semantics ----

    [Fact]
    public void LoginResponse_Equality_By_Value()
    {
        // Records have structural equality — same values = equal
        var expiresAt = DateTime.UtcNow.AddHours(1);

        var r1 = new LoginResponse(1, "admin", 1, "Admin", "token", expiresAt);
        var r2 = new LoginResponse(1, "admin", 1, "Admin", "token", expiresAt);

        Assert.Equal(r1, r2);
        Assert.Equal(r1.GetHashCode(), r2.GetHashCode());
    }

    [Fact]
    public void UsuarioResponse_Equality_By_Value()
    {
        var u1 = new UsuarioResponse(1, "admin", 1, "Admin", true, true);
        var u2 = new UsuarioResponse(1, "admin", 1, "Admin", true, true);

        Assert.Equal(u1, u2);
        Assert.Equal(u1.GetHashCode(), u2.GetHashCode());
    }
}