package com.cadence.analyticsservice.repository;

import com.cadence.analyticsservice.entity.ReportExportLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReportExportLogRepository extends JpaRepository<ReportExportLog, UUID> {
}
