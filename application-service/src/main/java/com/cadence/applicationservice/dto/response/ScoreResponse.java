package com.cadence.applicationservice.dto.response;

import com.cadence.applicationservice.constant.ScoreType;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ScoreResponse {
    private ScoreType scoreType;
    private Integer scoreValue;
    private String source;
    private LocalDateTime recordedAt;
}
