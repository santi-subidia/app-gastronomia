package com.example.app_movil_gastronomia.ui.cajero;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.caja.AbrirCajaRequest;
import com.example.app_movil_gastronomia.data.dto.caja.CajaDto;
import com.example.app_movil_gastronomia.data.dto.caja.CerrarCajaRequest;
import com.example.app_movil_gastronomia.data.repository.contract.CajaRepository;

import java.util.List;

import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * Backs {@link CajaFragment}. Powers three VM-owned LiveData streams:
 *
 * <ul>
 *   <li>{@link #getCajaState()} — the current open caja, or {@code null}
 *       on SUCCESS when no caja is currently open. Sourced from
 *       {@link CajaRepository#getCajas(String)} filtered by
 *       {@code "abiertas"}; the first list element is taken as the open
 *       caja (the server returns at most one).</li>
 *   <li>{@link #getAbrirState()} — last result of an open call.</li>
 *   <li>{@link #getCerrarState()} — last result of a close call.</li>
 * </ul>
 *
 * <p>After a successful open or close, the VM reloads the caja status
 * so the fragment can flip between the two modes without an explicit
 * call. The pattern mirrors {@link CocinaHomeViewModel}: every
 * {@code observeForever} registration is tracked and torn down in
 * {@link #onCleared()}.</p>
 */
@HiltViewModel
public class CajaViewModel extends ViewModel {

    private final CajaRepository cajaRepository;

    private final MutableLiveData<UiState<CajaDto>> cajaState = new MutableLiveData<>();
    private final MutableLiveData<UiState<CajaDto>> abrirState = new MutableLiveData<>();
    private final MutableLiveData<UiState<CajaDto>> cerrarState = new MutableLiveData<>();

    private final Observer<UiState<List<CajaDto>>> cajasRepositoryObserver;
    private final Observer<UiState<CajaDto>> abrirRepositoryObserver;
    private final Observer<UiState<CajaDto>> cerrarRepositoryObserver;


    @Inject
    public CajaViewModel(CajaRepository cajaRepository) {
        this.cajaRepository = cajaRepository;

        this.cajasRepositoryObserver = upstream -> {
            if (upstream == null) return;
            switch (upstream.getStatus()) {
                case LOADING:
                    cajaState.setValue(UiState.loading());
                    break;
                case SUCCESS:
                    List<CajaDto> list = upstream.getData();
                    CajaDto open = (list != null && !list.isEmpty()) ? list.get(0) : null;
                    cajaState.setValue(UiState.success(open));
                    break;
                case ERROR:
                    cajaState.setValue(UiState.error(upstream.getError()));
                    break;
            }
        };
        cajaRepository.getCajasState().observeForever(cajasRepositoryObserver);

        this.abrirRepositoryObserver = upstream -> {
            if (upstream == null) return;
            abrirState.setValue(upstream);
            if (upstream.getStatus() == UiState.Status.SUCCESS) {
                loadCajaStatus();
            }
        };
        cajaRepository.getAbrirState().observeForever(abrirRepositoryObserver);

        this.cerrarRepositoryObserver = upstream -> {
            if (upstream == null) return;
            cerrarState.setValue(upstream);
            if (upstream.getStatus() == UiState.Status.SUCCESS) {
                loadCajaStatus();
            }
        };
        cajaRepository.getCerrarState().observeForever(cerrarRepositoryObserver);

        loadCajaStatus();
    }

    /** Current open caja, or {@code null} on SUCCESS when no caja is open. */
    public LiveData<UiState<CajaDto>> getCajaState() {
        return cajaState;
    }

    /** Last result of an open call. */
    public LiveData<UiState<CajaDto>> getAbrirState() {
        return abrirState;
    }

    /** Last result of a close call. */
    public LiveData<UiState<CajaDto>> getCerrarState() {
        return cerrarState;
    }

    /** Reloads the open-caja status. */
    public void loadCajaStatus() {
        cajaRepository.getCajas("abiertas");
    }

    /** Reload entry point wired to the retry button. */
    public void retry() {
        loadCajaStatus();
    }

    /**
     * Opens a caja with the given apertura amount. The server
     * derives the apertura user from the auth token (v2 contract);
     * the server also enforces that at most one caja is open at a
     * time (409 if violated) — that error is surfaced through the
     * repository's standard error envelope.
     */
    public void abrirCaja(double montoApertura) {
        AbrirCajaRequest request = new AbrirCajaRequest(montoApertura);
        cajaRepository.abrirCaja(request);
    }

    /**
     * Closes the given caja with the supplied cierre real. The
     * {@link CerrarCajaRequest} DTO (v2) requires two fields:
     * {@code montoCierreTeorico} and {@code montoCierreReal} — the
     * cierre user is derived from the auth token server-side. The
     * theoretical close is the base opening amount plus all cash sales.
     */
    public void cerrarCaja(CajaDto caja, double montoCierreReal) {
        if (caja == null) return;
        CerrarCajaRequest request = new CerrarCajaRequest(
                caja.getMontoApertura() + caja.getIngresosEfectivo(),
                montoCierreReal
        );
        cajaRepository.cerrarCaja(caja.getId(), request);
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        cajaRepository.getCajasState().removeObserver(cajasRepositoryObserver);
        cajaRepository.getAbrirState().removeObserver(abrirRepositoryObserver);
        cajaRepository.getCerrarState().removeObserver(cerrarRepositoryObserver);
    }

}
