package com.cadence.jobservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SaveTemplateRequest {

    @NotBlank(message = "Template name is required")
    @Size(max = 150)
    private String templateName;
}
