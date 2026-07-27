package com.cadence.offermanagementservice.service.impl;

import com.cadence.offermanagementservice.constants.ActivityEventType;
import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.dto.request.CandidateDeclineRequest;
import com.cadence.offermanagementservice.dto.request.CandidateNegotiationRequest;
import com.cadence.offermanagementservice.dto.response.CandidateOfferResponse;
import com.cadence.offermanagementservice.entity.Offer;
import com.cadence.offermanagementservice.entity.OfferActivityLog;
import com.cadence.offermanagementservice.entity.OfferNegotiation;
import com.cadence.offermanagementservice.exception.AccessDeniedApiException;
import com.cadence.offermanagementservice.exception.ErrorCode;
import com.cadence.offermanagementservice.exception.OfferConflictException;
import com.cadence.offermanagementservice.exception.ResourceNotFoundException;
import com.cadence.offermanagementservice.feign.CompanyServiceClient;
import com.cadence.offermanagementservice.kafka.event.*;
import com.cadence.offermanagementservice.kafka.producer.OfferEventProducer;
import com.cadence.offermanagementservice.repository.OfferActivityLogRepository;
import com.cadence.offermanagementservice.repository.OfferDocumentRepository;
import com.cadence.offermanagementservice.repository.OfferNegotiationRepository;
import com.cadence.offermanagementservice.repository.OfferRepository;
import com.cadence.offermanagementservice.service.CandidateOfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class CandidateOfferServiceImpl implements CandidateOfferService {

    private final OfferRepository offerRepository;
    private final OfferDocumentRepository offerDocumentRepository;
    private final OfferNegotiationRepository offerNegotiationRepository;
    private final OfferActivityLogRepository offerActivityLogRepository;
    private final CompanyServiceClient companyServiceClient;
    private final OfferEventProducer eventProducer;

    @Override
    public List<CandidateOfferResponse> listMyOffers(UUID candidateId) {
        return offerRepository.findAllByCandidateId(candidateId).stream()
                .filter(o -> o.getStatus() != OfferStatus.DRAFT)
                .map(this::toCandidateResponse)
                .toList();
    }

    @Override
    public CandidateOfferResponse getMyOffer(UUID candidateId, UUID offerId) {
        return toCandidateResponse(findOwnedOffer(candidateId, offerId));
    }

    @Override
    public byte[] downloadMyOffer(UUID candidateId, UUID offerId) {
        findOwnedOffer(candidateId, offerId);
        return offerDocumentRepository.findFirstByOfferIdOrderByGeneratedAtDesc(offerId)
                .map(doc -> doc.getContent())
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OFFER_DOCUMENT_NOT_FOUND, "No offer letter is available yet"));
    }

    @Override
    @Transactional
    public CandidateOfferResponse accept(UUID candidateId, UUID offerId) {
        Offer offer = findOwnedOffer(candidateId, offerId);
        requireRespondable(offer);

        offer.setStatus(OfferStatus.ACCEPTED);
        offer.setAcceptedAt(LocalDateTime.now());
        offer = offerRepository.save(offer);
        logActivity(offer.getId(), ActivityEventType.ACCEPTED, candidateId, "Candidate accepted the offer");

        eventProducer.publishOfferAccepted(OfferAcceptedEvent.builder()
                .offerId(offer.getId()).applicationId(offer.getApplicationId()).candidateId(candidateId).build());
        eventProducer.publishCandidateOnboardingStarted(CandidateOnboardingStartedEvent.builder()
                .offerId(offer.getId()).applicationId(offer.getApplicationId()).candidateId(candidateId)
                .startDate(offer.getStartDate()).build());
        // Bridge onto application-service's own live topic/shape, also
        // consumed today by notification-service's OFFER_ACCEPTED template.
        eventProducer.publishApplicationOfferAccepted(ApplicationOfferAcceptedEvent.builder()
                .applicationId(offer.getApplicationId()).companyId(offer.getCompanyId()).jobId(offer.getJobId())
                .candidateId(candidateId).occurredAt(LocalDateTime.now()).build());

        return toCandidateResponse(offer);
    }

    @Override
    @Transactional
    public CandidateOfferResponse reject(UUID candidateId, UUID offerId, CandidateDeclineRequest request) {
        Offer offer = findOwnedOffer(candidateId, offerId);
        requireRespondable(offer);

        offer.setStatus(OfferStatus.DECLINED);
        offer.setDeclinedAt(LocalDateTime.now());
        offer.setDeclineReason(request != null ? request.getReason() : null);
        offer = offerRepository.save(offer);
        logActivity(offer.getId(), ActivityEventType.DECLINED, candidateId,
                offer.getDeclineReason() != null ? offer.getDeclineReason().name() : null);

        eventProducer.publishOfferRejected(OfferRejectedEvent.builder()
                .offerId(offer.getId()).applicationId(offer.getApplicationId()).candidateId(candidateId)
                .reason(offer.getDeclineReason() != null ? offer.getDeclineReason().name() : null).build());
        eventProducer.publishApplicationOfferRejected(ApplicationOfferRejectedEvent.builder()
                .applicationId(offer.getApplicationId()).companyId(offer.getCompanyId()).jobId(offer.getJobId())
                .candidateId(candidateId).occurredAt(LocalDateTime.now()).build());

        return toCandidateResponse(offer);
    }

    @Override
    @Transactional
    public void requestNegotiation(UUID candidateId, UUID offerId, CandidateNegotiationRequest request) {
        Offer offer = findOwnedOffer(candidateId, offerId);
        requireRespondable(offer);

        OfferNegotiation negotiation = offerNegotiationRepository.save(OfferNegotiation.builder()
                .offerId(offerId).candidateId(candidateId)
                .proposedCtc(request != null ? request.getProposedCtc() : null)
                .message(request != null ? request.getMessage() : null)
                .build());
        logActivity(offerId, ActivityEventType.NEGOTIATION_REQUESTED, candidateId, negotiation.getMessage());

        eventProducer.publishOfferNegotiationRequested(OfferNegotiationRequestedEvent.builder()
                .offerId(offerId).applicationId(offer.getApplicationId()).candidateId(candidateId)
                .proposedCtc(negotiation.getProposedCtc()).build());
    }

    private void requireRespondable(Offer offer) {
        if (offer.getStatus() != OfferStatus.SENT) {
            throw new OfferConflictException(ErrorCode.OFFER_NOT_RESPONDABLE, "This offer is not awaiting a response");
        }
    }

    private void logActivity(UUID offerId, ActivityEventType type, UUID actorId, String details) {
        offerActivityLogRepository.save(OfferActivityLog.builder()
                .offerId(offerId).eventType(type).actorId(actorId).details(details).build());
    }

    private CandidateOfferResponse toCandidateResponse(Offer offer) {
        Long daysUntilExpiry = offer.getExpiryDate() != null
                ? ChronoUnit.DAYS.between(LocalDate.now(), offer.getExpiryDate())
                : null;
        return CandidateOfferResponse.builder()
                .id(offer.getId())
                .jobTitle(offer.getJobTitle())
                .companyName(safeCompanyName(offer.getCompanyId()))
                .status(offer.getStatus())
                .baseSalary(offer.getBaseSalary())
                .variableBonus(offer.getVariableBonus())
                .esopEquity(offer.getEsopEquity())
                .totalCtc(offer.getTotalCtc())
                .startDate(offer.getStartDate())
                .expiryDate(offer.getExpiryDate())
                .daysUntilExpiry(daysUntilExpiry)
                .build();
    }

    private String safeCompanyName(UUID companyId) {
        if (companyId == null) return null;
        try {
            var data = companyServiceClient.getCompany(companyId).getData();
            return data != null ? data.getCompanyName() : null;
        } catch (Exception e) {
            return null;
        }
    }

    private Offer findOwnedOffer(UUID candidateId, UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OFFER_NOT_FOUND, "Offer not found: " + offerId));
        if (!offer.getCandidateId().equals(candidateId)) {
            throw new AccessDeniedApiException("This offer does not belong to you");
        }
        return offer;
    }
}
