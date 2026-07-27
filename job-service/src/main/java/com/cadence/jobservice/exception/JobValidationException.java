package com.cadence.jobservice.exception;

import org.springframework.http.HttpStatus;

/**
 * Domain-level validation that Jakarta Bean Validation can't express
 * (cross-field rules like min<=max salary/experience, deadline not in
 * the past, openings > 0, or "must have skills/pipeline before publish").
 */
public class JobValidationException extends JobServiceException {
    public JobValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
