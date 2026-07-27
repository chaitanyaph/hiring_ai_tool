package com.cadence.jobservice.dto.request;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignJobRequest {
    private UUID recruiterId;
    private UUID hiringManagerId;
}
