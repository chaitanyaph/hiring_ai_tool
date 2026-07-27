package com.cadence.analyticsservice.feign;

import com.cadence.analyticsservice.feign.dto.CandidateDto;
import com.cadence.analyticsservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "candidate-service")
public interface CandidateServiceClient {

    @GetMapping("/api/v1/candidates/{candidateId}/summary")
    FeignApiResponse<CandidateDto> getCandidateSummary(@PathVariable("candidateId") UUID candidateId);
}
