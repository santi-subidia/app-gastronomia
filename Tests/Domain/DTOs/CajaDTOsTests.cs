using ApiGastronomia.Domain.DTOs;

namespace ApiGastronomia.Tests.Domain.DTOs;

public class CajaDTOsTests
{
    // ================================================================
    // CajaResponse — construction and property storage
    // ================================================================

    [Fact]
    public void CajaResponse_Stores_All_Properties()
    {
        // Arrange
        var fechaApertura = DateTime.UtcNow;
        var fechaCierre = DateTime.UtcNow.AddHours(8);
        var montoCierreTeorico = 55000m;
        var montoCierreReal = 54800m;

        // Act
        var response = new CajaResponse(
            Id: 1,
            UsuarioAperturaId: 10,
            UsuarioAperturaNombre: "admin",
            UsuarioCierreId: 20,
            UsuarioCierreNombre: "cajero1",
            FechaApertura: fechaApertura,
            FechaCierre: fechaCierre,
            MontoApertura: 50000m,
            MontoCierreTeorico: montoCierreTeorico,
            MontoCierreReal: montoCierreReal
        );

        // Assert: every positional property carries the value passed to the constructor
        Assert.Equal(1, response.Id);
        Assert.Equal(10, response.UsuarioAperturaId);
        Assert.Equal("admin", response.UsuarioAperturaNombre);
        Assert.Equal(20, response.UsuarioCierreId);
        Assert.Equal("cajero1", response.UsuarioCierreNombre);
        Assert.Equal(fechaApertura, response.FechaApertura);
        Assert.Equal(fechaCierre, response.FechaCierre);
        Assert.Equal(50000m, response.MontoApertura);
        Assert.Equal(montoCierreTeorico, response.MontoCierreTeorico);
        Assert.Equal(montoCierreReal, response.MontoCierreReal);
    }

    [Fact]
    public void CajaResponse_With_Different_Values()
    {
        // Triangulation: verify with different values
        var fechaApertura = DateTime.UtcNow.AddDays(-2);
        var fechaCierre = DateTime.UtcNow.AddDays(-1);

        var response = new CajaResponse(
            Id: 42,
            UsuarioAperturaId: 5,
            UsuarioAperturaNombre: "supervisor",
            UsuarioCierreId: 8,
            UsuarioCierreNombre: "cajero2",
            FechaApertura: fechaApertura,
            FechaCierre: fechaCierre,
            MontoApertura: 100000m,
            MontoCierreTeorico: 98000m,
            MontoCierreReal: 97500m
        );

        Assert.Equal(42, response.Id);
        Assert.Equal(5, response.UsuarioAperturaId);
        Assert.Equal("supervisor", response.UsuarioAperturaNombre);
        Assert.Equal(8, response.UsuarioCierreId);
        Assert.Equal("cajero2", response.UsuarioCierreNombre);
        Assert.Equal(fechaApertura, response.FechaApertura);
        Assert.Equal(fechaCierre, response.FechaCierre);
        Assert.Equal(100000m, response.MontoApertura);
        Assert.Equal(98000m, response.MontoCierreTeorico);
        Assert.Equal(97500m, response.MontoCierreReal);
    }

    // ================================================================
    // CajaResponse — nullable cierre fields (open caja)
    // ================================================================

    [Fact]
    public void CajaResponse_OpenCaja_NullCierreFields()
    {
        // An open caja has null cierre fields
        var response = new CajaResponse(
            Id: 3,
            UsuarioAperturaId: 7,
            UsuarioAperturaNombre: "mozo1",
            UsuarioCierreId: null,
            UsuarioCierreNombre: null,
            FechaApertura: DateTime.UtcNow,
            FechaCierre: null,
            MontoApertura: 30000m,
            MontoCierreTeorico: null,
            MontoCierreReal: null
        );

        Assert.Null(response.UsuarioCierreId);
        Assert.Null(response.UsuarioCierreNombre);
        Assert.Null(response.FechaCierre);
        Assert.Null(response.MontoCierreTeorico);
        Assert.Null(response.MontoCierreReal);
    }

    // ================================================================
    // CajaResponse.Estado — computed property (the KEY testable behavior)
    // ================================================================

    [Fact]
    public void CajaResponse_Estado_Abierta_WhenFechaCierreIsNull()
    {
        // Arrange: open caja (FechaCierre = null)
        var response = new CajaResponse(
            Id: 1,
            UsuarioAperturaId: 10,
            UsuarioAperturaNombre: "admin",
            UsuarioCierreId: null,
            UsuarioCierreNombre: null,
            FechaApertura: DateTime.UtcNow,
            FechaCierre: null,
            MontoApertura: 50000m,
            MontoCierreTeorico: null,
            MontoCierreReal: null
        );

        // Assert: Estado is "abierta" when FechaCierre is null
        Assert.Equal("abierta", response.Estado);
    }

    [Fact]
    public void CajaResponse_Estado_Cerrada_WhenFechaCierreHasValue()
    {
        // Triangulation: Estado is "cerrada" when FechaCierre is set
        var response = new CajaResponse(
            Id: 2,
            UsuarioAperturaId: 10,
            UsuarioAperturaNombre: "admin",
            UsuarioCierreId: 20,
            UsuarioCierreNombre: "cajero1",
            FechaApertura: DateTime.UtcNow.AddHours(-8),
            FechaCierre: DateTime.UtcNow,
            MontoApertura: 50000m,
            MontoCierreTeorico: 55000m,
            MontoCierreReal: 54800m
        );

        // Assert: Estado is "cerrada" when FechaCierre has a value
        Assert.Equal("cerrada", response.Estado);
    }

    // ================================================================
    // CajaResponse — record equality semantics
    // ================================================================

    [Fact]
    public void CajaResponse_Equality_By_Value()
    {
        // Records have structural equality — same values = equal
        var fechaApertura = DateTime.UtcNow;
        var fechaCierre = fechaApertura.AddHours(8);

        var r1 = new CajaResponse(1, 10, "admin", 20, "cajero1", fechaApertura, fechaCierre, 50000m, 55000m, 54800m);
        var r2 = new CajaResponse(1, 10, "admin", 20, "cajero1", fechaApertura, fechaCierre, 50000m, 55000m, 54800m);

        Assert.Equal(r1, r2);
        Assert.Equal(r1.GetHashCode(), r2.GetHashCode());
    }

    // ================================================================
    // AperturaRequest — construction and property storage
    // ================================================================

    [Fact]
    public void AperturaRequest_Stores_All_Properties()
    {
        // Act
        var request = new AperturaRequest(UsuarioAperturaId: 5, MontoApertura: 50000m);

        // Assert
        Assert.Equal(5, request.UsuarioAperturaId);
        Assert.Equal(50000m, request.MontoApertura);
    }

    [Fact]
    public void AperturaRequest_With_Different_Values()
    {
        // Triangulation: different values
        var request = new AperturaRequest(UsuarioAperturaId: 99, MontoApertura: 100000m);

        Assert.Equal(99, request.UsuarioAperturaId);
        Assert.Equal(100000m, request.MontoApertura);
    }

    [Fact]
    public void AperturaRequest_Equality_By_Value()
    {
        var r1 = new AperturaRequest(5, 50000m);
        var r2 = new AperturaRequest(5, 50000m);

        Assert.Equal(r1, r2);
    }

    // ================================================================
    // CierreRequest — construction and property storage
    // ================================================================

    [Fact]
    public void CierreRequest_Stores_All_Properties()
    {
        // Act
        var request = new CierreRequest(UsuarioCierreId: 7, MontoCierreTeorico: 55000m, MontoCierreReal: 54800m);

        // Assert
        Assert.Equal(7, request.UsuarioCierreId);
        Assert.Equal(55000m, request.MontoCierreTeorico);
        Assert.Equal(54800m, request.MontoCierreReal);
    }

    [Fact]
    public void CierreRequest_With_Different_Values()
    {
        // Triangulation: different values
        var request = new CierreRequest(UsuarioCierreId: 42, MontoCierreTeorico: 98000m, MontoCierreReal: 97500m);

        Assert.Equal(42, request.UsuarioCierreId);
        Assert.Equal(98000m, request.MontoCierreTeorico);
        Assert.Equal(97500m, request.MontoCierreReal);
    }

    [Fact]
    public void CierreRequest_Equality_By_Value()
    {
        var r1 = new CierreRequest(7, 55000m, 54800m);
        var r2 = new CierreRequest(7, 55000m, 54800m);

        Assert.Equal(r1, r2);
    }
}