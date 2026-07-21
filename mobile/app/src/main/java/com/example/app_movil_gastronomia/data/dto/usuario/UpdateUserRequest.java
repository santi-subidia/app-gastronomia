package com.example.app_movil_gastronomia.data.dto.usuario;

public class UpdateUserRequest {
    private Boolean disponible;
    
    public UpdateUserRequest(Boolean disponible) {
        this.disponible = disponible;
    }
    
    public Boolean getDisponible() { return disponible; }
    public void setDisponible(Boolean disponible) { this.disponible = disponible; }
}
