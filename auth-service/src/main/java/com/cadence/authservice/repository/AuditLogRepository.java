package com.cadence.authservice.repository;

import com.cadence.authservice.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    Page<AuditLog> findAllByUserIdOrderByCreatedAtDesc(UUID userId, Pageable pageable);
}
