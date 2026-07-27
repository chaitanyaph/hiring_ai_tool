package com.cadence.apigatewayservice.dto;

import lombok.Builder;
import lombok.Getter;

/** Mirrors every downstream service's own {success, message, data} envelope, so a
 * Gateway-rejected request (invalid/missing JWT) looks identical on the wire to a
 * downstream 401/403 the frontend already knows how to render. */
@Getter
@Builder
public class ApiResponse<T> {
    private final boolean success;
    private final String message;
    private final T data;

    public static <T> ApiResponse<T> error(String message) {
        return ApiResponse.<T>builder().success(false).message(message).data(null).build();
    }
}
