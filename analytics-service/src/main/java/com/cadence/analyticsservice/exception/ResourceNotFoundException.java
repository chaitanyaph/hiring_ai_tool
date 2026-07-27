package com.cadence.analyticsservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AnalyticsServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
