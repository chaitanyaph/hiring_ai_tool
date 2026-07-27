package com.cadence.interviewmanagementservice.constants;

/**
 * Mirrors application-service's own InterviewType enum exactly (AI,
 * TECHNICAL, MANAGER, HR -- no ARCHITECT/CUSTOM). Used only for the
 * bridge event published onto application-service's existing
 * interview.interview.completed topic; this service's own RoundType
 * has 2 extra values that don't exist on the other side, so
 * ARCHITECT/CUSTOM are mapped down to TECHNICAL when bridging (see
 * InterviewFeedbackServiceImpl) -- flagged in the README, not silently
 * assumed correct.
 */
public enum ApplicationInterviewType {
    AI,
    TECHNICAL,
    MANAGER,
    HR
}
