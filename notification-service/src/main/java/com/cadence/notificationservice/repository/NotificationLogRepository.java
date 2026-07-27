package com.cadence.notificationservice.repository;

import com.cadence.notificationservice.entity.NotificationLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationLogRepository extends JpaRepository<NotificationLog, java.util.UUID> {
    Page<NotificationLog> findAllByOrderByOccurredAtDesc(Pageable pageable);
    Page<NotificationLog> findAllBySourceOrderByOccurredAtDesc(String source, Pageable pageable);
}
