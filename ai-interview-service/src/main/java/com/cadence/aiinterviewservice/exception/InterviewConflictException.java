package com.cadence.aiinterviewservice.exception;

import org.springframework.http.HttpStatus;

/** An interview action attempted in an incompatible state -- e.g. starting an already-started/expired session, answering out of order, finishing twice. */
public class InterviewConflictException extends AiInterviewServiceException {
    public InterviewConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
