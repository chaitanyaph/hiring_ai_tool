package com.cadence.codingassessmentservice.service;

import com.cadence.codingassessmentservice.dto.request.CreateAssessmentRequest;
import com.cadence.codingassessmentservice.dto.request.UpdateAssessmentRequest;
import com.cadence.codingassessmentservice.dto.response.AssessmentResponse;

import java.util.List;
import java.util.UUID;

/** Orchestration / write side of assessment management: the 4-step wizard's submit, publish (which invites every eligible candidate for the job), clone/archive/close, question assignment, and reminders. */
public interface AssessmentService {

    AssessmentResponse createAssessment(UUID companyId, UUID recruiterId, CreateAssessmentRequest request);

    AssessmentResponse updateAssessment(UUID companyId, UUID assessmentId, UpdateAssessmentRequest request);

    void deleteAssessment(UUID companyId, UUID assessmentId);

    void publishAssessment(UUID companyId, UUID assessmentId);

    AssessmentResponse cloneAssessment(UUID companyId, UUID assessmentId);

    void archiveAssessment(UUID companyId, UUID assessmentId);

    void closeAssessment(UUID companyId, UUID assessmentId);

    void assignQuestions(UUID companyId, UUID assessmentId, List<UUID> questionIds);

    void sendReminders(UUID companyId, UUID assessmentId);

    void remindCandidate(UUID companyId, UUID assessmentId, UUID applicationId);

    /** The submission drawer's "Move to next stage" -- re-publishes CodingAssessmentCompleted so Application Service's own pipeline can (re)act on it; this service never writes Application Service's state machine directly. */
    void moveToNextStage(UUID companyId, UUID assessmentId, UUID applicationId);
}
