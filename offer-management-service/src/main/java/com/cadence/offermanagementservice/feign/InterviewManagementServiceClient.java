package com.cadence.offermanagementservice.feign;

import com.cadence.offermanagementservice.feign.dto.FeignApiResponse;
import com.cadence.offermanagementservice.feign.dto.InterviewDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

@FeignClient(name = "interview-management-service")
public interface InterviewManagementServiceClient {

    @GetMapping("/api/v1/recruiter/interviews/{id}")
    FeignApiResponse<InterviewDetailDto> getInterviewDetail(@PathVariable("id") UUID interviewId);
}
