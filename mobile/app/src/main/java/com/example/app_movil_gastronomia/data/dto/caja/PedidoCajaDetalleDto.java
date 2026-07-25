package com.example.app_movil_gastronomia.data.dto.caja;

import com.google.gson.annotations.SerializedName;
import java.util.List;

public class PedidoCajaDetalleDto {
    @SerializedName("id") private int id;
    @SerializedName("estado") private String estado;
    @SerializedName("clienteNombre") private String clienteNombre;
    @SerializedName("metodoVenta") private String metodoVenta;
    @SerializedName("metodoPago") private String metodoPago;
    @SerializedName("total") private double total;
    @SerializedName("fechaIngreso") private String fechaIngreso;
    @SerializedName("detalles") private List<DetallePedidoCajaDto> detalles;

    public int getId() { return id; }
    public String getEstado() { return estado; }
    public String getClienteNombre() { return clienteNombre; }
    public String getMetodoVenta() { return metodoVenta; }
    public String getMetodoPago() { return metodoPago; }
    public double getTotal() { return total; }
    public String getFechaIngreso() { return fechaIngreso; }
    public List<DetallePedidoCajaDto> getDetalles() { return detalles; }
}
