package com.cadence.interviewmanagementservice.repository;

import com.cadence.interviewmanagementservice.constants.TimelineStage;
import com.cadence.interviewmanagementservice.entity.CandidateTimeline;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateTimelineRepository extends JpaRepository<CandidateTimeline, UUID> {
    List<CandidateTimeline> findAllByApplicationId(UUID applicationId);
    List<CandidateTimeline> findAllByCandidateId(UUID candidateId);
    Optional<CandidateTimeline> findByApplicationIdAndStage(UUID applicationId, TimelineStage stage);
}
