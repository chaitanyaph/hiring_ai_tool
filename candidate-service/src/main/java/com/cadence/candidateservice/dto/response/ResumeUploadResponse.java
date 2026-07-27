package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResumeUploadResponse {
    private String resumeUrl;
    private String resumeFilename;
    private LocalDateTime uploadedAt;
    private Integer profileCompletionPercent;
}
