package com.cadence.applicationservice.client.dto;

import lombok.*;

import java.util.UUID;

/** Only what apply() needs to validate a resumeId belongs to the applying candidate and is usable. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ResumeDto {
    private UUID id;
    private UUID candidateId;
    private String status;
}
