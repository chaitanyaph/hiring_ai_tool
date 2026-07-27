package com.cadence.notificationservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends NotificationServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
