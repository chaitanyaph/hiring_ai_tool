package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/**
 * Consumed -- published by the (future) Background Verification
 * Service. A pass does NOT itself release an offer (that's a separate
 * OfferReleased event from the Offer Service) -- it just clears the
 * candidate to remain at BACKGROUND_VERIFICATION awaiting that step. A
 * fail moves the application straight to REJECTED.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BackgroundVerificationCompletedEvent {
    private UUID applicationId;
    private boolean passed;
    private String remarks;
}
