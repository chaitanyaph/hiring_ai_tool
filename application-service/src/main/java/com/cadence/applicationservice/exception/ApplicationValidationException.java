package com.cadence.applicationservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain-level validation that Jakarta Bean Validation can't express:
 * duplicate apply, job not published, deadline passed, incomplete
 * profile/missing resume, non-withdrawable application.
 */
public class ApplicationValidationException extends ApplicationServiceException {
    public ApplicationValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
