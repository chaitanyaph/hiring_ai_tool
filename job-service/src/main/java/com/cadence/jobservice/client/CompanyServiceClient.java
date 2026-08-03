package com.cadence.jobservice.client;

import com.cadence.jobservice.client.dto.CompanyDto;
import com.cadence.jobservice.client.dto.DepartmentDto;
import com.cadence.jobservice.client.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * getDepartment resolves a department name for display on the Jobs
 * listing. getCompany resolves a company name for the candidate-facing
 * public job browse (job-service only stores companyId, never a name).
 * Office lookup isn't wired in -- the Figma shows a free-text location
 * field, not an office picker, so there's no real call site for it yet.
 */
@FeignClient(name = "company-service")
public interface CompanyServiceClient {

    @GetMapping("/api/v1/departments/{id}")
    FeignApiResponse<DepartmentDto> getDepartment(@PathVariable("id") UUID departmentId);

    @GetMapping("/api/v1/companies/{id}")
    FeignApiResponse<CompanyDto> getCompany(@PathVariable("id") UUID companyId);
}
