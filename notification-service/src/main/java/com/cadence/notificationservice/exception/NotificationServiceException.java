package com.cadence.notificationservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class NotificationServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public NotificationServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
