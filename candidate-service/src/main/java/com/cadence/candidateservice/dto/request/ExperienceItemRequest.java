package com.cadence.candidateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ExperienceItemRequest {
    private UUID id;

    @NotBlank(message = "Job title is required")
    private String jobTitle;

    @NotBlank(message = "Company name is required")
    private String companyName;

    private LocalDate startDate;
    private LocalDate endDate;
    private boolean currentlyWorking;
    private String achievements;
}
