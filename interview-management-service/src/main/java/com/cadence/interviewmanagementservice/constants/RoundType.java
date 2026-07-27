package com.cadence.interviewmanagementservice.constants;

/**
 * The Figma's schedule-interview modal only offers Technical/HR (plus
 * AI, which belongs to ai-interview-service, not this service) -- see
 * README "Architecture Decisions". MANAGER/ARCHITECT/CUSTOM are
 * supported per the text specification's Module 1 (configurable round
 * types) even though no Figma screen currently exercises them; a
 * per-company interview_round list is what would make them selectable
 * once the frontend's static 3-option dropdown is made dynamic.
 */
public enum RoundType {
    TECHNICAL,
    MANAGER,
    ARCHITECT,
    HR,
    CUSTOM
}
