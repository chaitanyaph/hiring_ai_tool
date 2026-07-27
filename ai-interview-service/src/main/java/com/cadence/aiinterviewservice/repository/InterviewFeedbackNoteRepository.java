package com.cadence.aiinterviewservice.repository;

import com.cadence.aiinterviewservice.constants.NoteType;
import com.cadence.aiinterviewservice.entity.InterviewFeedbackNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewFeedbackNoteRepository extends JpaRepository<InterviewFeedbackNote, UUID> {
    List<InterviewFeedbackNote> findAllBySessionIdAndNoteTypeOrderByDisplayOrderAsc(UUID sessionId, NoteType noteType);
    List<InterviewFeedbackNote> findAllBySessionIdOrderByDisplayOrderAsc(UUID sessionId);
    void deleteBySessionId(UUID sessionId);
}
