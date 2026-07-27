package com.cadence.applicationservice.constant;

/**
 * The 10 stages shown on the recruiter pipeline board and the
 * candidate's tracker. currentStage is derived automatically from
 * currentStatus (see ApplicationStatus.getDefaultStage()) -- it is
 * never set independently by a caller, so the two can never drift.
 */
public enum ApplicationStage {
    APPLICATION,
    AI_RESUME_SCREENING,
    AI_INTERVIEW,
    CODING_ASSESSMENT,
    TECHNICAL_INTERVIEW,
    MANAGER_INTERVIEW,
    HR_INTERVIEW,
    BACKGROUND_VERIFICATION,
    OFFER,
    HIRED
}
