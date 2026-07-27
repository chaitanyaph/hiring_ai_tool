package com.cadence.applicationservice.client;

import com.cadence.applicationservice.client.dto.CompanyDto;
import com.cadence.applicationservice.client.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Used to validate a companyId is real and to resolve its display name where needed (e.g. future reporting). */
@FeignClient(name = "company-service")
public interface CompanyServiceClient {

    @GetMapping("/api/v1/companies/{id}")
    FeignApiResponse<CompanyDto> getCompany(@PathVariable("id") UUID companyId);
}
