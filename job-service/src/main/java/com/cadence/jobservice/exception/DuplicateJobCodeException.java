package com.cadence.jobservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateJobCodeException extends JobServiceException {
    public DuplicateJobCodeException(String jobCode) {
        super(ErrorCode.DUPLICATE_JOB_CODE, "Job code '" + jobCode + "' already exists for this company", HttpStatus.CONFLICT);
    }
}
