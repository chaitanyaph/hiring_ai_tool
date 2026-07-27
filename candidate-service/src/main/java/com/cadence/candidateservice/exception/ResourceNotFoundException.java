package com.cadence.candidateservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends CandidateServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
