package com.cadence.authservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionResponse {
    private UUID id;
    private String deviceInfo;
    private String ipAddress;
    private String location;
    private boolean active;
    private boolean current;
    private LocalDateTime lastActiveAt;
    private LocalDateTime createdAt;
}
