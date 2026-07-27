package com.cadence.analyticsservice.feign;

import com.cadence.analyticsservice.feign.dto.ApplicationSummaryDto;
import com.cadence.analyticsservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/** No aggregate-counts endpoint exists anywhere on application-service (confirmed by research) -- this service's whole reason to exist is to avoid needing one, by pre-aggregating from events instead. */
@FeignClient(name = "application-service")
public interface ApplicationServiceClient {

    @GetMapping("/internal/application/job/{jobId}")
    FeignApiResponse<List<ApplicationSummaryDto>> getApplicationsByJob(@PathVariable("jobId") UUID jobId);
}
