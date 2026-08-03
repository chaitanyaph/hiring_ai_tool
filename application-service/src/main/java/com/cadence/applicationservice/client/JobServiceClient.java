package com.cadence.applicationservice.client;

import com.cadence.applicationservice.client.dto.FeignApiResponse;
import com.cadence.applicationservice.client.dto.JobDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Called at apply() time: confirms the job is PUBLISHED and its
 * application deadline hasn't passed, and snapshots the title onto the
 * new Application row so search/display work without a live join.
 *
 * Hits job-service's internal, trusted-network endpoint rather than its
 * human-facing /api/v1/jobs/{id} -- that one is class-level restricted to
 * recruiter/admin roles and requires a forwarded bearer token neither this
 * Feign client nor any other in this codebase sends, so calling it here
 * (this method runs for a CANDIDATE's apply request) always 401s.
 */
@FeignClient(name = "job-service")
public interface JobServiceClient {

    @GetMapping("/api/v1/internal/jobs/{id}")
    FeignApiResponse<JobDto> getJob(@PathVariable("id") UUID jobId);
}
