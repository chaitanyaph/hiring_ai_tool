package com.cadence.aiinterviewservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

/** Backs the Manual Review queue's bulk-action bar (Shortlist selected / Reject selected). */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BulkApplicationIdsRequest {
    @NotEmpty
    private List<UUID> applicationIds;
}
