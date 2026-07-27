package com.cadence.applicationservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class ApplicationServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public ApplicationServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
