package com.cadence.offermanagementservice.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApproveOfferRequest {

    @NotNull
    private boolean approve;

    private String notes;
}
