package com.cadence.aiinterviewservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends AiInterviewServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
