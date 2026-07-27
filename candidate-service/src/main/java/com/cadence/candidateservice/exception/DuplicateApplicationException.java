package com.cadence.candidateservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateApplicationException extends CandidateServiceException {
    public DuplicateApplicationException() {
        super(ErrorCode.DUPLICATE_APPLICATION, "You have already applied to this job", HttpStatus.CONFLICT);
    }
}
