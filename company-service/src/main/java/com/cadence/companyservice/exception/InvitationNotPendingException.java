package com.cadence.companyservice.exception;

import org.springframework.http.HttpStatus;

public class InvitationNotPendingException extends CompanyServiceException {
    public InvitationNotPendingException(String currentStatus) {
        super(ErrorCode.INVITATION_NOT_PENDING,
                "This invitation is " + currentStatus + " and can no longer be modified", HttpStatus.CONFLICT);
    }
}
