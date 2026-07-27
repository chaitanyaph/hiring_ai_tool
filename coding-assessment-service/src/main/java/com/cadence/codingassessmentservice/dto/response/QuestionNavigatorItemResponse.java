package com.cadence.codingassessmentservice.dto.response;

import java.util.UUID;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class QuestionNavigatorItemResponse {
    private UUID questionId;
    private int questionOrder;
    private String status; // NOT_VISITED | VISITED | COMPLETED
    private boolean markedForReview;
    private boolean current;
}
