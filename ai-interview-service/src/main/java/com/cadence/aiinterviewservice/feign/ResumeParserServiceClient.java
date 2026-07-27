package com.cadence.aiinterviewservice.feign;

import com.cadence.aiinterviewservice.feign.dto.FeignApiResponse;
import com.cadence.aiinterviewservice.feign.dto.ResumeMatchDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "resume-parser-service")
public interface ResumeParserServiceClient {

    @GetMapping("/api/v1/internal/resume-analysis/{applicationId}")
    FeignApiResponse<ResumeMatchDto> getResumeMatch(@PathVariable("applicationId") UUID applicationId);
}
