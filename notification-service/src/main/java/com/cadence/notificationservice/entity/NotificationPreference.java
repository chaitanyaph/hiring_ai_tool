package com.cadence.notificationservice.entity;

import com.cadence.notificationservice.constants.PreferenceCategory;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "notification_preference")
public class NotificationPreference extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 40)
    private PreferenceCategory category;

    @Column(name = "enabled", nullable = false)
    @Builder.Default
    private boolean enabled = true;
}
