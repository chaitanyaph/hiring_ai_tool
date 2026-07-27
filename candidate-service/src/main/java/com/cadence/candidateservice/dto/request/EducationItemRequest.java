package com.cadence.candidateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

/** id is null for a new entry, populated for an existing entry being edited in place. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationItemRequest {
    private UUID id;

    @NotBlank(message = "Degree is required")
    private String degree;

    @NotBlank(message = "Institution is required")
    private String institution;

    private Integer startYear;
    private Integer endYear;
    private String grade;
}
