package com.cadence.aiinterviewservice.entity;

import com.cadence.aiinterviewservice.constants.LogLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "interview_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InterviewLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "session_id", nullable = false)
    private UUID sessionId;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 10)
    private LogLevel logLevel;

    @Column(name = "message", nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void prePersist() {
        this.createdAt = LocalDateTime.now();
    }
}
