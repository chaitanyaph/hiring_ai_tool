package com.cadence.analyticsservice.feign;

import com.cadence.analyticsservice.feign.dto.FeignApiResponse;
import com.cadence.analyticsservice.feign.dto.ResumeMatchRankingItemDto;
import com.cadence.analyticsservice.dto.response.PagedResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "resume-parser-service")
public interface ResumeParserServiceClient {

    @GetMapping("/api/v1/internal/resume-analysis/job/{jobId}")
    FeignApiResponse<PagedResponse<ResumeMatchRankingItemDto>> getJobRanking(@PathVariable("jobId") UUID jobId);
}
