package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class TokenExpiredException extends AuthServiceException {
    public TokenExpiredException(String message) {
        super(ErrorCode.TOKEN_EXPIRED, message, HttpStatus.UNAUTHORIZED);
    }
}
