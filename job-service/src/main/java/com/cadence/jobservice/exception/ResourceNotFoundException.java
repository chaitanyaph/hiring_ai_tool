package com.cadence.jobservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends JobServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
