package com.cadence.aiinterviewservice.repository;

import com.cadence.aiinterviewservice.entity.InterviewQuestion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewQuestionRepository extends JpaRepository<InterviewQuestion, UUID> {
    List<InterviewQuestion> findAllBySessionIdOrderByQuestionOrderAsc(UUID sessionId);
    long countBySessionId(UUID sessionId);
}
