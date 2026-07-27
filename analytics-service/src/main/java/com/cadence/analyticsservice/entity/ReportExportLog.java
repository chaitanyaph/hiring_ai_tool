package com.cadence.analyticsservice.entity;

import com.cadence.analyticsservice.constants.ReportFormat;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Audit trail only -- no bytes persisted, reports are regenerable from metric_snapshot on demand. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "report_export_log")
public class ReportExportLog {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "report_type", nullable = false, length = 30)
    private String reportType;

    @Enumerated(EnumType.STRING)
    @Column(name = "format", length = 10)
    private ReportFormat format;

    @Column(name = "requested_by")
    private UUID requestedBy;

    @Column(name = "company_id")
    private UUID companyId;

    @Column(name = "filters", length = 500)
    private String filters;

    @Column(name = "file_size_bytes")
    private Long fileSizeBytes;

    @Column(name = "requested_at", nullable = false)
    @Builder.Default
    private LocalDateTime requestedAt = LocalDateTime.now();
}
