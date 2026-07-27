package com.cadence.notificationservice.feign;

import com.cadence.notificationservice.feign.dto.CompanyDto;
import com.cadence.notificationservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "company-service")
public interface CompanyServiceClient {

    @GetMapping("/api/v1/companies/{id}")
    FeignApiResponse<CompanyDto> getCompany(@PathVariable("id") UUID companyId);
}
