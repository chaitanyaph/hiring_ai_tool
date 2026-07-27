package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.MissingSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface MissingSkillRepository extends JpaRepository<MissingSkill, UUID> {
    List<MissingSkill> findAllByResumeMatchId(UUID resumeMatchId);
    void deleteAllByResumeMatchId(UUID resumeMatchId);
}
