package com.cadence.codingassessmentservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends CodingAssessmentServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
