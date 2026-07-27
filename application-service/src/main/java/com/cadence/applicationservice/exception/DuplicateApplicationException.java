package com.cadence.applicationservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateApplicationException extends ApplicationServiceException {
    public DuplicateApplicationException() {
        super(ErrorCode.DUPLICATE_APPLICATION, "This candidate has already applied to this job", HttpStatus.CONFLICT);
    }
}
