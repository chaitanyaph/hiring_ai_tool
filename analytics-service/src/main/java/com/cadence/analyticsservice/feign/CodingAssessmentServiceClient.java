package com.cadence.analyticsservice.feign;

import com.cadence.analyticsservice.feign.dto.FeignApiResponse;
import com.cadence.analyticsservice.feign.dto.SubmissionHistoryItemDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/** javadoc'd on the other side as "Built for a future Analytics service" -- this is literally that. */
@FeignClient(name = "coding-assessment-service")
public interface CodingAssessmentServiceClient {

    @GetMapping("/api/v1/internal/coding-assessments/{applicationId}")
    FeignApiResponse<List<SubmissionHistoryItemDto>> getSubmissionHistory(@PathVariable("applicationId") UUID applicationId);
}
