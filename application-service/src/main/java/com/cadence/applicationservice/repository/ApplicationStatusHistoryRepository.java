package com.cadence.applicationservice.repository;

import com.cadence.applicationservice.entity.ApplicationStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ApplicationStatusHistoryRepository extends JpaRepository<ApplicationStatusHistory, UUID> {
    List<ApplicationStatusHistory> findAllByApplicationIdOrderByChangedAtAsc(UUID applicationId);
}
