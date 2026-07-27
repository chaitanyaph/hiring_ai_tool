package com.cadence.candidateservice.client;

import com.cadence.candidateservice.client.dto.CompanyDto;
import com.cadence.candidateservice.client.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Called once at apply() time to resolve the company name for the Application snapshot. */
@FeignClient(name = "company-service")
public interface CompanyServiceClient {

    @GetMapping("/api/v1/companies/{id}")
    FeignApiResponse<CompanyDto> getCompany(@PathVariable("id") UUID companyId);
}
