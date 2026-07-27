package com.cadence.applicationservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignHiringManagerRequest {
    @NotNull(message = "hiringManagerId is required")
    private UUID hiringManagerId;
}
