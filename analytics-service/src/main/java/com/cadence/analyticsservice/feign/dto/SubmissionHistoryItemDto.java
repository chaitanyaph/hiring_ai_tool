package com.cadence.analyticsservice.feign.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class SubmissionHistoryItemDto {
    private String status;
    private Integer score;
}
