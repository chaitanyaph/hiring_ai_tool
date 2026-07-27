package com.cadence.candidateservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain-level validation that Jakarta Bean Validation can't express
 * (e.g. "job must be PUBLISHED to apply", "cannot apply twice to the
 * same job", "cannot withdraw an application already in a terminal
 * stage").
 */
public class CandidateValidationException extends CandidateServiceException {
    public CandidateValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
