package com.cadence.aiinterviewservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends AiInterviewServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
