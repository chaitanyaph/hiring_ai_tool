package com.cadence.codingassessmentservice.service;

import java.util.UUID;

/** The async evaluation pipeline, triggered once a candidate_assessment reaches COMPLETED: AI code review per submission, aggregate final score, write back to Application Service, publish CodingAssessmentCompleted. */
public interface CodingEvaluationService {

    void evaluate(UUID candidateAssessmentId);
}
