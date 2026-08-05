package com.cadence.codingassessmentservice.repository;

import java.util.UUID;

/** Projection for GROUP BY questionId, COUNT(*) queries -- avoids N+1 per-question count queries on list endpoints. */
public interface QuestionIdCount {
    UUID getQuestionId();
    long getCount();
}
