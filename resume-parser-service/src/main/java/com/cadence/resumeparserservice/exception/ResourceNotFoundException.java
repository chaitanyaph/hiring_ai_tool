package com.cadence.resumeparserservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends ResumeParserServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
