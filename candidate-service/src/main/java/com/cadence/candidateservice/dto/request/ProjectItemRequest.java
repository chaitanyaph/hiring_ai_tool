package com.cadence.candidateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectItemRequest {
    private UUID id;

    @NotBlank(message = "Project title is required")
    private String title;

    private String description;
    private String projectUrl;
}
