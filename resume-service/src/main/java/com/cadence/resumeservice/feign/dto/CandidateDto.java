package com.cadence.resumeservice.feign.dto;

import lombok.*;

import java.util.UUID;

/** Only the fields Resume Service needs to validate an upload -- candidate existence and account status. */
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
