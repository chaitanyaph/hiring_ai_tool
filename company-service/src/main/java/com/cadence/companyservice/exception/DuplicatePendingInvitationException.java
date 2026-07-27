package com.cadence.companyservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicatePendingInvitationException extends CompanyServiceException {
    public DuplicatePendingInvitationException(String email) {
        super(ErrorCode.DUPLICATE_PENDING_INVITATION,
                "A pending invitation already exists for " + email, HttpStatus.CONFLICT);
    }
}
