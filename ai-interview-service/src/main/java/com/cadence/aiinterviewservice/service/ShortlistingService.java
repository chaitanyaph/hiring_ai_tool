package com.cadence.aiinterviewservice.service;

import com.cadence.aiinterviewservice.kafka.event.ResumeAnalyzedEvent;

import java.util.List;
import java.util.UUID;

/** Orchestration / write side of AI Shortlisting: the ResumeAnalyzed trigger and manual bulk/single decisions on the manual-review queue. */
public interface ShortlistingService {

    void handleResumeAnalyzed(ResumeAnalyzedEvent event);

    void shortlistOne(UUID applicationId);

    void rejectOne(UUID applicationId);

    void bulkShortlist(List<UUID> applicationIds);

    void bulkReject(List<UUID> applicationIds);

    void assignRecruiter(List<UUID> applicationIds, UUID recruiterId);
}
