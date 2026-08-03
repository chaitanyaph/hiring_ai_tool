package com.cadence.applicationservice.client;

import com.cadence.applicationservice.client.dto.FeignApiResponse;
import com.cadence.applicationservice.client.dto.ResumeDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Called at apply() time to confirm the candidate-selected resumeId
 * actually belongs to them and is still active. Hits Resume Service's
 * trusted-network /internal endpoint (permitAll, no bearer token
 * required) -- same trust model as CandidateServiceClient/JobServiceClient.
 */
@FeignClient(name = "resume-service")
public interface ResumeServiceClient {

    @GetMapping("/api/v1/internal/resumes/{resumeId}")
    FeignApiResponse<ResumeDto> getResume(@PathVariable("resumeId") UUID resumeId);
}
