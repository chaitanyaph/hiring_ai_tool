package com.cadence.interviewmanagementservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class InterviewManagementServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public InterviewManagementServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
