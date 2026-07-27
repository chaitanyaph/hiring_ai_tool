package com.cadence.candidateservice.dto.response;

import com.cadence.candidateservice.constant.ApplicationStatus;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplicationStatusHistoryResponse {
    private ApplicationStatus fromStatus;
    private ApplicationStatus toStatus;
    private LocalDateTime changedAt;
    private String note;
}
