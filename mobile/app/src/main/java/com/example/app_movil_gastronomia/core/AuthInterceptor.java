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

        // Only inject the JWT if the request is going to our backend API.
        // Third-party APIs (like OSRM or MapTiler) will reject the SSL handshake or return 400s if sent unexpected Authorization headers.
        boolean isOurApi = originalRequest.url().toString().contains("api/") || originalRequest.url().toString().contains("hubs/");

        Request.Builder requestBuilder = originalRequest.newBuilder();

        if (isOurApi) {
            String token = tokenManager.getToken();
            if (token != null) {
                requestBuilder.header("Authorization", "Bearer " + token);
            }
        }

        Response response = chain.proceed(requestBuilder.build());

        if (response.code() == 401 && isOurApi) {
            tokenManager.clearToken();
            sessionManager.expireSession();
        }

        return response;
    }
}
