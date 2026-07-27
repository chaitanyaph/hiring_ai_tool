package com.cadence.jobservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class JobServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public JobServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
