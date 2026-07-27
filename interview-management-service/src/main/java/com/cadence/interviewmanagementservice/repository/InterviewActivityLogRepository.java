package com.cadence.interviewmanagementservice.repository;

import com.cadence.interviewmanagementservice.entity.InterviewActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewActivityLogRepository extends JpaRepository<InterviewActivityLog, UUID> {
    List<InterviewActivityLog> findAllByInterviewIdOrderByOccurredAtDesc(UUID interviewId);
}
