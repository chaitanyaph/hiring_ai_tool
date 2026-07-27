package com.cadence.resumeservice.entity;

import com.cadence.resumeservice.constants.ResumeStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * The file itself lives in MinIO (bucketName/objectName point to it);
 * this row is the queryable metadata + the single source of truth for
 * which resume is a candidate's default. candidateId is a plain id
 * into Candidate Service's database -- validated via Feign at upload
 * time, never joined.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
@Entity
@Table(name = "resumes")
public class Resume extends BaseAuditEntity {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "candidate_id", nullable = false)
    private UUID candidateId;

    @Column(name = "display_name", nullable = false, length = 150)
    private String displayName;

    @Column(name = "original_file_name", nullable = false, length = 255)
    private String originalFileName;

    @Column(name = "file_extension", nullable = false, length = 10)
    private String fileExtension;

    @Column(name = "mime_type", nullable = false, length = 100)
    private String mimeType;

    @Column(name = "bucket_name", nullable = false, length = 100)
    private String bucketName;

    @Column(name = "object_name", nullable = false, length = 500)
    private String objectName;

    @Column(name = "checksum", nullable = false, length = 64)
    private String checksum;

    @Column(name = "file_size", nullable = false)
    private long fileSize;

    @Column(name = "is_default", nullable = false)
    @Builder.Default
    private boolean defaultResume = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private ResumeStatus status = ResumeStatus.ACTIVE;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt;

    @PrePersist
    private void onUploadPersist() {
        if (this.uploadedAt == null) {
            this.uploadedAt = LocalDateTime.now();
        }
    }
}
