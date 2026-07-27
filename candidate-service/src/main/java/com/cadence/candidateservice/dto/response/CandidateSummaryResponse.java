package com.cadence.candidateservice.dto.response;

import lombok.*;

import java.util.UUID;

/**
 * Minimal projection of a candidate's profile for service-to-service
 * lookups: (a) Application Service validating "profile completed" /
 * "resume exists" before allowing an apply(), (b) denormalizing a
 * name/email snapshot onto an application row for recruiter-side
 * search, and (c) Resume Service validating "candidate exists and is
 * ACTIVE" before accepting an upload. Deliberately excludes phone/
 * location/resume URL -- only fields another service has a genuine,
 * documented call site for.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateSummaryResponse {
    private UUID id;
    private String fullName;
    private String email;
    private boolean resumeUploaded;
    private Integer profileCompletionPercent;
    private String status;
}
