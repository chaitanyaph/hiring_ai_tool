package com.cadence.notificationservice.dto.response;

import com.cadence.notificationservice.constants.LogLevel;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationLogResponse {
    private LogLevel level;
    private String source;
    private String eventType;
    private String message;
    private LocalDateTime occurredAt;
}
