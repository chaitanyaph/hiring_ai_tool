package com.cadence.notificationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTemplateRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String subject;

    @NotBlank
    private String bodyHtml;

    private String variablesHint;

    private boolean active;
}
