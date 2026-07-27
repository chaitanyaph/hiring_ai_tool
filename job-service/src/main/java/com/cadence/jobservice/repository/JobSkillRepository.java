package com.cadence.jobservice.repository;

import com.cadence.jobservice.entity.JobSkill;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobSkillRepository extends JpaRepository<JobSkill, UUID> {
    List<JobSkill> findAllByJobId(UUID jobId);
    void deleteAllByJobId(UUID jobId);
}
