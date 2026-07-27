package com.cadence.applicationservice.dto.request;

import com.cadence.applicationservice.constant.ApplicationStatus;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatusChangeRequest {
    @NotNull(message = "toStatus is required")
    private ApplicationStatus toStatus;

    private String reason;
}
