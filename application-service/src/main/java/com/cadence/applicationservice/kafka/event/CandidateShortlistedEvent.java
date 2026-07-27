package com.cadence.applicationservice.kafka.event;

import lombok.*;

import java.util.UUID;

/** Consumed -- published by the (future) AI Shortlisting Service. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateShortlistedEvent {
    private UUID applicationId;
}
