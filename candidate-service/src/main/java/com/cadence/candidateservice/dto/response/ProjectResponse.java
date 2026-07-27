package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectResponse {
    private UUID id;
    private String title;
    private String description;
    private String projectUrl;
}
