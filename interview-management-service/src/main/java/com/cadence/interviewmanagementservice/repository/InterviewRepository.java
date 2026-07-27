package com.cadence.interviewmanagementservice.repository;

import com.cadence.interviewmanagementservice.constants.InterviewStatus;
import com.cadence.interviewmanagementservice.entity.Interview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface InterviewRepository extends JpaRepository<Interview, UUID> {

    Page<Interview> findAllByCompanyIdAndStatus(UUID companyId, InterviewStatus status, Pageable pageable);

    Page<Interview> findAllByCompanyId(UUID companyId, Pageable pageable);

    List<Interview> findAllByApplicationIdOrderByScheduledDateDescScheduledTimeDesc(UUID applicationId);

    List<Interview> findAllByCandidateIdOrderByScheduledDateDescScheduledTimeDesc(UUID candidateId);

    List<Interview> findAllByCompanyIdAndStatusAndScheduledDate(UUID companyId, InterviewStatus status, LocalDate date);
}
