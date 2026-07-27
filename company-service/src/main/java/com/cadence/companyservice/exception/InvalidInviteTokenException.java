package com.cadence.companyservice.exception;

import org.springframework.http.HttpStatus;

public class InvalidInviteTokenException extends CompanyServiceException {
    public InvalidInviteTokenException() {
        super(ErrorCode.INVALID_INVITE_TOKEN, "Invite token is invalid", HttpStatus.NOT_FOUND);
    }
}
