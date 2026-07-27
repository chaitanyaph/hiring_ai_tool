package com.cadence.applicationservice.dto.response;

import com.cadence.applicationservice.constant.ApplicationStage;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StageHistoryResponse {
    private ApplicationStage fromStage;
    private ApplicationStage toStage;
    private UUID changedBy;
    private LocalDateTime changedAt;
    private String reason;
}
