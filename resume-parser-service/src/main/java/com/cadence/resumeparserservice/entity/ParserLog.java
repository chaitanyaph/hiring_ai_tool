package com.cadence.resumeparserservice.entity;

import com.cadence.resumeparserservice.constants.LogLevel;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Append-only audit/debug trail feeding the drawer's "Parsing logs"
 * box -- never updated after insert, so it carries no version/updated
 * columns, just a single createdAt.
 */
@Entity
@Table(name = "parser_log")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParserLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "parsed_resume_id", nullable = false)
    private UUID parsedResumeId;

    @Column(name = "resume_id", nullable = false)
    private UUID resumeId;

    @Enumerated(EnumType.STRING)
    @Column(name = "log_level", nullable = false, length = 10)
    private LogLevel logLevel;

    @Column(name = "message", nullable = false, length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    private void onPersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
