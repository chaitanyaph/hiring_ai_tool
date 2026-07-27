package com.cadence.authservice.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.time.Instant;

/** Generic envelope for simple message-only responses (e.g. "email sent"). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private boolean success;
    private String message;
    private T data;
    @Builder.Default
    private Instant timestamp = Instant.now();

    public static <T> ApiResponse<T> ok(String message, T data) {
        return ApiResponse.<T>builder().success(true).message(message).data(data).build();
    }

    public static <T> ApiResponse<T> ok(String message) {
        return ApiResponse.<T>builder().success(true).message(message).build();
    }
}
