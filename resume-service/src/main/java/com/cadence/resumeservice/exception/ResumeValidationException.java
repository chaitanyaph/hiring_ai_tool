package com.cadence.resumeservice.exception;

import org.springframework.http.HttpStatus;

/** Every upload business rule (file type, size, limit, duplicate, profile state) surfaces through this one type. */
public class ResumeValidationException extends ResumeServiceException {
    public ResumeValidationException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.BAD_REQUEST);
    }
}
