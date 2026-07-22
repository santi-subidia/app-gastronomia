package com.example.app_movil_gastronomia.data.dto.usuario;

public class UsuarioDto {
    private int id;
    private String usuarioNombre;
    private int rolId;
    private String rolNombre;
    private boolean disponible;
    private boolean activo;
    private boolean fueraDeServicio;

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getUsuarioNombre() { return usuarioNombre; }
    public void setUsuarioNombre(String usuarioNombre) { this.usuarioNombre = usuarioNombre; }
    public int getRolId() { return rolId; }
    public void setRolId(int rolId) { this.rolId = rolId; }
    public String getRolNombre() { return rolNombre; }
    public void setRolNombre(String rolNombre) { this.rolNombre = rolNombre; }
    public boolean isDisponible() { return disponible; }
    public void setDisponible(boolean disponible) { this.disponible = disponible; }
    public boolean isActivo() { return activo; }
    public void setActivo(boolean activo) { this.activo = activo; }
    public boolean isFueraDeServicio() { return fueraDeServicio; }
    public void setFueraDeServicio(boolean fueraDeServicio) { this.fueraDeServicio = fueraDeServicio; }
}
