package com.cadence.interviewmanagementservice.dto.response;

import com.cadence.interviewmanagementservice.constants.RoundType;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewRoundResponse {
    private UUID id;
    private UUID companyId;
    private String name;
    private RoundType type;
    private int roundOrder;
    private String description;
    private boolean active;
}
