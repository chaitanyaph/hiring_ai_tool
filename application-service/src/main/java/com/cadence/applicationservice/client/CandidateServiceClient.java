package com.cadence.applicationservice.client;

import com.cadence.applicationservice.client.dto.CandidateDto;
import com.cadence.applicationservice.client.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Called at apply() time to validate "profile completed" / "resume
 * exists" and to snapshot the candidate's name/email onto the
 * Application row for recruiter-side search. Hits Candidate Service's
 * trusted-network /summary endpoint (no PII beyond name/email, no
 * bearer token required -- same trust model as Company Service).
 */
@FeignClient(name = "candidate-service")
public interface CandidateServiceClient {

    @GetMapping("/api/v1/candidates/{candidateId}/summary")
    FeignApiResponse<CandidateDto> getCandidateSummary(@PathVariable("candidateId") UUID candidateId);
}
