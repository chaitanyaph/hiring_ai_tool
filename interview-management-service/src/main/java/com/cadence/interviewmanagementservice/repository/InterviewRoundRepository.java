package com.cadence.interviewmanagementservice.repository;

import com.cadence.interviewmanagementservice.entity.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {
    List<InterviewRound> findAllByCompanyIdOrderByRoundOrderAsc(UUID companyId);
    List<InterviewRound> findAllByCompanyIdAndActiveTrueOrderByRoundOrderAsc(UUID companyId);
}
