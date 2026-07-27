package com.cadence.analyticsservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AnalyticsServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public AnalyticsServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
