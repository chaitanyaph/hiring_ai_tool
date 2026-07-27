package com.cadence.analyticsservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends AnalyticsServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
