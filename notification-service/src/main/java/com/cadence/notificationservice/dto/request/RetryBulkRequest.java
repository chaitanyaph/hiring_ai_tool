package com.cadence.notificationservice.dto.request;

import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.util.List;
import java.util.UUID;

/** Matches the Figma's Failed tab bulk-select "Retry selected" bar. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RetryBulkRequest {

    @NotEmpty
    private List<UUID> emailQueueIds;
}
