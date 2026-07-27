package com.cadence.notificationservice.feign;

import com.cadence.notificationservice.feign.dto.FeignApiResponse;
import com.cadence.notificationservice.feign.dto.InterviewDetailDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/** Flagged: this is an auth-protected endpoint, not internal/machine-to-machine -- see InterviewDetailDto javadoc. */
@FeignClient(name = "interview-management-service")
public interface InterviewManagementServiceClient {

    @GetMapping("/api/v1/recruiter/interviews/{id}")
    FeignApiResponse<InterviewDetailDto> getInterviewDetail(@PathVariable("id") UUID interviewId);
}
