package com.example.app_movil_gastronomia.data.repository;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.Observer;

import com.example.app_movil_gastronomia.core.TokenManager;
import com.example.app_movil_gastronomia.core.UiState;
import com.example.app_movil_gastronomia.data.api.AuthApi;
import com.example.app_movil_gastronomia.data.dto.auth.LoginRequest;
import com.example.app_movil_gastronomia.data.dto.auth.LoginResponse;

import org.junit.Rule;
import org.junit.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import okhttp3.Request;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class AuthRepositoryImplTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    /**
     * Verifies that two back-to-back login() calls expose the SAME
     * MutableLiveData instance — no per-call allocation. This is the
     * core contract of FIX-LD-001.
     */
    @Test
    public void getLoginStateReturnsSameInstanceAcrossCalls() {
        FakeAuthApi api = new FakeAuthApi();
        api.nextResponse = Response.success(new LoginResponse());
        NoopTokenManager tokenManager = new NoopTokenManager();
        AuthRepositoryImpl repo = new AuthRepositoryImpl(api, tokenManager);

        LiveData<UiState<LoginResponse>> first = repo.getLoginState();
        LiveData<UiState<LoginResponse>> second = repo.getLoginState();

        assertSame(
                "getLoginState() must return the same LiveData instance every call",
                first, second
        );

        // Even after triggering a login, the same instance is returned.
        repo.login(new LoginRequest("u", "p"));
        LiveData<UiState<LoginResponse>> third = repo.getLoginState();
        assertSame(first, third);
    }

    /**
     * Verifies that login() loads LOADING first, then SUCCESS, on the single
     * repository instance — and that an observer registered once sees every
     * emission.
     */
    @Test
    public void loginEmitsLoadingThenSuccessOnSameInstance() {
        FakeAuthApi api = new FakeAuthApi();
        LoginResponse expected = new LoginResponse();
        expected.setToken("jwt");
        api.nextResponse = Response.success(expected);
        NoopTokenManager tokenManager = new NoopTokenManager();
        AuthRepositoryImpl repo = new AuthRepositoryImpl(api, tokenManager);

        LiveData<UiState<LoginResponse>> state = repo.getLoginState();
        AtomicReference<UiState<LoginResponse>> latest = new AtomicReference<>();
        Observer<UiState<LoginResponse>> observer = latest::set;
        state.observeForever(observer);
        try {
            // MutableLiveData initial value is null until first setValue; observer has nothing yet.
            repo.login(new LoginRequest("u", "p"));

            // The single shared instance must now have been set to LOADING then SUCCESS.
            UiState<LoginResponse> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.SUCCESS, after.getStatus());
            assertNotNull(after.getData());
            assertEquals("jwt", after.getData().getToken());
        } finally {
            state.removeObserver(observer);
        }
    }

    /**
     * Verifies the LOADING emission happens BEFORE the terminal state on the
     * same instance — by registering an observer that counts LOADING/SUCCESS.
     */
    @Test
    public void loadingIsEmittedBeforeSuccessOnSameInstance() {
        FakeAuthApi api = new FakeAuthApi();
        LoginResponse expected = new LoginResponse();
        expected.setToken("jwt");
        api.nextResponse = Response.success(expected);
        NoopTokenManager tokenManager = new NoopTokenManager();
        AuthRepositoryImpl repo = new AuthRepositoryImpl(api, tokenManager);

        LiveData<UiState<LoginResponse>> state = repo.getLoginState();
        final java.util.List<UiState.Status> seen = new java.util.ArrayList<>();
        Observer<UiState<LoginResponse>> observer = s -> {
            if (s != null) seen.add(s.getStatus());
        };
        state.observeForever(observer);
        try {
            repo.login(new LoginRequest("u", "p"));

            assertTrue("expected at least one LOADING, got: " + seen, seen.contains(UiState.Status.LOADING));
            assertTrue("expected SUCCESS, got: " + seen, seen.contains(UiState.Status.SUCCESS));
            assertEquals(UiState.Status.LOADING, seen.get(0));
            assertEquals(UiState.Status.SUCCESS, seen.get(seen.size() - 1));
        } finally {
            state.removeObserver(observer);
        }
    }

    /**
     * Verifies that the same _loginState instance posts ERROR on network
     * failure — no new LiveData is allocated.
     */
    @Test
    public void loginEmitsErrorOnNetworkFailure() {
        FakeAuthApi api = new FakeAuthApi();
        api.nextFailure = new IOException("boom");
        NoopTokenManager tokenManager = new NoopTokenManager();
        AuthRepositoryImpl repo = new AuthRepositoryImpl(api, tokenManager);

        LiveData<UiState<LoginResponse>> state = repo.getLoginState();
        AtomicReference<UiState<LoginResponse>> latest = new AtomicReference<>();
        Observer<UiState<LoginResponse>> observer = latest::set;
        state.observeForever(observer);
        try {
            repo.login(new LoginRequest("u", "p"));

            UiState<LoginResponse> after = latest.get();
            assertNotNull(after);
            assertEquals(UiState.Status.ERROR, after.getStatus());
            assertEquals("No hay conexión a internet", after.getError());
        } finally {
            state.removeObserver(observer);
        }
    }

    /**
     * Verifies that two back-to-back login() calls reuse the same field
     * (reference equality of returned LiveData) — guard against per-call
     * reallocation.
     */
    @Test
    public void secondLoginResetsStateToLoadingOnSameInstance() {
        FakeAuthApi api = new FakeAuthApi();
        api.nextResponse = Response.success(new LoginResponse());
        NoopTokenManager tokenManager = new NoopTokenManager();
        AuthRepositoryImpl repo = new AuthRepositoryImpl(api, tokenManager);

        LiveData<UiState<LoginResponse>> state = repo.getLoginState();
        assertSame(state, repo.getLoginState());

        // First call: posts LOADING then SUCCESS.
        repo.login(new LoginRequest("u", "p"));

        // Second call: must NOT allocate a new MutableLiveData.
        LiveData<UiState<LoginResponse>> sameRef = repo.getLoginState();
        assertSame(state, sameRef);
    }

    // -- Fakes ---------------------------------------------------------------

    static final class FakeAuthApi implements AuthApi {
        Response<LoginResponse> nextResponse;
        Throwable nextFailure;
        final AtomicInteger callCount = new AtomicInteger();

        @Override
        public Call<LoginResponse> login(LoginRequest request) {
            callCount.incrementAndGet();
            return new FakeCall<>(nextResponse, nextFailure);
        }
    }

    static final class NoopTokenManager implements TokenManager {
        @Override public void saveToken(String token, String rolNombre, int userId, String nombreUsuario) { }
        @Override public String getToken() { return null; }
        @Override public String getRole() { return null; }
        @Override public int getUserId() { return -1; }
        @Override public String getNombreUsuario() { return null; }
        @Override public long decodeTokenExp() { return -1L; }
        @Override public boolean hasToken() { return false; }
        @Override public void clearToken() { }
    }

    /**
     * Minimal Call<T> stand-in that synchronously delivers a queued
     * Response or Throwable to the registered Callback.
     */
    static final class FakeCall<T> implements Call<T> {
        private final Response<T> response;
        private final Throwable failure;
        boolean executed;
        boolean canceled;

        FakeCall(Response<T> response, Throwable failure) {
            this.response = response;
            this.failure = failure;
        }

        @Override
        public Response<T> execute() throws IOException {
            executed = true;
            if (failure != null) {
                if (failure instanceof IOException) throw (IOException) failure;
                if (failure instanceof RuntimeException) throw (RuntimeException) failure;
                throw new RuntimeException(failure);
            }
            return response;
        }

        @Override
        public void enqueue(Callback<T> callback) {
            executed = true;
            if (failure != null) {
                callback.onFailure(this, failure);
            } else {
                callback.onResponse(this, response);
            }
        }

        @Override public boolean isExecuted() { return executed; }
        @Override public void cancel() { canceled = true; }
        @Override public boolean isCanceled() { return canceled; }
        @Override public Call<T> clone() { return new FakeCall<>(response, failure); }
        @Override public Request request() { return new Request.Builder().url("http://localhost/").build(); }
        @Override public okio.Timeout timeout() { return okio.Timeout.NONE; }
    }
}
