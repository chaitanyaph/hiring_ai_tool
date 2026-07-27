package com.cadence.resumeparserservice.exception;

import org.springframework.http.HttpStatus;

/** Retry called on a resume that isn't FAILED, or a concurrent processing attempt is already holding the idempotency lock. */
public class ParsingConflictException extends ResumeParserServiceException {
    public ParsingConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
