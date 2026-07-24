package com.example.app_movil_gastronomia.core;

import androidx.annotation.MainThread;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public final class SessionManager {
    private final MutableLiveData<Boolean> _sessionExpired = new MutableLiveData<>(false);
    @Inject
    public SessionManager() {
    }
    public LiveData<Boolean> getSessionExpired() {
        return _sessionExpired;
    }
    public void expireSession() {
        _sessionExpired.postValue(true);
    }
    @MainThread
    public void consume() {
        _sessionExpired.setValue(false);
    }
}
