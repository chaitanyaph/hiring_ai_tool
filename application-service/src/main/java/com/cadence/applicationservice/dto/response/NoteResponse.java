package com.cadence.applicationservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NoteResponse {
    private UUID id;
    private UUID authorId;
    private String note;
    private LocalDateTime createdAt;
}
