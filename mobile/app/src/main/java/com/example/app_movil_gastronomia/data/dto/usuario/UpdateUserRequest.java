package com.example.app_movil_gastronomia.data.dto.usuario;

public class UpdateUserRequest {
    private Boolean disponible;
    private Boolean fueraDeServicio;
    
    public UpdateUserRequest(Boolean disponible, Boolean fueraDeServicio) {
        this.disponible = disponible;
        this.fueraDeServicio = fueraDeServicio;
    }
    
    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
    public Boolean getFueraDeServicio() { return fueraDeServicio; }
    public void setFueraDeServicio(Boolean fueraDeServicio) { this.fueraDeServicio = fueraDeServicio; }
}
