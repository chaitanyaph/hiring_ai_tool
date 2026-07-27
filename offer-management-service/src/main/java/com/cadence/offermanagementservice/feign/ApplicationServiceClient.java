package com.cadence.offermanagementservice.feign;

import com.cadence.offermanagementservice.feign.dto.ApplicationSummaryDto;
import com.cadence.offermanagementservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/** No dedicated single-application internal endpoint exists (confirmed by research) -- only this job-scoped list. Enrichment filters client-side by applicationId. */
@FeignClient(name = "application-service")
public interface ApplicationServiceClient {

    @GetMapping("/internal/application/job/{jobId}")
    FeignApiResponse<List<ApplicationSummaryDto>> getApplicationsByJob(@PathVariable("jobId") UUID jobId);
}
