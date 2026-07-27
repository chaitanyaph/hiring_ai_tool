package com.cadence.jobservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/** Consumed by Notification Service (announce the opening) and Analytics. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobPublishedEvent {
    private UUID jobId;
    private UUID companyId;
    private UUID departmentId;
    private String title;
    private String jobCode;
    private LocalDateTime occurredAt;
}
