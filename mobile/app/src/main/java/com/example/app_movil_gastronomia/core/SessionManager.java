package com.example.app_movil_gastronomia.core;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

/**
 * App-wide session signal used to coordinate session-expiration across the
 * OkHttp interceptor (which detects 401 responses off the main thread) and
 * the host Activity (which is the only place that can safely navigate).
 *
 * The event is a one-shot flag held in a single {@link MutableLiveData}
 * instance. Consumers MUST call {@link #consume()} after handling the event
 * so the flag is re-armed for future expirations.
 */
@Singleton
public final class SessionManager {

    private final MutableLiveData<Boolean> _sessionExpired = new MutableLiveData<>(false);

    @Inject
    public SessionManager() {
    }

    /** Exposes the session-expired flag as immutable LiveData. */
    public LiveData<Boolean> getSessionExpired() {
        return _sessionExpired;
    }

    /**
     * Signals that the current session has expired.
     * Uses {@link MutableLiveData#postValue} because it is called from
     * OkHttp's background thread via {@code AuthInterceptor}.
     */
    public void expireSession() {
        _sessionExpired.postValue(true);
    }

    /** Re-arms the session-expired flag to {@code false} after the consumer handles it. */
    @MainThread
    public void consume() {
        _sessionExpired.setValue(false);
    }
}
