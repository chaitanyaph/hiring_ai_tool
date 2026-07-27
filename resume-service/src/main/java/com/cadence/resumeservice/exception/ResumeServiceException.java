package com.cadence.resumeservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ResumeServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public ResumeServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
