package com.cadence.resumeservice.exception;

import org.springframework.http.HttpStatus;

/** State conflicts, not input validation: resume limit reached, duplicate checksum, or resume still in use. */
public class ResumeConflictException extends ResumeServiceException {
    public ResumeConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
