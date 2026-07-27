package com.cadence.jobservice.client;

import com.cadence.jobservice.client.dto.DepartmentDto;
import com.cadence.jobservice.client.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Only getDepartment is actually called (to resolve a department name
 * for display on the Jobs listing). Office lookup isn't wired in --
 * the Figma shows a free-text location field, not an office picker, so
 * there's no real call site for it yet.
 */
@FeignClient(name = "company-service")
public interface CompanyServiceClient {

    @GetMapping("/api/v1/departments/{id}")
    FeignApiResponse<DepartmentDto> getDepartment(@PathVariable("id") UUID departmentId);
}
