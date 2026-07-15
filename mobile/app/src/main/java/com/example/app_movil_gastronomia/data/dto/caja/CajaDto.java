package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;

/**
 * Wire-format representation of a caja returned by the
 * {@code /api/cajas} endpoints (list, get-by-id, apertura and
 * cierre responses).
 *
 * <p>Spec CAJ-DTO-001 (v2): the 9 remaining fields map 1:1 to
 * the v2 JSON contract in {@code doc/API_REFERENCIA.md} §3.4.
 * The v2 server no longer includes {@code usuarioAperturaId} or
 * {@code usuarioCierreId} — the caller derives them from the auth
 * token. The display-only name fields ({@code usuarioAperturaNombre},
 * {@code usuarioCierreNombre}) are kept. Cierre-related fields
 * ({@code fechaCierre}, {@code montoCierreTeorico},
 * {@code montoCierreReal}) are typed as boxed wrappers so Gson
 * keeps them {@code null} for an open caja that has no cierre
 * data yet.</p>
 */
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
}
