package com.cadence.offermanagementservice.exception;

import org.springframework.http.HttpStatus;

public class ResourceNotFoundException extends OfferManagementServiceException {
    public ResourceNotFoundException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.NOT_FOUND);
    }
}
