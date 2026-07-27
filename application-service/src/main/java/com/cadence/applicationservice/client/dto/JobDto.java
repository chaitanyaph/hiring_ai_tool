package com.cadence.applicationservice.client.dto;

import lombok.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Only the fields Application Service needs to validate an apply()
 * request and snapshot onto the Application row. status/applicationDeadline
 * are used purely for validation, never persisted verbatim.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private UUID id;
    private UUID companyId;
    private String title;
    private String status;
    private LocalDate applicationDeadline;
}
