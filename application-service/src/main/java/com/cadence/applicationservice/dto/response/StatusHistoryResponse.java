package com.cadence.applicationservice.dto.response;

import com.cadence.applicationservice.constant.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusHistoryResponse {
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private UUID changedBy;
    private LocalDateTime changedAt;
    private String reason;
}
