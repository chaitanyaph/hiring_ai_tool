package com.cadence.interviewmanagementservice.repository;

import com.cadence.interviewmanagementservice.entity.InterviewPanelist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewPanelistRepository extends JpaRepository<InterviewPanelist, UUID> {
    List<InterviewPanelist> findAllByInterviewId(UUID interviewId);
    boolean existsByInterviewIdAndInterviewerId(UUID interviewId, UUID interviewerId);
    void deleteAllByInterviewId(UUID interviewId);
}
