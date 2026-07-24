package com.example.app_movil_gastronomia.ui.login;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModel;

import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.dto.auth.LoginRequest;
import com.example.app_movil_gastronomia.data.dto.auth.LoginResponse;
import com.example.app_movil_gastronomia.data.repository.contract.AuthRepository;

import org.junit.Rule;
import org.junit.Test;

public class LoginViewModelTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    /**
     * Verifica FIX-LD-004: el ViewModel registra un observador una sola vez
     * durante la construcción y reenvía cada emisión a su propio estado.
     */
    @Test
    public void registersObserverOnceInConstructorAndForwardsValues() {
        RecordingAuthRepository repo = new RecordingAuthRepository();
        LoginViewModel vm = new LoginViewModel(repo);

        assertEquals(1, vm.getObserverRegistrationCount());

        repo.getLoginStateInternal().setValue(UiState.success(loginResponse()));
        assertNotNull(vm.getLoginState().getValue());
        assertEquals(UiState.Status.SUCCESS, vm.getLoginState().getValue().getStatus());
    }

    /**
     * Verifica que varias llamadas a login() no registren observadores nuevos.
     */
    @Test
    public void loginDoesNotRegisterAdditionalObservers() {
        RecordingAuthRepository repo = new RecordingAuthRepository();
        LoginViewModel vm = new LoginViewModel(repo);

        int before = vm.getObserverRegistrationCount();

        vm.login("user", "password");
        vm.login("user", "password");
        vm.login("user", "password");

        assertEquals(
                 "login() no debe llamar observeForever en el repositorio",
                before, vm.getObserverRegistrationCount()
        );
    }

    /**
     * Verifica que el observador reenvíe cada estado del repositorio al LiveData del ViewModel.
     */
    @Test
    public void loginForwardsRepositoryEmissionsToVmState() {
        RecordingAuthRepository repo = new RecordingAuthRepository();
        LoginViewModel vm = new LoginViewModel(repo);

        vm.login("user", "password");
        repo.getLoginStateInternal().setValue(UiState.loading());
        assertEquals(UiState.Status.LOADING, vm.getLoginState().getValue().getStatus());

        repo.getLoginStateInternal().setValue(UiState.success(loginResponse()));
        assertEquals(UiState.Status.SUCCESS, vm.getLoginState().getValue().getStatus());
    }

    /**
     * Verifica la limpieza FIX-LD-004: onCleared() debe quitar el observador
     * del LiveData del repositorio exactamente una vez.
     */
    @Test
    public void onClearedRemovesObserver() {
        RecordingAuthRepository repo = new RecordingAuthRepository();
        LoginViewModel vm = new LoginViewModel(repo);

        assertEquals(0, repo.getLoginStateInternal().removeObserverCount);

        try {
            java.lang.reflect.Method onCleared = ViewModel.class.getDeclaredMethod("onCleared");
            onCleared.setAccessible(true);
            onCleared.invoke(vm);
        } catch (Exception e) {
            fail("Could not invoke onCleared: " + e);
        }

        assertEquals(
                 "onCleared debe quitar el observador del LiveData del repositorio",
                1, repo.getLoginStateInternal().removeObserverCount
        );
    }

    /**
     * Verifica que la validación local detecte usuario vacío o contraseña corta
     * sin llamar al repositorio.
     */
    @Test
    public void localValidationBypassesRepository() {
        RecordingAuthRepository repo = new RecordingAuthRepository();
        LoginViewModel vm = new LoginViewModel(repo);

        int callsBefore = repo.loginCallCount;

        vm.login("", "password");
        vm.login("user", "123");

         assertEquals("la entrada inválida no debe llamar al repositorio", callsBefore, repo.loginCallCount);
        assertEquals(UiState.Status.ERROR, vm.getLoginState().getValue().getStatus());
    }


    private static LoginResponse loginResponse() {
        LoginResponse r = new LoginResponse();
        r.setToken("jwt");
        r.setId(1);
        r.setRolNombre("Cajero");
        return r;
    }

    /**
     * Repositorio falso respaldado por un {@link CountingMutableLiveData} que
     * registra cuántas veces se quitó el observador.
     */
    static final class RecordingAuthRepository implements AuthRepository {
        final CountingMutableLiveData<UiState<LoginResponse>> state = new CountingMutableLiveData<>();
        int loginCallCount = 0;

        public CountingMutableLiveData<UiState<LoginResponse>> getLoginStateInternal() {
            return state;
        }

        @Override
        public MutableLiveData<UiState<LoginResponse>> getLoginState() {
            return state;
        }

        @Override
        public MutableLiveData<UiState<LoginResponse>> login(LoginRequest request) {
            loginCallCount++;
            return state;
        }
    }

    /** MutableLiveData que cuenta las llamadas a removeObserver. */
    static final class CountingMutableLiveData<T> extends MutableLiveData<T> {
        int removeObserverCount = 0;

        @Override
        public void removeObserver(Observer<? super T> observer) {
            removeObserverCount++;
            super.removeObserver(observer);
        }
    }
}
