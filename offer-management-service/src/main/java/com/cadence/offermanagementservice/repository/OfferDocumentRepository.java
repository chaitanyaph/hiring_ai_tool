package com.cadence.offermanagementservice.repository;

import com.cadence.offermanagementservice.entity.OfferDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OfferDocumentRepository extends JpaRepository<OfferDocument, UUID> {
    Optional<OfferDocument> findFirstByOfferIdOrderByGeneratedAtDesc(UUID offerId);
}
