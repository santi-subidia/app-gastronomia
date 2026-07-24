package com.example.app_movil_gastronomia.core;

import androidx.annotation.NonNull;

import java.io.IOException;

import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

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
