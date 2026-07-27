package com.cadence.jobservice.dto.request;

import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusChangeRequest {

    @Size(max = 255)
    private String reason;
}
