package com.cadence.offermanagementservice.constants;

/**
 * Matches the Figma's 5 visible tabs (PENDING_APPROVAL/SENT/ACCEPTED/
 * DECLINED, plus "All") exactly, with DRAFT and WITHDRAWN added since
 * both are real actions in the Figma (wizard's "Save as draft", the
 * detail drawer's "Withdraw" button) even though neither has its own
 * dedicated tab. EXPIRED is added for the expiry-date sweep (§ Offer
 * Expiry functional requirement) -- also not a dedicated tab, same
 * reasoning as WITHDRAWN.
 */
public enum OfferStatus {
    DRAFT,
    PENDING_APPROVAL,
    SENT,
    ACCEPTED,
    DECLINED,
    WITHDRAWN,
    EXPIRED
}
