package com.cadence.applicationservice.constant;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * The full 20-state application lifecycle. Forward progression is
 * driven partly by recruiters (moving a candidate to the next stage)
 * and partly by Kafka events from other services (Resume Parser,
 * Resume Matching, AI Shortlisting, AI Interview, Coding Assessment,
 * Background Verification, Offer Service) -- this enum only enforces
 * that a transition is *legal*, it doesn't decide who triggers it.
 * REJECTED, WITHDRAWN, OFFER_DECLINED and HIRED are terminal.
 */
public enum ApplicationStatus {
    APPLIED,
    RESUME_PARSING,
    RESUME_PARSED,
    AI_MATCHING,
    AI_MATCHED,
    MANUAL_REVIEW,
    SHORTLISTED,
    AI_INTERVIEW_PENDING,
    AI_INTERVIEW_COMPLETED,
    CODING_ASSESSMENT_PENDING,
    CODING_ASSESSMENT_COMPLETED,
    TECHNICAL_INTERVIEW,
    MANAGER_INTERVIEW,
    HR_INTERVIEW,
    BACKGROUND_VERIFICATION,
    OFFER_RELEASED,
    OFFER_ACCEPTED,
    OFFER_DECLINED,
    HIRED,
    REJECTED,
    WITHDRAWN;

    private static final Set<ApplicationStatus> TERMINAL = EnumSet.of(HIRED, REJECTED, WITHDRAWN, OFFER_DECLINED);

    private static final Map<ApplicationStatus, ApplicationStatus> LINEAR_NEXT = Map.ofEntries(
            Map.entry(APPLIED, RESUME_PARSING),
            Map.entry(RESUME_PARSING, RESUME_PARSED),
            Map.entry(RESUME_PARSED, AI_MATCHING),
            Map.entry(AI_MATCHING, AI_MATCHED),
            Map.entry(AI_MATCHED, SHORTLISTED),
            Map.entry(SHORTLISTED, AI_INTERVIEW_PENDING),
            Map.entry(AI_INTERVIEW_PENDING, AI_INTERVIEW_COMPLETED),
            Map.entry(AI_INTERVIEW_COMPLETED, CODING_ASSESSMENT_PENDING),
            Map.entry(CODING_ASSESSMENT_PENDING, CODING_ASSESSMENT_COMPLETED),
            Map.entry(CODING_ASSESSMENT_COMPLETED, TECHNICAL_INTERVIEW),
            Map.entry(TECHNICAL_INTERVIEW, MANAGER_INTERVIEW),
            Map.entry(MANAGER_INTERVIEW, HR_INTERVIEW),
            Map.entry(HR_INTERVIEW, BACKGROUND_VERIFICATION),
            Map.entry(BACKGROUND_VERIFICATION, OFFER_RELEASED),
            Map.entry(OFFER_ACCEPTED, HIRED)
    );

    private static final Map<ApplicationStatus, ApplicationStage> STAGE_MAP = Map.ofEntries(
            Map.entry(APPLIED, ApplicationStage.APPLICATION),
            Map.entry(RESUME_PARSING, ApplicationStage.AI_RESUME_SCREENING),
            Map.entry(RESUME_PARSED, ApplicationStage.AI_RESUME_SCREENING),
            Map.entry(AI_MATCHING, ApplicationStage.AI_RESUME_SCREENING),
            Map.entry(AI_MATCHED, ApplicationStage.AI_RESUME_SCREENING),
            Map.entry(MANUAL_REVIEW, ApplicationStage.AI_RESUME_SCREENING),
            Map.entry(SHORTLISTED, ApplicationStage.AI_RESUME_SCREENING),
            Map.entry(AI_INTERVIEW_PENDING, ApplicationStage.AI_INTERVIEW),
            Map.entry(AI_INTERVIEW_COMPLETED, ApplicationStage.AI_INTERVIEW),
            Map.entry(CODING_ASSESSMENT_PENDING, ApplicationStage.CODING_ASSESSMENT),
            Map.entry(CODING_ASSESSMENT_COMPLETED, ApplicationStage.CODING_ASSESSMENT),
            Map.entry(TECHNICAL_INTERVIEW, ApplicationStage.TECHNICAL_INTERVIEW),
            Map.entry(MANAGER_INTERVIEW, ApplicationStage.MANAGER_INTERVIEW),
            Map.entry(HR_INTERVIEW, ApplicationStage.HR_INTERVIEW),
            Map.entry(BACKGROUND_VERIFICATION, ApplicationStage.BACKGROUND_VERIFICATION),
            Map.entry(OFFER_RELEASED, ApplicationStage.OFFER),
            Map.entry(OFFER_ACCEPTED, ApplicationStage.OFFER),
            Map.entry(OFFER_DECLINED, ApplicationStage.OFFER),
            Map.entry(HIRED, ApplicationStage.HIRED)
    );

    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }

    /** REJECTED/WITHDRAWN keep whatever stage the application was already in -- returns null for those. */
    public ApplicationStage getDefaultStage() {
        return STAGE_MAP.get(this);
    }

    public boolean isWithdrawable() {
        return this != HIRED && this != REJECTED && this != WITHDRAWN && this != OFFER_DECLINED;
    }

    public boolean isRejectable() {
        return !TERMINAL.contains(this);
    }

    public boolean canTransitionTo(ApplicationStatus target) {
        if (TERMINAL.contains(this)) {
            return false;
        }
        if (target == REJECTED) {
            return isRejectable();
        }
        if (target == WITHDRAWN) {
            return isWithdrawable();
        }
        if (this == OFFER_RELEASED && (target == OFFER_ACCEPTED || target == OFFER_DECLINED)) {
            return true;
        }
        if (this == AI_MATCHED && target == MANUAL_REVIEW) {
            return true;
        }
        if (this == MANUAL_REVIEW && target == SHORTLISTED) {
            return true;
        }
        return LINEAR_NEXT.get(this) == target;
    }
}
