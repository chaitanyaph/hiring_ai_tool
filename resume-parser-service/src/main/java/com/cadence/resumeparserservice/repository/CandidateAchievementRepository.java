package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.CandidateAchievement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateAchievementRepository extends JpaRepository<CandidateAchievement, UUID> {
    List<CandidateAchievement> findAllByParsedResumeIdOrderByDisplayOrderAsc(UUID parsedResumeId);
    void deleteAllByParsedResumeId(UUID parsedResumeId);
}
