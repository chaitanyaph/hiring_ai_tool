package com.cadence.resumeservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends ResumeServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
