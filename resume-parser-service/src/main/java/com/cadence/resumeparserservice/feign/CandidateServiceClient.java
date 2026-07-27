package com.cadence.resumeparserservice.feign;

import com.cadence.resumeparserservice.feign.dto.CandidateDto;
import com.cadence.resumeparserservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Called before parsing to confirm the candidate still exists and is
 * ACTIVE. Hits Candidate Service's trusted-network /summary endpoint --
 * same trust model every other service uses for this exact call.
 */
@FeignClient(name = "candidate-service")
public interface CandidateServiceClient {

    @GetMapping("/api/v1/candidates/{candidateId}/summary")
    FeignApiResponse<CandidateDto> getCandidateSummary(@PathVariable("candidateId") UUID candidateId);
}
