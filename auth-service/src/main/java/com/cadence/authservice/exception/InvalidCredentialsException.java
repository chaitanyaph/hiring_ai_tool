package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidCredentialsException extends AuthServiceException {
    public InvalidCredentialsException() {
        super(ErrorCode.INVALID_CREDENTIALS, "Invalid email or password", HttpStatus.UNAUTHORIZED);
    }
}
