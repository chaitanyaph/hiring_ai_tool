package com.cadence.candidateservice.client.dto;

import lombok.*;

import java.util.UUID;

/**
 * Only the fields Candidate Service actually needs to validate an apply
 * request and snapshot onto the Application row -- deliberately not the
 * full JobDetailResponse shape, so this client doesn't silently break
 * every time Job Service adds an unrelated field. status is a String
 * (not Job Service's JobStatus enum) so the two services never share an
 * enum class across the wire.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class JobDto {
    private UUID id;
    private String title;
    private UUID companyId;
    private String location;
    private String employmentType;
    private String status;
}
