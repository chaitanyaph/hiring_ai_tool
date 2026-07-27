package com.cadence.applicationservice.service;

import com.cadence.applicationservice.dto.request.ScoreUpdateRequest;
import com.cadence.applicationservice.dto.response.ApplicationResponse;

import java.util.UUID;

/**
 * Backs the 4 /internal/application/{id}/*-score REST endpoints --
 * a synchronous alternative to the Kafka event path for services that
 * prefer a direct call. Unlike ApplicationLifecycleEventService, these
 * only record a score value; they never drive a status/stage
 * transition, since a bare score alone doesn't tell us the upstream
 * step actually finished (a Kafka *Completed event carries that intent
 * explicitly).
 */
public interface InternalScoreService {
    ApplicationResponse updateResumeScore(UUID applicationId, ScoreUpdateRequest request);
    ApplicationResponse updateInterviewScore(UUID applicationId, ScoreUpdateRequest request);
    ApplicationResponse updateCodingScore(UUID applicationId, ScoreUpdateRequest request);
    ApplicationResponse updateOverallScore(UUID applicationId, ScoreUpdateRequest request);
}
