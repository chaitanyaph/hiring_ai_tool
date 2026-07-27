package com.cadence.interviewmanagementservice.exception;

import lombok.*;

import java.time.Instant;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApiError {
    private Instant timestamp;
    private int status;
    private String error;
    private ErrorCode errorCode;
    private String message;
    private String path;
    private List<FieldValidationError> fieldErrors;

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class FieldValidationError {
        private String field;
        private String message;
    }
}
