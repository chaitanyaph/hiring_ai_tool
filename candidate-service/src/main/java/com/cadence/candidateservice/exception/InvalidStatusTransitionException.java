package com.cadence.candidateservice.exception;

import com.cadence.candidateservice.constant.ApplicationStatus;
import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends CandidateServiceException {
    public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                "Cannot move an application from " + from + " to " + to, HttpStatus.CONFLICT);
    }
}
