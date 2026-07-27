package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.CandidateLanguage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateLanguageRepository extends JpaRepository<CandidateLanguage, UUID> {
    List<CandidateLanguage> findAllByParsedResumeId(UUID parsedResumeId);
    void deleteAllByParsedResumeId(UUID parsedResumeId);
}
