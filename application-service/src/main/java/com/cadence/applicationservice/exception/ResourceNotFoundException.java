package com.cadence.applicationservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ApplicationServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
