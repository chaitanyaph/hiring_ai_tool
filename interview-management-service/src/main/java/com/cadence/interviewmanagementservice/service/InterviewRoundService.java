package com.cadence.interviewmanagementservice.service;

import com.cadence.interviewmanagementservice.dto.request.CreateInterviewRoundRequest;
import com.cadence.interviewmanagementservice.dto.request.UpdateInterviewRoundRequest;
import com.cadence.interviewmanagementservice.dto.response.InterviewRoundResponse;

import java.util.List;
import java.util.UUID;

public interface InterviewRoundService {

    InterviewRoundResponse createRound(UUID companyId, CreateInterviewRoundRequest request);

    InterviewRoundResponse updateRound(UUID companyId, UUID roundId, UpdateInterviewRoundRequest request);

    void deleteRound(UUID companyId, UUID roundId);

    List<InterviewRoundResponse> listRounds(UUID companyId, boolean activeOnly);
}
