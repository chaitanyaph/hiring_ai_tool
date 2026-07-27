package com.cadence.aiinterviewservice.dto.response;

import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import lombok.*;

import java.util.List;

/** Candidate-facing: backs the completion screen's "View my transcript" / result view. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewResultResponse {
    private InterviewSessionStatus status;
    private String message;
    private List<TranscriptTurnResponse> transcript;
}
