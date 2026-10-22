package com.tt.labexam.dto;

/** Consistent JSON envelope used by every endpoint. */
public record ApiResponse<T>(boolean success, String message, T data, long timestamp) {

    public static <T> ApiResponse<T> ok(T data, String message) {
        return new ApiResponse<>(true, message, data, System.currentTimeMillis());
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, message, null, System.currentTimeMillis());
    }
}
