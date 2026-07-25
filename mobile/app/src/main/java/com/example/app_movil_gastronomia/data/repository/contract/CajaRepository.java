package com.example.app_movil_gastronomia.data.repository.contract;

import androidx.lifecycle.LiveData;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialDetalleDto;
import com.example.app_movil_gastronomia.data.dto.caja.CajaHistorialResumenDto;
import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;

import java.util.List;

public interface CajaRepository {

    LiveData<UiState<List<CajaDto>>> getCajas(String estado);

    LiveData<UiState<List<CajaDto>>> getCajasState();

    LiveData<UiState<List<CajaDto>>> getCajasAbiertas();

    LiveData<UiState<List<CajaDto>>> getCajasAbiertasState();

    LiveData<UiState<CajaDto>> getCaja(int id);

    LiveData<UiState<CajaDto>> getCajaState();

    LiveData<UiState<CajaDto>> abrirCaja(AbrirCajaRequest request);

    LiveData<UiState<CajaDto>> getAbrirState();

    LiveData<UiState<CajaDto>> cerrarCaja(int id, CerrarCajaRequest request);

    LiveData<UiState<CajaDto>> getCerrarState();

    LiveData<UiState<List<CajaHistorialResumenDto>>> getHistorial();

    LiveData<UiState<List<CajaHistorialResumenDto>>> getHistorialState();

    LiveData<UiState<CajaHistorialDetalleDto>> getHistorialDetalle(int id);

    LiveData<UiState<CajaHistorialDetalleDto>> getHistorialDetalleState();
}
