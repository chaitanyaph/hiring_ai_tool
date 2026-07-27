package com.cadence.resumeparserservice.service;

import com.cadence.resumeparserservice.kafka.event.ApplicationCreatedEvent;

import java.util.UUID;

/** Orchestration / write side of the matching extension: the ApplicationCreated trigger, manual recalculation, and the parsing-completion hook. */
public interface ResumeMatchAnalysisService {

    void handleApplicationCreated(ApplicationCreatedEvent event);

    void recalculate(UUID applicationId);

    /** Called by ResumeParsingPipelineRunner once a resume finishes parsing, to resume any AWAITING_PARSE rows waiting on it. */
    void onResumeParsed(UUID resumeId);
}
