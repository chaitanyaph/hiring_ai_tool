package com.cadence.resumeparserservice.repository;

import com.cadence.resumeparserservice.entity.CandidateCertification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface CandidateCertificationRepository extends JpaRepository<CandidateCertification, UUID> {
    List<CandidateCertification> findAllByParsedResumeIdOrderByDisplayOrderAsc(UUID parsedResumeId);
    void deleteAllByParsedResumeId(UUID parsedResumeId);
}
