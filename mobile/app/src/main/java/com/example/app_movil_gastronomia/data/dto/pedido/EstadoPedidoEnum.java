package com.example.app_movil_gastronomia.data.dto.pedido;

import com.google.gson.annotations.SerializedName;


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
    DEVUELTO("Devuelto", "Devuelto"),

    @SerializedName("Contingencia")
    CONTINGENCIA("Contingencia", "Contingencia");

    private final String dbValue;
    private final String apiValue;

    EstadoPedidoEnum(String dbValue, String apiValue) {
        this.dbValue = dbValue;
        this.apiValue = apiValue;
    }

    public String getDbValue() {
        return dbValue;
    }

    public String getApiValue() {
        return apiValue;
    }

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
