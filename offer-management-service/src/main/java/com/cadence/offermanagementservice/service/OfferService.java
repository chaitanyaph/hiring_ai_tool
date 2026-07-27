package com.cadence.offermanagementservice.service;

import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.dto.request.CreateOrUpdateOfferRequest;
import com.cadence.offermanagementservice.dto.response.*;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface OfferService {

    /** Kafka consumer entry point -- upserts a minimal placeholder DRAFT row so POST /api/v1/offers can fill it in rather than duplicate it. Not surfaced in any Figma screen, see README. */
    void upsertDraftFromCandidateSelected(UUID applicationId, UUID candidateId);

    OfferDetailResponse createOffer(UUID companyId, UUID recruiterId, CreateOrUpdateOfferRequest request);

    OfferDetailResponse updateOffer(UUID companyId, UUID offerId, CreateOrUpdateOfferRequest request);

    void deleteDraft(UUID companyId, UUID offerId);

    PagedResponse<OfferListItemResponse> listOffers(UUID companyId, OfferStatus status, Pageable pageable);

    OfferDetailResponse getOffer(UUID companyId, UUID offerId);

    OfferDashboardStatsResponse getDashboardStats(UUID companyId);

    byte[] generateDocument(UUID companyId, UUID offerId);

    byte[] previewDocument(UUID companyId, UUID offerId);

    byte[] downloadDocument(UUID companyId, UUID offerId);

    List<ActivityLogResponse> getActivity(UUID companyId, UUID offerId);
}
