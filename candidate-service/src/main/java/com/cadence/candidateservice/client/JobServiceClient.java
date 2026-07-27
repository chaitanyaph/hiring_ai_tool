package com.cadence.candidateservice.client;

import com.cadence.candidateservice.client.dto.FeignApiResponse;
import com.cadence.candidateservice.client.dto.JobDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Called once at apply() time: to confirm the job is PUBLISHED (not
 * DRAFT/CLOSED/ARCHIVED) and to snapshot title/location/employmentType
 * onto the new Application row so it stays readable even if Job Service
 * later archives or deletes the job.
 */
@FeignClient(name = "job-service")
public interface JobServiceClient {

    @GetMapping("/api/v1/jobs/{id}")
    FeignApiResponse<JobDto> getJob(@PathVariable("id") UUID jobId);
}
