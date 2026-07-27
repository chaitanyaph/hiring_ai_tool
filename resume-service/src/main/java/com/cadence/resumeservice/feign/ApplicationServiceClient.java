package com.cadence.resumeservice.feign;

import com.cadence.resumeservice.feign.dto.FeignApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.UUID;

/**
 * Two narrow, real call sites: (1) block deleting a resume that's
 * still attached to a non-terminal application, and (2) scope a
 * recruiter's preview/download access to candidates who actually
 * applied to their own company.
 */
@FeignClient(name = "application-service")
public interface ApplicationServiceClient {

    @GetMapping("/internal/application/resume/{resumeId}/in-use")
    FeignApiResponse<Boolean> isResumeInUse(@PathVariable("resumeId") UUID resumeId);

    @GetMapping("/internal/application/exists")
    FeignApiResponse<Boolean> hasApplicationFromCandidateToCompany(
            @RequestParam("candidateId") UUID candidateId, @RequestParam("companyId") UUID companyId);
}
