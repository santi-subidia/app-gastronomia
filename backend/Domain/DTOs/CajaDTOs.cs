using System;
using System.Text.Json.Serialization;

namespace ApiGastronomia.Domain.DTOs;

/// <summary>
/// DTO de respuesta para cajas. Proyectado desde la entidad Caja.
/// </summary>
public record CajaResponse(
    int Id,
    int UsuarioAperturaId,
    string UsuarioAperturaNombre,
    int? UsuarioCierreId,
    string? UsuarioCierreNombre,
    DateTime FechaApertura,
    DateTime? FechaCierre,
    decimal MontoApertura,
    decimal? MontoCierreTeorico,
    decimal? MontoCierreReal,
    [property: JsonPropertyName("ingresosEfectivo")] decimal IngresosEfectivo = 0,
    [property: JsonPropertyName("ingresosTransferencia")] decimal IngresosTransferencia = 0,
    [property: JsonPropertyName("ingresosTarjeta")] decimal IngresosTarjeta = 0
)
{
    /// <summary>
    /// Computed estado: "abierta" when FechaCierre is null, "cerrada" otherwise.
    /// </summary>
    public string Estado => FechaCierre == null ? "abierta" : "cerrada";
}

/// <summary>
/// Request DTO for opening a new caja (apertura).
/// </summary>
public record AperturaRequest(decimal MontoApertura);

/// <summary>
/// Request DTO for closing an existing caja (cierre).
/// </summary>
public record CierreRequest(decimal MontoCierreTeorico, decimal MontoCierreReal);
