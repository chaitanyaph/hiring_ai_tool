package com.cadence.applicationservice.service.impl;

import com.cadence.applicationservice.entity.Application;

import java.util.Objects;
import java.util.stream.Stream;

/**
 * overallScore is a real, derived average of whichever component
 * scores have been reported so far (resume match, AI interview,
 * coding) -- recomputed every time one of them changes, never set
 * directly by a caller (the /overall-score internal endpoint exists
 * for a future service that computes its own weighted overall score
 * server-side and just needs somewhere to put it, overriding this
 * default average).
 */
final class ScoreCalculator {
    private ScoreCalculator() {}

    static Integer recomputeOverall(Application app) {
        var scores = Stream.of(app.getResumeMatchScore(), app.getAiInterviewScore(), app.getCodingScore())
                .filter(Objects::nonNull)
                .toList();
        if (scores.isEmpty()) {
            return app.getOverallScore();
        }
        return (int) Math.round(scores.stream().mapToInt(Integer::intValue).average().orElse(0));
    }
}
