package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AuthServiceException {
    public ResourceNotFoundException(String message) {
        super(ErrorCode.USER_NOT_FOUND, message, HttpStatus.NOT_FOUND);
    }
}
