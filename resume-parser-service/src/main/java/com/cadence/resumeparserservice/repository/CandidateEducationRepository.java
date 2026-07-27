package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.CandidateEducation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateEducationRepository extends JpaRepository<CandidateEducation, UUID> {
    List<CandidateEducation> findAllByParsedResumeIdOrderByDisplayOrderAsc(UUID parsedResumeId);
    void deleteAllByParsedResumeId(UUID parsedResumeId);
}
