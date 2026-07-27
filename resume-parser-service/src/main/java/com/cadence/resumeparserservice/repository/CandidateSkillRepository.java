package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.CandidateSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, UUID> {
    List<CandidateSkill> findAllByParsedResumeId(UUID parsedResumeId);
    void deleteAllByParsedResumeId(UUID parsedResumeId);
}
