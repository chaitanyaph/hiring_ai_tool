package com.cadence.jobservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobTemplateResponse {
    private UUID id;
    private String templateName;
    private LocalDateTime createdAt;
}
