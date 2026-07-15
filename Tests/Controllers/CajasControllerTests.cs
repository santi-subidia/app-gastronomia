using ApiGastronomia.Controllers;
using ApiGastronomia.Domain;
using ApiGastronomia.Domain.DTOs;
using ApiGastronomia.Services.Interfaces;
using Microsoft.AspNetCore.Mvc;
using Moq;

namespace ApiGastronomia.Tests.Controllers;

public class CajasControllerTests
{
    // ================================================================
    // Helpers
    // ================================================================

    private static readonly CajaResponse CajaAbiertaResponse = new(
        Id: 1,
        UsuarioAperturaId: 1,
        UsuarioAperturaNombre: "Admin",
        UsuarioCierreId: null,
        UsuarioCierreNombre: null,
        FechaApertura: DateTime.UtcNow,
        FechaCierre: null,
        MontoApertura: 5000m,
        MontoCierreTeorico: null,
        MontoCierreReal: null
    );

    private static readonly CajaResponse CajaCerradaResponse = new(
        Id: 2,
        UsuarioAperturaId: 1,
        UsuarioAperturaNombre: "Admin",
        UsuarioCierreId: 2,
        UsuarioCierreNombre: "Cajero",
        FechaApertura: DateTime.UtcNow.AddDays(-1),
        FechaCierre: DateTime.UtcNow,
        MontoApertura: 3000m,
        MontoCierreTeorico: 3500m,
        MontoCierreReal: 3450m
    );

    /// <summary>
    /// Creates a CajasController with mocked service and logger.
    /// </summary>
    private static CajasController CreateController(ICajaService service)
    {
        var logger = Mock.Of<Microsoft.Extensions.Logging.ILogger<CajasController>>();
        return new CajasController(service, logger);
    }

    // ================================================================
    // POST /api/cajas/apertura — Apertura
    // ================================================================

    [Fact]
    public async Task Apertura_Success_ReturnsCreatedAtAction()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.AperturaAsync(1, 5000m))
            .ReturnsAsync(CajaAbiertaResponse);

        var controller = CreateController(mockService.Object);
        var request = new AperturaRequest(UsuarioAperturaId: 1, MontoApertura: 5000m);

        // Act
        var result = await controller.Apertura(request);

        // Assert: 201 CreatedAtAction pointing to GetById
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        Assert.Equal(nameof(CajasController.GetById), createdResult.ActionName);
        Assert.Equal(1, createdResult.RouteValues!["id"]);
        var response = Assert.IsType<CajaResponse>(createdResult.Value!);
        Assert.Equal(1, response.Id);
        Assert.Equal(5000m, response.MontoApertura);
        Assert.Equal("abierta", response.Estado);
    }

    [Fact]
    public async Task Apertura_OpenCajaExists_ReturnsConflict()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.AperturaAsync(1, 5000m))
            .ThrowsAsync(new InvalidOperationException("Ya existe una caja abierta."));

        var controller = CreateController(mockService.Object);
        var request = new AperturaRequest(UsuarioAperturaId: 1, MontoApertura: 5000m);

        // Act
        var result = await controller.Apertura(request);

        // Assert: 409 Conflict
        var conflictResult = Assert.IsType<ConflictObjectResult>(result.Result);
        Assert.NotNull(conflictResult.Value);
    }

    [Fact]
    public async Task Apertura_UserNotFound_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.AperturaAsync(999, 5000m))
            .ThrowsAsync(new KeyNotFoundException("Usuario no encontrado."));

        var controller = CreateController(mockService.Object);
        var request = new AperturaRequest(UsuarioAperturaId: 999, MontoApertura: 5000m);

        // Act
        var result = await controller.Apertura(request);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // POST /api/cajas/{id}/cierre — Cerrar
    // ================================================================

    [Fact]
    public async Task Cerrar_Success_ReturnsOk()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.CierreAsync(1, 2, 5500m, 5450m))
            .ReturnsAsync(CajaCerradaResponse);

        var controller = CreateController(mockService.Object);
        var request = new CierreRequest(UsuarioCierreId: 2, MontoCierreTeorico: 5500m, MontoCierreReal: 5450m);

        // Act
        var result = await controller.Cerrar(1, request);

        // Assert: 200 OK with CajaResponse
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<CajaResponse>(okResult.Value!);
        Assert.Equal(2, response.Id);
        Assert.Equal("cerrada", response.Estado);
    }

    [Fact]
    public async Task Cerrar_CajaNotFound_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.CierreAsync(999, 2, 5500m, 5450m))
            .ThrowsAsync(new KeyNotFoundException("Caja no encontrada."));

        var controller = CreateController(mockService.Object);
        var request = new CierreRequest(UsuarioCierreId: 2, MontoCierreTeorico: 5500m, MontoCierreReal: 5450m);

        // Act
        var result = await controller.Cerrar(999, request);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    [Fact]
    public async Task Cerrar_CajaAlreadyClosed_ReturnsConflict()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.CierreAsync(1, 2, 5500m, 5450m))
            .ThrowsAsync(new InvalidOperationException("La caja ya se encuentra cerrada."));

        var controller = CreateController(mockService.Object);
        var request = new CierreRequest(UsuarioCierreId: 2, MontoCierreTeorico: 5500m, MontoCierreReal: 5450m);

        // Act
        var result = await controller.Cerrar(1, request);

        // Assert: 409 Conflict
        Assert.IsType<ConflictObjectResult>(result.Result);
    }

    [Fact]
    public async Task Cerrar_PendingOrders_ReturnsConflictWithStableCode()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.CierreAsync(1, 2, 5500m, 5450m))
            .ThrowsAsync(new BusinessRuleException(
                "PENDING_ORDERS_ON_CLOSE",
                "No se puede cerrar la caja porque tiene 1 pedido(s) pendiente(s)."));

        var controller = CreateController(mockService.Object);
        var request = new CierreRequest(UsuarioCierreId: 2, MontoCierreTeorico: 5500m, MontoCierreReal: 5450m);

        // Act
        var result = await controller.Cerrar(1, request);

        // Assert
        var conflictResult = Assert.IsType<ConflictObjectResult>(result.Result);
        var value = conflictResult.Value!;
        Assert.Equal("PENDING_ORDERS_ON_CLOSE", value.GetType().GetProperty("Codigo")!.GetValue(value));
    }

    [Fact]
    public async Task Cerrar_UserNotFound_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.CierreAsync(1, 999, 5500m, 5450m))
            .ThrowsAsync(new KeyNotFoundException("Usuario no encontrado."));

        var controller = CreateController(mockService.Object);
        var request = new CierreRequest(UsuarioCierreId: 999, MontoCierreTeorico: 5500m, MontoCierreReal: 5450m);

        // Act
        var result = await controller.Cerrar(1, request);

        // Assert: 404 NotFound (user not found)
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // GET /api/cajas — GetAll
    // ================================================================

    [Fact]
    public async Task GetAll_ReturnsAllCajas()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.ObtenerTodasAsync(null))
            .ReturnsAsync([CajaAbiertaResponse, CajaCerradaResponse]);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetAll();

        // Assert: 200 OK with 2 items
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var cajas = Assert.IsAssignableFrom<IEnumerable<CajaResponse>>(okResult.Value!);
        Assert.Equal(2, cajas.Count());
    }

    [Fact]
    public async Task GetAll_EmptyList_ReturnsOkWithEmptyArray()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.ObtenerTodasAsync(null))
            .ReturnsAsync([]);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetAll();

        // Assert: 200 OK with empty collection
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var cajas = Assert.IsAssignableFrom<IEnumerable<CajaResponse>>(okResult.Value!);
        Assert.Empty(cajas);
    }

    // ================================================================
    // GET /api/cajas/{id} — GetById
    // ================================================================

    [Fact]
    public async Task GetById_Found_ReturnsOk()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.ObtenerPorIdAsync(1))
            .ReturnsAsync(CajaAbiertaResponse);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetById(1);

        // Assert: 200 OK with CajaResponse
        var okResult = Assert.IsType<OkObjectResult>(result.Result);
        var response = Assert.IsType<CajaResponse>(okResult.Value!);
        Assert.Equal(1, response.Id);
        Assert.Equal("abierta", response.Estado);
    }

    [Fact]
    public async Task GetById_NotFound_ReturnsNotFound()
    {
        // Arrange
        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.ObtenerPorIdAsync(999))
            .ReturnsAsync((CajaResponse?)null);

        var controller = CreateController(mockService.Object);

        // Act
        var result = await controller.GetById(999);

        // Assert: 404 NotFound
        Assert.IsType<NotFoundObjectResult>(result.Result);
    }

    // ================================================================
    // Triangulation
    // ================================================================

    [Fact]
    public async Task Apertura_DifferentMontos_ReturnsCorrectValues()
    {
        // Arrange: triangulation with different monto — verify the controller
        // correctly passes through different monto values from the service
        var response2500 = new CajaResponse(
            Id: 10, UsuarioAperturaId: 3, UsuarioAperturaNombre: "TestUser",
            UsuarioCierreId: null, UsuarioCierreNombre: null,
            FechaApertura: DateTime.UtcNow, FechaCierre: null,
            MontoApertura: 2500m, MontoCierreTeorico: null, MontoCierreReal: null
        );

        var mockService = new Mock<ICajaService>();
        mockService
            .Setup(s => s.AperturaAsync(3, 2500m))
            .ReturnsAsync(response2500);

        var controller = CreateController(mockService.Object);
        var request = new AperturaRequest(UsuarioAperturaId: 3, MontoApertura: 2500m);

        // Act
        var result = await controller.Apertura(request);

        // Assert: 201 and correct monto propagated through
        var createdResult = Assert.IsType<CreatedAtActionResult>(result.Result);
        var response = Assert.IsType<CajaResponse>(createdResult.Value!);
        Assert.Equal(2500m, response.MontoApertura);
        Assert.Equal(3, response.UsuarioAperturaId);
        Assert.Equal("abierta", response.Estado);
    }
}
