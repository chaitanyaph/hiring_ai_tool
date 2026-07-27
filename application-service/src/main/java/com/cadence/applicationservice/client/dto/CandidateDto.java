package com.cadence.applicationservice.client.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CandidateDto {
    private UUID id;
    private String fullName;
    private String email;
    private boolean resumeUploaded;
    private Integer profileCompletionPercent;
}
