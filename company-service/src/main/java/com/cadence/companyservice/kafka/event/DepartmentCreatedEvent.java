package com.cadence.companyservice.kafka.event;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DepartmentCreatedEvent {
    private UUID departmentId;
    private UUID companyId;
    private String departmentName;
    private LocalDateTime occurredAt;
}
