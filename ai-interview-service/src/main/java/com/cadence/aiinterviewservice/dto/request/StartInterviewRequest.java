package com.cadence.aiinterviewservice.dto.request;

import com.cadence.aiinterviewservice.constants.InterviewMode;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StartInterviewRequest {
    @NotNull
    private UUID applicationId;

    @NotNull
    private InterviewMode mode;
}
