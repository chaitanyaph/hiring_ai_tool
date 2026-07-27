package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJobResponse {
    private UUID id;
    private UUID jobId;
    private LocalDateTime savedAt;
}
