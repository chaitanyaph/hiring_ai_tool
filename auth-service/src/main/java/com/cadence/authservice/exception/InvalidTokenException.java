package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidTokenException extends AuthServiceException {
    public InvalidTokenException(String message) {
        super(ErrorCode.INVALID_TOKEN, message, HttpStatus.UNAUTHORIZED);
    }
}
