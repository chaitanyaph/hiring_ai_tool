package com.cadence.companyservice.exception;

import org.springframework.http.HttpStatus;

public class DuplicateDepartmentException extends CompanyServiceException {
    public DuplicateDepartmentException(String name) {
        super(ErrorCode.DUPLICATE_DEPARTMENT_NAME, "A department named '" + name + "' already exists for this company", HttpStatus.CONFLICT);
    }
}
