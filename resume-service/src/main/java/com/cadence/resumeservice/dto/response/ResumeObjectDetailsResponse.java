package com.cadence.resumeservice.dto.response;

import lombok.*;

import java.util.UUID;

/**
 * Internal, trusted-network only -- lets the future Resume Parser
 * Service read the object straight out of MinIO with its own
 * credentials instead of proxying every download through this
 * service's HTTP path, which matters at parsing-pipeline volume.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeObjectDetailsResponse {
    private UUID resumeId;
    private String bucketName;
    private String objectName;
    private String mimeType;
    private String checksum;
    private long fileSize;
}
