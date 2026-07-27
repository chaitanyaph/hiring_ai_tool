package com.cadence.applicationservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends ApplicationServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
