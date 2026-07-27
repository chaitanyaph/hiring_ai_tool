package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class UserAlreadyExistsException extends AuthServiceException {
    public UserAlreadyExistsException(String email) {
        super(ErrorCode.USER_ALREADY_EXISTS, "A user already exists with email: " + email, HttpStatus.CONFLICT);
    }
}
