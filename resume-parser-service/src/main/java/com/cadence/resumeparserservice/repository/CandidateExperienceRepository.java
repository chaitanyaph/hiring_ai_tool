package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.CandidateExperience;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateExperienceRepository extends JpaRepository<CandidateExperience, UUID> {
    List<CandidateExperience> findAllByParsedResumeIdOrderByDisplayOrderAsc(UUID parsedResumeId);
    void deleteAllByParsedResumeId(UUID parsedResumeId);
}
