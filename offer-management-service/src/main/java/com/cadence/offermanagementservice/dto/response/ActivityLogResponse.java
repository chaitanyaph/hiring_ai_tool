package com.cadence.offermanagementservice.dto.response;

import com.cadence.offermanagementservice.constants.ActivityEventType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ActivityLogResponse {
    private ActivityEventType eventType;
    private UUID actorId;
    private LocalDateTime occurredAt;
    private String details;
}
