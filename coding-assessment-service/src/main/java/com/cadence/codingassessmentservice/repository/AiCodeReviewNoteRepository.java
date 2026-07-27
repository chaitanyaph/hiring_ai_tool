package com.cadence.codingassessmentservice.repository;

import com.cadence.codingassessmentservice.constants.NoteType;
import com.cadence.codingassessmentservice.entity.AiCodeReviewNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AiCodeReviewNoteRepository extends JpaRepository<AiCodeReviewNote, UUID> {
    List<AiCodeReviewNote> findAllByAiCodeReviewIdAndNoteTypeOrderByDisplayOrderAsc(UUID aiCodeReviewId, NoteType noteType);
    void deleteAllByAiCodeReviewId(UUID aiCodeReviewId);
}
