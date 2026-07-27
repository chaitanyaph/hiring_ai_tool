package com.cadence.companyservice.service;

import com.cadence.companyservice.dto.request.CreateCompanyRequest;
import com.cadence.companyservice.dto.request.UpdateCompanyRequest;
import com.cadence.companyservice.dto.response.CompanyResponse;

import java.util.UUID;

public interface CompanyService {
    CompanyResponse createCompany(CreateCompanyRequest request, String actor);
    CompanyResponse getCompany(UUID companyId);
    CompanyResponse updateCompany(UUID companyId, UpdateCompanyRequest request, String actor);
    void deleteCompany(UUID companyId, String actor);
}
