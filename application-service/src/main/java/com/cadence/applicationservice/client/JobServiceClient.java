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
 */
@FeignClient(name = "job-service")
public interface JobServiceClient {

    @GetMapping("/api/v1/jobs/{id}")
    FeignApiResponse<JobDto> getJob(@PathVariable("id") UUID jobId);
}
