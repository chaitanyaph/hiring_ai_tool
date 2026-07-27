package com.cadence.resumeparserservice.feign;

import com.cadence.resumeparserservice.feign.dto.FeignApiResponse;
import com.cadence.resumeparserservice.feign.dto.ResumeMetadataDto;
import com.cadence.resumeparserservice.feign.dto.ResumeObjectDetailsDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.UUID;

/**
 * Both endpoints are Resume Service's trusted-network, permitAll
 * internal API -- built specifically for this service to consume
 * (metadata for validation, object coordinates to read the PDF
 * straight out of the shared MinIO instance instead of proxying every
 * download through Resume Service's HTTP path).
 */
@FeignClient(name = "resume-service")
public interface ResumeServiceClient {

    @GetMapping("/api/v1/internal/resumes/{resumeId}")
    FeignApiResponse<ResumeMetadataDto> getResumeMetadata(@PathVariable("resumeId") UUID resumeId);

    @GetMapping("/api/v1/internal/resumes/{resumeId}/object")
    FeignApiResponse<ResumeObjectDetailsDto> getObjectDetails(@PathVariable("resumeId") UUID resumeId);
}
