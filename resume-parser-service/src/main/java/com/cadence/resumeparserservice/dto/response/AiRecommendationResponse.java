package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.HiringRecommendation;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRecommendationResponse {
    private HiringRecommendation hiringRecommendation;
    private String overallAiSummary;
    private String recommendedLearningTopics;
}
