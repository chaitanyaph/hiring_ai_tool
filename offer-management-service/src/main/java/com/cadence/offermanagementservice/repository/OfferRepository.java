package com.cadence.offermanagementservice.repository;

import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.entity.Offer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface OfferRepository extends JpaRepository<Offer, UUID> {

    Optional<Offer> findByApplicationId(UUID applicationId);

    Page<Offer> findAllByCompanyIdAndStatus(UUID companyId, OfferStatus status, Pageable pageable);

    Page<Offer> findAllByCompanyId(UUID companyId, Pageable pageable);

    List<Offer> findAllByCandidateId(UUID candidateId);

    List<Offer> findAllByStatusAndExpiryDateLessThan(OfferStatus status, LocalDate date);
}
