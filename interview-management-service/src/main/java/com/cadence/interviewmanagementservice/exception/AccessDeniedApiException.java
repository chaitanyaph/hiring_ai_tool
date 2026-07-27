package com.cadence.interviewmanagementservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends InterviewManagementServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
