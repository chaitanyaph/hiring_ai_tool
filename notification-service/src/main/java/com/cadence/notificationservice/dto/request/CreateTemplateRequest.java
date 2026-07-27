package com.cadence.notificationservice.dto.request;

import com.cadence.notificationservice.constants.TemplateCategory;
import com.cadence.notificationservice.constants.TriggerEvent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTemplateRequest {

    @NotBlank
    private String name;

    @NotNull
    private TriggerEvent triggerEvent;

    @NotNull
    private TemplateCategory category;

    @NotBlank
    private String subject;

    @NotBlank
    private String bodyHtml;

    private String variablesHint;
}
