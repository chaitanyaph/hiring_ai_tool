package com.cadence.aiinterviewservice.feign;

import com.cadence.aiinterviewservice.feign.dto.ApplicationSummaryDto;
import com.cadence.aiinterviewservice.feign.dto.FeignApiResponse;
import com.cadence.aiinterviewservice.feign.dto.ScoreUpdateRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.UUID;

@FeignClient(name = "application-service")
public interface ApplicationServiceClient {

    @GetMapping("/internal/application/job/{jobId}")
    FeignApiResponse<List<ApplicationSummaryDto>> getApplicationsByJob(@PathVariable("jobId") UUID jobId);

    @PutMapping("/internal/application/{id}/interview-score")
    FeignApiResponse<Object> updateInterviewScore(@PathVariable("id") UUID applicationId, @RequestBody ScoreUpdateRequest request);
}
