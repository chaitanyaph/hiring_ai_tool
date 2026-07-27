package com.cadence.analyticsservice.repository;

import com.cadence.analyticsservice.entity.RecruiterPerformanceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RecruiterPerformanceSnapshotRepository extends JpaRepository<RecruiterPerformanceSnapshot, UUID> {
    Optional<RecruiterPerformanceSnapshot> findByRecruiterIdAndPeriodDate(UUID recruiterId, LocalDate periodDate);
    List<RecruiterPerformanceSnapshot> findAllByCompanyIdAndPeriodDate(UUID companyId, LocalDate periodDate);
}
