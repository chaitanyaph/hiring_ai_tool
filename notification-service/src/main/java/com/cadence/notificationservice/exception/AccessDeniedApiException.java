package com.cadence.notificationservice.exception;

import org.springframework.http.HttpStatus;

public class AccessDeniedApiException extends NotificationServiceException {
    public AccessDeniedApiException(String message) {
        super(ErrorCode.ACCESS_DENIED, message, HttpStatus.FORBIDDEN);
    }
}
