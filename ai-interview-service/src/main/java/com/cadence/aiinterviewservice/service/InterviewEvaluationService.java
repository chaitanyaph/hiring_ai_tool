package com.cadence.aiinterviewservice.service;

import java.util.UUID;

/** The async evaluation pipeline, triggered once a session reaches COMPLETED. */
public interface InterviewEvaluationService {

    void evaluate(UUID sessionId);
}
