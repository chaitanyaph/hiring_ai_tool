package com.cadence.jobservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends JobServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
