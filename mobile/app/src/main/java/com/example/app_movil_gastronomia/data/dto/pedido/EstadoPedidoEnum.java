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
    PENDIENTE("Pendiente"),

    @SerializedName("EnPreparacion")
    EN_PREPARACION("EnPreparacion"),

    @SerializedName("ListoParaRetirar")
    LISTO_PARA_RETIRAR("ListoParaRetirar"),

    @SerializedName("EnCamino")
    EN_CAMINO("EnCamino"),

    @SerializedName("Entregado")
    ENTREGADO("Entregado"),

    @SerializedName("Retirado")
    RETIRADO("Retirado"),

    @SerializedName("Cancelado")
    CANCELADO("Cancelado"),

    @SerializedName("Devuelto")
    DEVUELTO("Devuelto");

    private final String apiValue;

    EstadoPedidoEnum(String apiValue) {
        this.apiValue = apiValue;
    }

    public String getApiValue() {
        return apiValue;
    }

    /**
     * Inverse of {@link #getApiValue()}. Returns {@code null} if no constant
     * matches the provided API string.
     */
    public static EstadoPedidoEnum fromApiValue(String apiValue) {
        if (apiValue == null) {
            return null;
        }
        for (EstadoPedidoEnum estado : values()) {
            if (estado.apiValue.equals(apiValue)) {
                return estado;
            }
        }
        return null;
    }
}
