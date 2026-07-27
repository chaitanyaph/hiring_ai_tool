package com.cadence.authservice.exception;

import org.springframework.http.HttpStatus;

public class RoleNotFoundException extends AuthServiceException {
    public RoleNotFoundException(String role) {
        super(ErrorCode.ROLE_NOT_FOUND, "Role not found: " + role, HttpStatus.NOT_FOUND);
    }
}
