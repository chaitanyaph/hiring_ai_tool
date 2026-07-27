package com.cadence.resumeparserservice.dto.response;

import com.cadence.resumeparserservice.constants.LogLevel;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParserLogResponse {
    private LogLevel logLevel;
    private String message;
    private LocalDateTime createdAt;
}
