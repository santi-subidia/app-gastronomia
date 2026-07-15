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
     * Verifies FIX-LD-004: the ViewModel registers an observer exactly once
     * during construction — NOT per call to login(). The observer forwards
     * every emission to the VM's own state.
     */
    @Test
    public void registersObserverOnceInConstructorAndForwardsValues() {
        RecordingAuthRepository repo = new RecordingAuthRepository();
        LoginViewModel vm = new LoginViewModel(repo);

        assertEquals(1, vm.getObserverRegistrationCount());

        // Repository emissions must reach the VM's own LiveData.
        repo.getLoginStateInternal().setValue(UiState.success(loginResponse()));
        assertNotNull(vm.getLoginState().getValue());
        assertEquals(UiState.Status.SUCCESS, vm.getLoginState().getValue().getStatus());
    }

    /**
     * Verifies that calling login() multiple times does NOT register new
     * observers — the count stays at 1.
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
                "login() must not call observeForever on the repository",
                before, vm.getObserverRegistrationCount()
        );
    }

    /**
     * Verifies the VM observer forwards every repository state transition
     * to the VM-owned LiveData.
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
     * Verifies FIX-LD-004 cleanup: onCleared() must removeObserver on the
     * repository's LiveData. The recording repo's MutableLiveData subclass
     * counts removeObserver calls so we can assert it was called exactly once.
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
                "onCleared must call removeObserver on the repository's LiveData",
                1, repo.getLoginStateInternal().removeObserverCount
        );
    }

    /**
     * Verifies local validation catches empty username / short password
     * without calling the repository.
     */
    @Test
    public void localValidationBypassesRepository() {
        RecordingAuthRepository repo = new RecordingAuthRepository();
        LoginViewModel vm = new LoginViewModel(repo);

        int callsBefore = repo.loginCallCount;

        vm.login("", "password");
        vm.login("user", "123");

        assertEquals("invalid input must not call the repository", callsBefore, repo.loginCallCount);
        assertEquals(UiState.Status.ERROR, vm.getLoginState().getValue().getStatus());
    }

    // -- helpers -------------------------------------------------------------

    private static LoginResponse loginResponse() {
        LoginResponse r = new LoginResponse();
        r.setToken("jwt");
        r.setId(1);
        r.setRolNombre("Cajero");
        return r;
    }

    /**
     * Fake repository backed by a {@link CountingMutableLiveData} that
     * records how many times removeObserver was invoked.
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

    /** MutableLiveData that counts removeObserver calls. */
    static final class CountingMutableLiveData<T> extends MutableLiveData<T> {
        int removeObserverCount = 0;

        @Override
        public void removeObserver(Observer<? super T> observer) {
            removeObserverCount++;
            super.removeObserver(observer);
        }
    }
}
