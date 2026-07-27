package com.cadence.notificationservice.repository;

import com.cadence.notificationservice.constants.TemplateCategory;
import com.cadence.notificationservice.entity.NotificationTemplate;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface NotificationTemplateRepository extends JpaRepository<NotificationTemplate, UUID> {
    Optional<NotificationTemplate> findByCategory(TemplateCategory category);
}
