package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EducationResponse {
    private UUID id;
    private String degree;
    private String institution;
    private Integer startYear;
    private Integer endYear;
    private String grade;
}
