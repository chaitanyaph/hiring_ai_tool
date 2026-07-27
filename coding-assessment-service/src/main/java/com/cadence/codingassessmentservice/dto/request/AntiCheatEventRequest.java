package com.cadence.codingassessmentservice.dto.request;

import com.cadence.codingassessmentservice.constants.AntiCheatEventType;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AntiCheatEventRequest {
    @NotNull
    private AntiCheatEventType eventType;

    private String metadata;
}
