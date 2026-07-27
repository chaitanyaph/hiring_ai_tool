package com.cadence.resumeservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RenameResumeRequest {
    @NotBlank(message = "displayName is required")
    @Size(max = 150)
    private String displayName;
}
