package com.cadence.offermanagementservice.exception;

import org.springframework.http.HttpStatus;

/** An offer action attempted in an incompatible state -- e.g. editing a sent offer, approving a non-pending offer, accepting an already-declined offer. */
public class OfferConflictException extends OfferManagementServiceException {
    public OfferConflictException(ErrorCode errorCode, String message) {
        super(errorCode, message, HttpStatus.CONFLICT);
    }
}
