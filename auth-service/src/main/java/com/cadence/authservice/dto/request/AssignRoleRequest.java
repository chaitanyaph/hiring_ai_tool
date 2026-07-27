package com.cadence.authservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.Set;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignRoleRequest {

    @NotEmpty(message = "At least one role name must be provided")
    private Set<String> roleNames;

    private UUID userId;
}
