package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class CajaHistorialDetalleDto {
    @SerializedName("id") private int id;
    @SerializedName("usuarioAperturaNombre") private String usuarioAperturaNombre;
    @SerializedName("usuarioCierreNombre") private String usuarioCierreNombre;
    @SerializedName("fechaApertura") private String fechaApertura;
    @SerializedName("fechaCierre") private String fechaCierre;
    @SerializedName("montoApertura") private double montoApertura;
    @SerializedName("montoCierreTeorico") private double montoCierreTeorico;
    @SerializedName("montoCierreReal") private double montoCierreReal;
    @SerializedName("diferenciaCierre") private double diferenciaCierre;
    @SerializedName("ingresosEfectivo") private double ingresosEfectivo;
    @SerializedName("ingresosTransferencia") private double ingresosTransferencia;
    @SerializedName("ingresosTarjeta") private double ingresosTarjeta;
    @SerializedName("pedidos") private List<PedidoCajaDetalleDto> pedidos;

    public int getId() { return id; }
    public String getUsuarioAperturaNombre() { return usuarioAperturaNombre; }
    public String getUsuarioCierreNombre() { return usuarioCierreNombre; }
    public String getFechaApertura() { return fechaApertura; }
    public String getFechaCierre() { return fechaCierre; }
    public double getMontoApertura() { return montoApertura; }
    public double getMontoCierreTeorico() { return montoCierreTeorico; }
    public double getMontoCierreReal() { return montoCierreReal; }
    public double getDiferenciaCierre() { return diferenciaCierre; }
    public double getIngresosEfectivo() { return ingresosEfectivo; }
    public double getIngresosTransferencia() { return ingresosTransferencia; }
    public double getIngresosTarjeta() { return ingresosTarjeta; }
    public List<PedidoCajaDetalleDto> getPedidos() { return pedidos; }
}
