package com.cadence.applicationservice.repository;

import com.cadence.applicationservice.entity.ApplicationNote;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationNoteRepository extends JpaRepository<ApplicationNote, UUID> {
    List<ApplicationNote> findAllByApplicationIdOrderByCreatedAtDesc(UUID applicationId);
}
