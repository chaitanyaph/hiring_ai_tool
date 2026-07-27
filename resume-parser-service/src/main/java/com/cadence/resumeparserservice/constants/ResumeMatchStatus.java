package com.cadence.resumeparserservice.constants;

/**
 * AWAITING_RESUME and AWAITING_PARSE exist because matching needs a
 * job (only known once ApplicationCreatedEvent arrives) AND a parsed
 * resume (only known once this service's own parsing pipeline
 * finishes) -- either can be missing when the trigger event shows up,
 * so the pipeline has to wait rather than fail.
 */
public enum ResumeMatchStatus {
    AWAITING_RESUME,
    AWAITING_PARSE,
    ANALYZING,
    ANALYZED,
    FAILED
}
