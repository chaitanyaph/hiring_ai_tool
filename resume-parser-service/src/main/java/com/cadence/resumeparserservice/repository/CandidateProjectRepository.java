package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.CandidateProject;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateProjectRepository extends JpaRepository<CandidateProject, UUID> {
    List<CandidateProject> findAllByParsedResumeIdOrderByDisplayOrderAsc(UUID parsedResumeId);
    void deleteAllByParsedResumeId(UUID parsedResumeId);
}
