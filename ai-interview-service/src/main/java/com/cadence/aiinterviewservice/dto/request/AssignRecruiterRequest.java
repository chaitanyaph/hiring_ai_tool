package com.cadence.aiinterviewservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRecruiterRequest {
    @NotEmpty
    private List<UUID> applicationIds;

    @NotNull
    private UUID recruiterId;
}
