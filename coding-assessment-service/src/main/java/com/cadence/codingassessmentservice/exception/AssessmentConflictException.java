package com.cadence.codingassessmentservice.exception;

import org.springframework.http.HttpStatus;

/** An assessment/candidate-assessment action attempted in an incompatible state -- e.g. publishing an already-published assessment, starting an already-started/expired attempt, submitting without accepting rules. */
public class AssessmentConflictException extends CodingAssessmentServiceException {
    public AssessmentConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
