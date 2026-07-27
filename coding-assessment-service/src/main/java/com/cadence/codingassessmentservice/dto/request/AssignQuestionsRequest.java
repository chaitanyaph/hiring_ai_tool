package com.cadence.codingassessmentservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignQuestionsRequest {
    @NotEmpty
    private List<UUID> questionIds;
}
