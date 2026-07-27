package com.cadence.resumeparserservice.feign.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Mirrors Resume Service's ResumeResponse -- only the fields this service actually reads. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeMetadataDto {
    private UUID id;
    private UUID candidateId;
    private String displayName;
    private String originalFileName;
    private String fileExtension;
    private String mimeType;
    private long fileSize;
    private boolean defaultResume;
    private String status;
    private LocalDateTime uploadedAt;
}
