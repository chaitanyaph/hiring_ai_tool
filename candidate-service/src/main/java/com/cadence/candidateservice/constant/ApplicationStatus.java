package com.cadence.candidateservice.constant;

import java.util.EnumSet;
import java.util.Set;

/**
 * The 9-stage pipeline shown on the candidate's Application Tracker.
 * Forward progression (APPLIED -> ... -> OFFER) is driven by recruiters
 * and future AI services (resume screening, AI interview, coding
 * assessment) via the stage-change endpoint -- Candidate Service does
 * not compute scores or run any of those steps itself, it only records
 * the pipeline state. REJECTED and WITHDRAWN are terminal.
 */
public enum ApplicationStatus {
    APPLIED,
    RESUME_SCREENING,
    AI_RESUME_MATCH,
    AI_INTERVIEW,
    CODING_ASSESSMENT,
    TECHNICAL_INTERVIEW,
    HR_INTERVIEW,
    OFFER,
    REJECTED,
    WITHDRAWN;

    private static final Set<ApplicationStatus> TERMINAL = EnumSet.of(REJECTED, WITHDRAWN);

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** Whether a candidate may withdraw while the application is in this stage. */
    public boolean isWithdrawable() {
        return !TERMINAL.contains(this);
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        if (TERMINAL.contains(this)) {
            return false;
        }
        if (target == WITHDRAWN) {
            return isWithdrawable();
        }
        if (target == REJECTED) {
            return this != OFFER;
        }
        return switch (this) {
            case APPLIED -> target == RESUME_SCREENING;
            case RESUME_SCREENING -> target == AI_RESUME_MATCH;
            case AI_RESUME_MATCH -> target == AI_INTERVIEW;
            case AI_INTERVIEW -> target == CODING_ASSESSMENT;
            case CODING_ASSESSMENT -> target == TECHNICAL_INTERVIEW;
            case TECHNICAL_INTERVIEW -> target == HR_INTERVIEW;
            case HR_INTERVIEW -> target == OFFER;
            default -> false;
        };
    }
}
