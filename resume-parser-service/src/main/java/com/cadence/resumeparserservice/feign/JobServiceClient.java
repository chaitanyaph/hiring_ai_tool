package com.cadence.resumeparserservice.feign;

import com.cadence.resumeparserservice.feign.dto.FeignApiResponse;
import com.cadence.resumeparserservice.feign.dto.JobDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Job Service's trusted-network internal endpoint, added specifically to support this service's resume-to-job matching. */
@FeignClient(name = "job-service")
public interface JobServiceClient {

    @GetMapping("/api/v1/internal/jobs/{jobId}")
    FeignApiResponse<JobDetailDto> getJobDetail(@PathVariable("jobId") UUID jobId);
}
