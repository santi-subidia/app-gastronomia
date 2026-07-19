package com.example.app_movil_gastronomia.ui.cajero;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.configuracion.ConfiguracionDto;
import com.example.app_movil_gastronomia.data.repository.contract.ConfiguracionRepository;


import javax.inject.Inject;

import dagger.hilt.android.lifecycle.HiltViewModel;

/**
 * VM for the singleton business configuration form. Bridges the three
 * repository streams (get, create, update) into two VM-owned
 * {@link LiveData}:
 *
 * <ul>
 *   <li>{@link #getConfigState()} — the current configuration, or
 *       {@code success(null)} when the server has no record yet
 *       (which the fragment interprets as "create mode").</li>
 *   <li>{@link #getSaveState()} — the result of the latest create or
 *       update call. The fragment reacts to SUCCESS by toasting and
 *       popping the back stack, and to ERROR with a Snackbar.</li>
 * </ul>
 *
 * <p>Singleton semantics: {@link #saveConfiguracion(ConfiguracionDto)}
 * dispatches to {@link ConfiguracionRepository#crearConfiguracion} when
 * the cached {@link #configState} has no payload, and to
 * {@link ConfiguracionRepository#actualizarConfiguracion} otherwise.
 * The id from the cached config is carried into the update payload so
 * the server's PUT body matches what the user previously saved.</p>
 *
 * <p>404 handling: when the server returns a "not found" envelope for
 * GET (e.g. "Configuracion no encontrada"), the VM re-emits
 * {@code success(null)} on the config stream so the fragment switches
 * to "create mode" without showing a transient error. Any other error
 * is forwarded as-is.</p>
 *
 * <p>Observer lifecycle: every {@code observeForever} registration is
 * torn down in {@link #onCleared()} so the VM never leaks subscriptions
 * to the singleton repository.</p>
 */
@HiltViewModel
public class ConfiguracionViewModel extends ViewModel {

    private final ConfiguracionRepository repository;

    private final MutableLiveData<UiState<ConfiguracionDto>> configState = new MutableLiveData<>();
    private final MutableLiveData<UiState<ConfiguracionDto>> saveState = new MutableLiveData<>();

    private final Observer<UiState<ConfiguracionDto>> getConfigObserver;
    private final Observer<UiState<ConfiguracionDto>> crearObserver;
    private final Observer<UiState<ConfiguracionDto>> actualizarObserver;


    @Inject
    public ConfiguracionViewModel(ConfiguracionRepository repository) {
        this.repository = repository;

        this.getConfigObserver = state -> {
            if (state == null) return;
            switch (state.getStatus()) {
                case LOADING:
                    configState.setValue(UiState.loading());
                    break;
                case SUCCESS:
                    configState.setValue(UiState.success(state.getData()));
                    break;
                case ERROR:
                    String error = state.getError();
                    if (isNotFoundMessage(error)) {
                        configState.setValue(UiState.success(null));
                    } else {
                        configState.setValue(UiState.error(error));
                    }
                    break;
            }
        };
        repository.getConfiguracionState().observeForever(getConfigObserver);

        this.crearObserver = state -> bridgeSave(state, true);
        repository.getCrearState().observeForever(crearObserver);

        this.actualizarObserver = state -> bridgeSave(state, true);
        repository.getActualizarState().observeForever(actualizarObserver);

        repository.getConfiguracion();
    }


    public LiveData<UiState<ConfiguracionDto>> getConfigState() {
        return configState;
    }

    public LiveData<UiState<ConfiguracionDto>> getSaveState() {
        return saveState;
    }


    /** Reloads the current configuration. Wired to the retry button. */
    public void loadConfiguracion() {
        repository.getConfiguracion();
    }

    /**
     * Persists the form payload. Dispatches to
     * {@link ConfiguracionRepository#crearConfiguracion} when no
     * configuration exists yet, and to
     * {@link ConfiguracionRepository#actualizarConfiguracion}
     * otherwise — carrying the previously-assigned {@code id} into the
     * update body so the server's PUT contract is satisfied.
     */
    public void saveConfiguracion(ConfiguracionDto dto) {
        if (dto == null) return;
        UiState<ConfiguracionDto> current = configState.getValue();
        if (current != null
                && current.getStatus() == UiState.Status.SUCCESS
                && current.getData() != null) {
            dto.setId(current.getData().getId());
            repository.actualizarConfiguracion(dto);
        } else {
            repository.crearConfiguracion(dto);
        }
    }


    /**
     * Forwards a create/update state into {@link #saveState} and, on
     * success, asks the repository to refresh the cached config so the
     * next render reflects the new id and timestamps.
     */
    private void bridgeSave(UiState<ConfiguracionDto> state, boolean reloadOnSuccess) {
        if (state == null) return;
        switch (state.getStatus()) {
            case LOADING:
                saveState.setValue(UiState.loading());
                break;
            case SUCCESS:
                saveState.setValue(UiState.success(state.getData()));
                if (reloadOnSuccess) {
                    repository.getConfiguracion();
                }
                break;
            case ERROR:
                saveState.setValue(UiState.error(state.getError()));
                break;
        }
    }

    /**
     * Returns true when the parsed server error indicates a missing
     * singleton (e.g. "Configuracion no encontrada"). The check is
     * intentionally case-insensitive and locale-tolerant so it covers
     * both the Spanish "no encontrada" and the English "not found"
     * phrases without coupling the VM to a specific server string.
     */
    static boolean isNotFoundMessage(String error) {
        if (error == null) return false;
        String lower = error.toLowerCase();
        return lower.contains("no encontrada")
                || lower.contains("not found");
    }


    @Override
    protected void onCleared() {
        super.onCleared();
        repository.getConfiguracionState().removeObserver(getConfigObserver);
        repository.getCrearState().removeObserver(crearObserver);
        repository.getActualizarState().removeObserver(actualizarObserver);
    }

}
