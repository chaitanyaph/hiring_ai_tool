package com.cadence.companyservice.exception;

import org.springframework.http.HttpStatus;

public class InvitationExpiredException extends CompanyServiceException {
    public InvitationExpiredException() {
        super(ErrorCode.INVITATION_EXPIRED, "This invitation has expired", HttpStatus.GONE);
    }
}
