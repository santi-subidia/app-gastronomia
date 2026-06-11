namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// DTOs para transferencia de datos entre capas.
/// Las clases de request/response específicas de cada endpoint viven en su controller.
/// Los DTOs compartidos entre múltiples controllers van aquí.
/// </summary>

public record PedidoResumenDTO(
    int Id,
    string Estado,
    string? ClienteNombre,
    string? MetodoVenta,
    double TotalEstimado,
    DateTime FechaIngreso
);

public record ProductoDTO(
    int Id,
    string Nombre,
    double Precio,
    int Demora
);

public record DetallePedidoDTO(
    int ProductoId,
    string Nombre,
    int Cantidad,
    double Precio,
    int TiempoMaquina
);
