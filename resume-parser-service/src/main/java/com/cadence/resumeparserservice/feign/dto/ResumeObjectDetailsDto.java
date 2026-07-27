package com.cadence.resumeparserservice.feign.dto;

import lombok.*;

import java.util.UUID;

/**
 * Mirrors Resume Service's ResumeObjectDetailsResponse -- bucket/object
 * coordinates this service uses to read the PDF straight out of the
 * shared MinIO instance with its own credentials, per Resume Service's
 * own documented intent for this exact endpoint.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeObjectDetailsDto {
    private UUID resumeId;
    private String bucketName;
    private String objectName;
    private String mimeType;
    private String checksum;
    private long fileSize;
}
