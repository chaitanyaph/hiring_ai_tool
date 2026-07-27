package com.cadence.authservice.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class AuthServiceException extends RuntimeException {

    private final ErrorCode errorCode;
    private final HttpStatus httpStatus;

    public AuthServiceException(ErrorCode errorCode, String message, HttpStatus httpStatus) {
        super(message);
        this.errorCode = errorCode;
        this.httpStatus = httpStatus;
    }
}
