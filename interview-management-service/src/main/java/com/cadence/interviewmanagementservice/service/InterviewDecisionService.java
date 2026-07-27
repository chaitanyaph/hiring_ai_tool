package com.cadence.interviewmanagementservice.service;

import com.cadence.interviewmanagementservice.dto.request.RecruiterDecisionRequest;

import java.util.UUID;

public interface InterviewDecisionService {

    void recordDecision(UUID companyId, UUID interviewId, UUID actorId, RecruiterDecisionRequest request);
}
