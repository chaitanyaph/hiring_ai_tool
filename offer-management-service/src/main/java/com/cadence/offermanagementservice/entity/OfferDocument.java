package com.cadence.offermanagementservice.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/** Stored inline as LONGBLOB -- no object-storage client requested for this service, same reasoning as notification-service's email_attachment. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "offer_document")
public class OfferDocument {

    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "offer_id", nullable = false)
    private UUID offerId;

    @Column(name = "offer_number", nullable = false, length = 50)
    private String offerNumber;

    @Column(name = "file_name", nullable = false, length = 255)
    private String fileName;

    @Column(name = "content_type", nullable = false, length = 100)
    @Builder.Default
    private String contentType = "application/pdf";

    @Column(name = "size_bytes", nullable = false)
    private long sizeBytes;

    @Lob
    @Column(name = "content", nullable = false)
    private byte[] content;

    @Column(name = "generated_at", nullable = false)
    @Builder.Default
    private LocalDateTime generatedAt = LocalDateTime.now();
}
