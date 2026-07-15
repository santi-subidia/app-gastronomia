package com.example.app_movil_gastronomia.core;

/**
 * Generic UI state wrapper for LiveData-driven screens.
 * Encapsulates loading, success, and error states with typed data.
 *
 * @param <T> the type of data held on success
 */
public class UiState<T> {

    public enum Status { LOADING, SUCCESS, ERROR }

    private final Status status;
    private final T data;
    private final String error;
    private final String errorCode;

    private UiState(Status status, T data, String error, String errorCode) {
        this.status = status;
        this.data = data;
        this.error = error;
        this.errorCode = errorCode;
    }

    public static <T> UiState<T> loading() {
        return new UiState<>(Status.LOADING, null, null, null);
    }

    public static <T> UiState<T> success(T data) {
        return new UiState<>(Status.SUCCESS, data, null, null);
    }

    public static <T> UiState<T> error(String error) {
        return error(error, null);
    }

    public static <T> UiState<T> error(String error, String errorCode) {
        return new UiState<>(Status.ERROR, null, error, errorCode);
    }

    public Status getStatus() {
        return status;
    }

    public T getData() {
        return data;
    }

    public String getError() {
        return error;
    }

    public String getErrorCode() {
        return errorCode;
    }
}
