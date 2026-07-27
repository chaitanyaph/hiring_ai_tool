package com.cadence.notificationservice.repository;

import com.cadence.notificationservice.constants.NotificationStatus;
import com.cadence.notificationservice.entity.Notification;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    Page<Notification> findAllByRecipientIdAndStatus(UUID recipientId, NotificationStatus status, Pageable pageable);

    Page<Notification> findAllByRecipientIdAndStatusNot(UUID recipientId, NotificationStatus status, Pageable pageable);

    long countByRecipientIdAndStatus(UUID recipientId, NotificationStatus status);

    List<Notification> findAllByRecipientIdAndStatus(UUID recipientId, NotificationStatus status);

    @Modifying
    @Query("UPDATE Notification n SET n.status = 'READ', n.readAt = :now, n.updatedAt = :now WHERE n.recipientId = :recipientId AND n.status = 'UNREAD'")
    int markAllRead(@Param("recipientId") UUID recipientId, @Param("now") LocalDateTime now);
}
