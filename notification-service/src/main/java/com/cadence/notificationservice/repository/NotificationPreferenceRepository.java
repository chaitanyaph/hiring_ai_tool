package com.cadence.notificationservice.repository;

import com.cadence.notificationservice.constants.PreferenceCategory;
import com.cadence.notificationservice.entity.NotificationPreference;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface NotificationPreferenceRepository extends JpaRepository<NotificationPreference, UUID> {
    List<NotificationPreference> findAllByUserId(UUID userId);
    Optional<NotificationPreference> findByUserIdAndCategory(UUID userId, PreferenceCategory category);
}
