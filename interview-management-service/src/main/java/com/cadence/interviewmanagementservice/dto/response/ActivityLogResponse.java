package com.cadence.interviewmanagementservice.dto.response;

import com.cadence.interviewmanagementservice.constants.ActivityEventType;
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
