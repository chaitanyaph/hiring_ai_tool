package com.cadence.analyticsservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class JobPublishedEvent {
    private UUID jobId;
    private UUID companyId;
    private UUID departmentId;
    private String title;
    private String jobCode;
    private LocalDateTime occurredAt;
}
