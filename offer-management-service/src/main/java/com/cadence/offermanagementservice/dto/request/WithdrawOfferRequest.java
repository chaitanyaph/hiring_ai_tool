package com.cadence.offermanagementservice.dto.request;

import lombok.*;

/** Matches the Figma's Withdraw action exactly -- no modal, no reason field in the mockup. Reason kept optional for API completeness. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WithdrawOfferRequest {
    private String reason;
}
