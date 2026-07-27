package com.cadence.offermanagementservice.repository;

import com.cadence.offermanagementservice.entity.OfferActivityLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface OfferActivityLogRepository extends JpaRepository<OfferActivityLog, UUID> {
    List<OfferActivityLog> findAllByOfferIdOrderByOccurredAtDesc(UUID offerId);
}
