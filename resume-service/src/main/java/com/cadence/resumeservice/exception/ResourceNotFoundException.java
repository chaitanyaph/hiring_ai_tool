package com.cadence.resumeservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ResumeServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
