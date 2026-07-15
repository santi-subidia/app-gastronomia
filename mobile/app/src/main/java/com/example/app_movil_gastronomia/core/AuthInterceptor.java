package com.example.app_movil_gastronomia.core;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

/**
 * OkHttp interceptor that injects the stored JWT as an Authorization Bearer header
 * into every outgoing request. On a 401 response it clears the local session
 * AND signals {@link SessionManager#expireSession()} so the host Activity
 * (the only place with a NavController) can navigate to login.
 *
 * <p>Navigation is intentionally NOT performed here: interceptors run on OkHttp
 * worker threads and have no access to the UI layer.</p>
 */
public class AuthInterceptor implements Interceptor {

    private final TokenManager tokenManager;
    private final SessionManager sessionManager;

    public AuthInterceptor(TokenManager tokenManager, SessionManager sessionManager) {
        this.tokenManager = tokenManager;
        this.sessionManager = sessionManager;
    }

    @NonNull
    @Override
    public Response intercept(@NonNull Chain chain) throws IOException {
        Request originalRequest = chain.request();

        String token = tokenManager.getToken();
        Request.Builder requestBuilder = originalRequest.newBuilder();

        if (token != null) {
            requestBuilder.header("Authorization", "Bearer " + token);
        }

        Response response = chain.proceed(requestBuilder.build());

        if (response.code() == 401) {
            tokenManager.clearToken();
            sessionManager.expireSession();
        }

        return response;
    }
}
