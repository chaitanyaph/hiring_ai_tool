package com.cadence.resumeparserservice.service;

import java.util.UUID;

/** Orchestration / write side: triggering, retrying, and cleaning up parsing work. */
public interface ResumeParsingService {

    void processResume(UUID resumeId, UUID candidateId, String checksum);

    void retryParsing(UUID resumeId);

    void handleCandidateDeleted(UUID candidateId);
}
