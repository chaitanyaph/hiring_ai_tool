package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.constants.NoteType;
import com.cadence.resumeparserservice.entity.ResumeMatchNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ResumeMatchNoteRepository extends JpaRepository<ResumeMatchNote, UUID> {
    List<ResumeMatchNote> findAllByResumeMatchIdAndNoteTypeOrderByDisplayOrderAsc(UUID resumeMatchId, NoteType noteType);
    List<ResumeMatchNote> findAllByResumeMatchIdOrderByDisplayOrderAsc(UUID resumeMatchId);
    void deleteAllByResumeMatchId(UUID resumeMatchId);
}
