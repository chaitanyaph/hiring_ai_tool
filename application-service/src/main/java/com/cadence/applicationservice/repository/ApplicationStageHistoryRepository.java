package com.cadence.applicationservice.repository;

import com.cadence.applicationservice.entity.ApplicationStageHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationStageHistoryRepository extends JpaRepository<ApplicationStageHistory, UUID> {
    List<ApplicationStageHistory> findAllByApplicationIdOrderByChangedAtAsc(UUID applicationId);
}
