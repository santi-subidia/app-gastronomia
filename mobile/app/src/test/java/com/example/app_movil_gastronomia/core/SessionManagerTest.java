package com.example.app_movil_gastronomia.core;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import androidx.arch.core.executor.testing.InstantTaskExecutorRule;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;

import org.junit.Rule;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public class SessionManagerTest {

    @Rule
    public InstantTaskExecutorRule instantTaskExecutorRule = new InstantTaskExecutorRule();

    @Test
    public void initialValueIsFalse() {
        SessionManager sessionManager = new SessionManager();
        assertNotNull(sessionManager.getSessionExpired());
        assertEquals(Boolean.FALSE, sessionManager.getSessionExpired().getValue());
    }

    @Test
    public void expireSessionEmitsTrue() {
        SessionManager sessionManager = new SessionManager();

        sessionManager.expireSession();

        assertEquals(Boolean.TRUE, sessionManager.getSessionExpired().getValue());
    }

    @Test
    public void consumeEmitsFalse() {
        SessionManager sessionManager = new SessionManager();
        sessionManager.expireSession();

        sessionManager.consume();

        assertEquals(Boolean.FALSE, sessionManager.getSessionExpired().getValue());
    }

    @Test
    public void expireSessionIsIdempotent() {
        SessionManager sessionManager = new SessionManager();
        sessionManager.expireSession();

        sessionManager.expireSession();
        sessionManager.expireSession();

        assertEquals(Boolean.TRUE, sessionManager.getSessionExpired().getValue());
    }

    @Test
    public void observerReceivesEmissions() {
        SessionManager sessionManager = new SessionManager();
        AtomicReference<Boolean> latest = new AtomicReference<>();
        latest.set(null);

        Observer<Boolean> observer = latest::set;
        sessionManager.getSessionExpired().observeForever(observer);
        try {
            sessionManager.expireSession();
            assertEquals(Boolean.TRUE, latest.get());

            sessionManager.consume();
            assertEquals(Boolean.FALSE, latest.get());
        } finally {
            sessionManager.getSessionExpired().removeObserver(observer);
        }
    }

    @Test
    public void sameLiveDataInstanceIsExposed() {
        SessionManager sessionManager = new SessionManager();
        LiveData<Boolean> first = sessionManager.getSessionExpired();
        LiveData<Boolean> second = sessionManager.getSessionExpired();
        assertTrue("getSessionExpired must return the same instance", first == second);
    }

    /**
     * Verifica que expireSession() sea seguro al llamarse desde un
     * background thread (simulating OkHttp's interceptor thread on 401).
     * hilo secundario. Usa postValue(), por lo que el observador se ejecuta en el hilo principal.
     */
    @Test
    public void expireSession_fromBackgroundThread_doesNotCrash() throws Exception {
        SessionManager sessionManager = new SessionManager();
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<Boolean> received = new AtomicReference<>();

        sessionManager.getSessionExpired().observeForever(value -> {
            received.set(value);
            if (Boolean.TRUE.equals(value)) {
                latch.countDown();
            }
        });

        new Thread(() -> sessionManager.expireSession()).start();

        assertTrue("Observer should receive TRUE within 2 seconds",
                latch.await(2, TimeUnit.SECONDS));
        assertEquals(Boolean.TRUE, received.get());
    }
}
