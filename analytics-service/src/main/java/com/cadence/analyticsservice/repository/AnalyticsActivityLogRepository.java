package com.cadence.analyticsservice.repository;

import com.cadence.analyticsservice.entity.AnalyticsActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnalyticsActivityLogRepository extends JpaRepository<AnalyticsActivityLog, UUID> {
}
