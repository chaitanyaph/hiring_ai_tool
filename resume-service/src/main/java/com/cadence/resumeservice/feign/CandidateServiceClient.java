package com.cadence.resumeservice.feign;

import com.cadence.resumeservice.feign.dto.CandidateDto;
import com.cadence.resumeservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Called at upload time to validate the candidate exists and is
 * ACTIVE. Hits Candidate Service's trusted-network /summary endpoint
 * (no PII beyond name/email, no bearer token required -- same trust
 * model as Company Service).
 */
@FeignClient(name = "candidate-service")
public interface CandidateServiceClient {

    @GetMapping("/api/v1/candidates/{candidateId}/summary")
    FeignApiResponse<CandidateDto> getCandidateSummary(@PathVariable("candidateId") UUID candidateId);
}
