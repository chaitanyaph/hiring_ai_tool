package com.cadence.offermanagementservice.repository;

import com.cadence.offermanagementservice.entity.OfferNegotiation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfferNegotiationRepository extends JpaRepository<OfferNegotiation, UUID> {
    List<OfferNegotiation> findAllByOfferIdOrderByRequestedAtDesc(UUID offerId);
}
