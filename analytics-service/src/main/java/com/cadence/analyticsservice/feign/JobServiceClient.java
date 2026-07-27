package com.cadence.analyticsservice.feign;

import com.cadence.analyticsservice.feign.dto.FeignApiResponse;
import com.cadence.analyticsservice.feign.dto.JobDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "job-service")
public interface JobServiceClient {

    @GetMapping("/api/v1/internal/jobs/{jobId}")
    FeignApiResponse<JobDetailDto> getJobDetail(@PathVariable("jobId") UUID jobId);
}
