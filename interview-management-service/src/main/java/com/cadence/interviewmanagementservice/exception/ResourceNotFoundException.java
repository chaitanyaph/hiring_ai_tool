package com.cadence.interviewmanagementservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends InterviewManagementServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
