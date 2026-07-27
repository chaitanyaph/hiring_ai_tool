package com.cadence.resumeparserservice.feign;

import com.cadence.resumeparserservice.feign.dto.ApplicationSummaryDto;
import com.cadence.resumeparserservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/** Application Service's trusted-network internal endpoint, added specifically to support this service's per-job candidate ranking. */
@FeignClient(name = "application-service")
public interface ApplicationServiceClient {

    @GetMapping("/internal/application/job/{jobId}")
    FeignApiResponse<List<ApplicationSummaryDto>> getApplicationsByJob(@PathVariable("jobId") UUID jobId);
}
