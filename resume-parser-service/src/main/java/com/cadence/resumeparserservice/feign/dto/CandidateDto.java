package com.cadence.resumeparserservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Only the fields this service needs to validate a candidate before parsing. */
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
    private String status;
}
