package com.cadence.resumeservice.repository;

import com.cadence.resumeservice.constants.ResumeStatus;
import com.cadence.resumeservice.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    List<Resume> findAllByCandidateIdAndStatusOrderByUploadedAtDesc(UUID candidateId, ResumeStatus status);

    Optional<Resume> findByIdAndCandidateId(UUID id, UUID candidateId);

    long countByCandidateIdAndStatus(UUID candidateId, ResumeStatus status);

    boolean existsByCandidateIdAndChecksumAndStatus(UUID candidateId, String checksum, ResumeStatus status);

    Optional<Resume> findByCandidateIdAndDefaultResumeTrueAndStatus(UUID candidateId, ResumeStatus status);

    /** Used by the CandidateDeleted consumer to sweep every resume a deleted candidate ever uploaded, regardless of status. */
    List<Resume> findAllByCandidateId(UUID candidateId);
}
