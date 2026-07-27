package com.cadence.companyservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateCompanyNameException extends CompanyServiceException {
    public DuplicateCompanyNameException(String name) {
        super(ErrorCode.DUPLICATE_COMPANY_NAME, "A company named '" + name + "' already exists", HttpStatus.CONFLICT);
    }
}
