package com.cadence.aiinterviewservice.dto.response;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TranscriptTurnResponse {
    private String speaker; // AI | CANDIDATE
    private String text;
}
