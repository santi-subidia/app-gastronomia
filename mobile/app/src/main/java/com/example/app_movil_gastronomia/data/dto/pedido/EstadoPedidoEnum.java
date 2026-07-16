package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;

/**
 * Lifecycle states of a pedido (order), matching the backend contract 1:1.
 *
 * <p>Spec PED-DTO-001: the eight API string values are case-sensitive and
 * contain no accents. The Java constant names are uppercase by convention,
 * while {@link #getApiValue()} returns the exact wire format the server
 * expects (e.g. {@code "EnPreparacion"}). {@link SerializedName} mirrors
 * the same value so Gson serializes the enum to its API form, not the
 * Java identifier.</p>
 *
 * <p>Use {@link #fromApiValue(String)} to convert a string received from the
 * API back to the matching constant; returns {@code null} when the input
 * does not match any known state.</p>
 */
public enum EstadoPedidoEnum {

    @SerializedName("Pendiente")
    PENDIENTE("Pendiente", "Pendiente"),

    @SerializedName("EnPreparacion")
    EN_PREPARACION("En preparacion", "EnPreparacion"),

    @SerializedName("ListoParaRetirar")
    LISTO_PARA_RETIRAR("Listo para retirar", "ListoParaRetirar"),

    @SerializedName("EnCamino")
    EN_CAMINO("En camino", "EnCamino"),

    @SerializedName("Entregado")
    ENTREGADO("Entregado", "Entregado"),

    @SerializedName("Retirado")
    RETIRADO("Retirado", "Retirado"),

    @SerializedName("Cancelado")
    CANCELADO("Cancelado", "Cancelado"),

    @SerializedName("Devuelto")
    DEVUELTO("Devuelto", "Devuelto");

    private final String dbValue;
    private final String apiValue;

    EstadoPedidoEnum(String dbValue, String apiValue) {
        this.dbValue = dbValue;
        this.apiValue = apiValue;
    }

    /**
     * Returns the exact PascalCase string the C# backend expects for URL parameters.
     */
    public String getApiValue() {
        return apiValue;
    }

    /**
     * Inverse of database string (used for parsing if needed).
     */
    public static EstadoPedidoEnum fromApiValue(String value) {
        if (value == null) {
            return null;
        }
        for (EstadoPedidoEnum estado : values()) {
            if (estado.dbValue.equals(value) || estado.apiValue.equals(value)) {
                return estado;
            }
        }
        return null;
    }
}
