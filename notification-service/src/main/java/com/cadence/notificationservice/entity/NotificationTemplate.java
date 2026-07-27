package com.cadence.notificationservice.entity;

import com.cadence.notificationservice.constants.TemplateCategory;
import com.cadence.notificationservice.constants.TriggerEvent;
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
@Table(name = "notification_template")
public class NotificationTemplate extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "name", nullable = false, length = 150)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "trigger_event", nullable = false, length = 50)
    private TriggerEvent triggerEvent;

    @Enumerated(EnumType.STRING)
    @Column(name = "category", nullable = false, length = 50)
    private TemplateCategory category;

    @Column(name = "subject", nullable = false, length = 500)
    private String subject;

    @Column(name = "body_html", nullable = false, columnDefinition = "LONGTEXT")
    private String bodyHtml;

    @Column(name = "variables_hint", length = 500)
    private String variablesHint;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private boolean active = true;
}
