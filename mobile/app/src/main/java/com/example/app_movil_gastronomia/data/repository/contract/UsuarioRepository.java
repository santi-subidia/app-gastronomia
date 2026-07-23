package com.example.app_movil_gastronomia.data.repository.contract;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.usuario.UsuarioDto;
import java.util.List;
import androidx.lifecycle.LiveData;

public interface UsuarioRepository {
    LiveData<UiState<List<UsuarioDto>>> getRepartidoresState();
    void fetchRepartidores();

    LiveData<UiState<List<UsuarioDto>>> getRepartidoresDisponiblesState();
    void fetchRepartidoresDisponibles();
    
    LiveData<UiState<UsuarioDto>> getUpdateState();
    void updateDisponibilidad(int id, boolean disponible);
    
    LiveData<UiState<UsuarioDto>> getUsuarioState();
    void fetchUsuario(int id);

    LiveData<UiState<Void>> getContingenciaState();
    void reportarContingencia(int id, String motivo);
}
