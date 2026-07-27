package com.cadence.applicationservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddNoteRequest {
    @NotBlank(message = "note is required")
    @Size(max = 2000)
    private String note;
}
