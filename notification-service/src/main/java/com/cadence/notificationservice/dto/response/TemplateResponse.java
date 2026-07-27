package com.cadence.notificationservice.dto.response;

import com.cadence.notificationservice.constants.TemplateCategory;
import com.cadence.notificationservice.constants.TriggerEvent;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TemplateResponse {
    private UUID id;
    private String name;
    private TriggerEvent triggerEvent;
    private TemplateCategory category;
    private String subject;
    private String bodyHtml;
    private String variablesHint;
    private boolean active;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
