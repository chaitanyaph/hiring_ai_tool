package com.cadence.aiinterviewservice.repository;

import com.cadence.aiinterviewservice.entity.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, UUID> {
    Optional<InterviewAnswer> findByQuestionId(UUID questionId);
    List<InterviewAnswer> findAllBySessionIdOrderByAnsweredAtAsc(UUID sessionId);
}
