package com.cadence.resumeservice.dto.response;

import com.cadence.resumeservice.constants.ResumeStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Candidate/recruiter-facing -- deliberately excludes bucketName/objectName/checksum, which are storage internals no API consumer needs. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeResponse {
    private UUID id;
    private UUID candidateId;
    private String displayName;
    private String originalFileName;
    private String fileExtension;
    private String mimeType;
    private long fileSize;
    private boolean defaultResume;
    private ResumeStatus status;
    private LocalDateTime uploadedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
