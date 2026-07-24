package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;

public class CajaDto {

    @SerializedName("id")
    private int id;

    @SerializedName("usuarioAperturaNombre")
    private String usuarioAperturaNombre;

    @SerializedName("usuarioCierreNombre")
    private String usuarioCierreNombre;

    @SerializedName("fechaApertura")
    private String fechaApertura;

    @SerializedName("fechaCierre")
    private String fechaCierre;

    @SerializedName("montoApertura")
    private double montoApertura;

    @SerializedName("montoCierreTeorico")
    private Double montoCierreTeorico;

    @SerializedName("montoCierreReal")
    private Double montoCierreReal;

    @SerializedName("estado")
    private String estado;

    @SerializedName("ingresosEfectivo")
    private double ingresosEfectivo;

    @SerializedName("ingresosTransferencia")
    private double ingresosTransferencia;

    @SerializedName("ingresosTarjeta")
    private double ingresosTarjeta;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getUsuarioAperturaNombre() {
        return usuarioAperturaNombre;
    }

    public void setUsuarioAperturaNombre(String usuarioAperturaNombre) {
        this.usuarioAperturaNombre = usuarioAperturaNombre;
    }

    public String getUsuarioCierreNombre() {
        return usuarioCierreNombre;
    }

    public void setUsuarioCierreNombre(String usuarioCierreNombre) {
        this.usuarioCierreNombre = usuarioCierreNombre;
    }

    public String getFechaApertura() {
        return fechaApertura;
    }

    public void setFechaApertura(String fechaApertura) {
        this.fechaApertura = fechaApertura;
    }

    public String getFechaCierre() {
        return fechaCierre;
    }

    public void setFechaCierre(String fechaCierre) {
        this.fechaCierre = fechaCierre;
    }

    public double getMontoApertura() {
        return montoApertura;
    }

    public void setMontoApertura(double montoApertura) {
        this.montoApertura = montoApertura;
    }

    public Double getMontoCierreTeorico() {
        return montoCierreTeorico;
    }

    public void setMontoCierreTeorico(Double montoCierreTeorico) {
        this.montoCierreTeorico = montoCierreTeorico;
    }

    public Double getMontoCierreReal() {
        return montoCierreReal;
    }

    public void setMontoCierreReal(Double montoCierreReal) {
        this.montoCierreReal = montoCierreReal;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public double getIngresosEfectivo() {
        return ingresosEfectivo;
    }

    public void setIngresosEfectivo(double ingresosEfectivo) {
        this.ingresosEfectivo = ingresosEfectivo;
    }

    public double getIngresosTransferencia() {
        return ingresosTransferencia;
    }

    public void setIngresosTransferencia(double ingresosTransferencia) {
        this.ingresosTransferencia = ingresosTransferencia;
    }

    public double getIngresosTarjeta() {
        return ingresosTarjeta;
    }

    public void setIngresosTarjeta(double ingresosTarjeta) {
        this.ingresosTarjeta = ingresosTarjeta;
    }
}
