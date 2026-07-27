package com.cadence.aiinterviewservice.repository;

import com.cadence.aiinterviewservice.entity.InterviewLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewLogRepository extends JpaRepository<InterviewLog, UUID> {
    List<InterviewLog> findAllBySessionIdOrderByCreatedAtAsc(UUID sessionId);
}
