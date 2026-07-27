package com.cadence.jobservice.exception;

import com.cadence.jobservice.constant.JobStatus;
import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends JobServiceException {
    public InvalidStatusTransitionException(JobStatus from, JobStatus to) {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                "Cannot move a job from " + from + " to " + to, HttpStatus.CONFLICT);
    }
}
