package com.cadence.interviewmanagementservice.feign;

import com.cadence.interviewmanagementservice.feign.dto.ApplicationSummaryDto;
import com.cadence.interviewmanagementservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.UUID;

/**
 * No dedicated technical/manager/HR interview-score internal endpoint
 * exists on application-service (confirmed by research -- only resume/
 * interview(=AI)/coding/overall score fields exist). This client is
 * therefore read-only; feedback outcomes are pushed via the Kafka
 * bridge (see InterviewManagementEventProducer) onto application-
 * service's own already-wired interview.interview.completed topic.
 */
@FeignClient(name = "application-service")
public interface ApplicationServiceClient {

    @GetMapping("/internal/application/job/{jobId}")
    FeignApiResponse<List<ApplicationSummaryDto>> getApplicationsByJob(@PathVariable("jobId") UUID jobId);
}
