package com.cadence.resumeparserservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends ResumeParserServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
