package com.cadence.companyservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends CompanyServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
