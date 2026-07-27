package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceResponse {
    private UUID id;
    private String jobTitle;
    private String companyName;
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean currentlyWorking;
    private String achievements;
}
