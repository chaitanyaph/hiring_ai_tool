package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.SkillMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface SkillMatchRepository extends JpaRepository<SkillMatch, UUID> {
    List<SkillMatch> findAllByResumeMatchId(UUID resumeMatchId);
    void deleteAllByResumeMatchId(UUID resumeMatchId);
}
