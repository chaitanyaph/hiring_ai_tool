package com.cadence.applicationservice.constant;

/** Discriminates which interview an InterviewCompletedEvent refers to -- the platform has one generic "interview completed" topic, not one per round. */
public enum InterviewType {
    AI,
    TECHNICAL,
    MANAGER,
    HR
}
