package com.cadence.applicationservice.exception;

import com.cadence.applicationservice.constant.ApplicationStatus;
import org.springframework.http.HttpStatus;

public class InvalidStatusTransitionException extends ApplicationServiceException {
    public InvalidStatusTransitionException(ApplicationStatus from, ApplicationStatus to) {
        super(ErrorCode.INVALID_STATUS_TRANSITION,
                "Cannot move an application from " + from + " to " + to, HttpStatus.CONFLICT);
    }
}
