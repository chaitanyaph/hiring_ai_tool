package com.cadence.interviewmanagementservice.exception;

import org.springframework.http.HttpStatus;

/** An interview action attempted in an incompatible state -- e.g. rescheduling/cancelling a completed interview, submitting feedback twice. */
public class InterviewConflictException extends InterviewManagementServiceException {
    public InterviewConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
