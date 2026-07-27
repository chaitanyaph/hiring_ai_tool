package com.cadence.interviewmanagementservice.repository;

import com.cadence.interviewmanagementservice.entity.InterviewFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewFeedbackRepository extends JpaRepository<InterviewFeedback, UUID> {
    List<InterviewFeedback> findAllByInterviewId(UUID interviewId);
    Optional<InterviewFeedback> findByInterviewIdAndInterviewerId(UUID interviewId, UUID interviewerId);
}
