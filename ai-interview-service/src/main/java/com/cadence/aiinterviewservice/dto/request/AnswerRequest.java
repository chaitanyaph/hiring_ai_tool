package com.cadence.aiinterviewservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AnswerRequest {
    @NotNull
    private UUID applicationId;

    @NotNull
    private UUID questionId;

    @NotBlank
    private String answerText;

    private Integer responseTimeSeconds;
}
