package com.cadence.offermanagementservice.service.impl;

import com.cadence.offermanagementservice.constants.ActivityEventType;
import com.cadence.offermanagementservice.constants.ApprovalStatus;
import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.dto.request.ApproveOfferRequest;
import com.cadence.offermanagementservice.dto.request.WithdrawOfferRequest;
import com.cadence.offermanagementservice.dto.response.OfferDetailResponse;
import com.cadence.offermanagementservice.entity.Offer;
import com.cadence.offermanagementservice.entity.OfferActivityLog;
import com.cadence.offermanagementservice.exception.AccessDeniedApiException;
import com.cadence.offermanagementservice.exception.ErrorCode;
import com.cadence.offermanagementservice.exception.OfferConflictException;
import com.cadence.offermanagementservice.exception.ResourceNotFoundException;
import com.cadence.offermanagementservice.kafka.event.ApplicationOfferReleasedEvent;
import com.cadence.offermanagementservice.kafka.event.OfferApprovedEvent;
import com.cadence.offermanagementservice.kafka.event.OfferSentEvent;
import com.cadence.offermanagementservice.kafka.producer.OfferEventProducer;
import com.cadence.offermanagementservice.mapper.ActivityLogMapper;
import com.cadence.offermanagementservice.mapper.NegotiationMapper;
import com.cadence.offermanagementservice.mapper.OfferMapper;
import com.cadence.offermanagementservice.repository.OfferActivityLogRepository;
import com.cadence.offermanagementservice.repository.OfferDocumentRepository;
import com.cadence.offermanagementservice.repository.OfferNegotiationRepository;
import com.cadence.offermanagementservice.repository.OfferRepository;
import com.cadence.offermanagementservice.service.OfferApprovalService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferApprovalServiceImpl implements OfferApprovalService {

    private final OfferRepository offerRepository;
    private final OfferActivityLogRepository offerActivityLogRepository;
    private final OfferDocumentRepository offerDocumentRepository;
    private final OfferNegotiationRepository offerNegotiationRepository;
    private final OfferMapper offerMapper;
    private final ActivityLogMapper activityLogMapper;
    private final NegotiationMapper negotiationMapper;
    private final OfferEventProducer eventProducer;

    @Override
    @Transactional
    public OfferDetailResponse submitForApproval(UUID companyId, UUID offerId, UUID recruiterId) {
        Offer offer = findOwnedOffer(companyId, offerId);
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new OfferConflictException(ErrorCode.OFFER_NOT_APPROVABLE, "Only a draft offer can be submitted for approval");
        }
        offer.setStatus(OfferStatus.PENDING_APPROVAL);
        offer.setApprovalStatus(ApprovalStatus.PENDING);
        offer = offerRepository.save(offer);
        logActivity(offer.getId(), ActivityEventType.SUBMITTED_FOR_APPROVAL, recruiterId, "Submitted for approval");
        return toDetailResponse(offer);
    }

    @Override
    @Transactional
    public OfferDetailResponse approve(UUID companyId, UUID offerId, UUID callerId, boolean callerIsCompanyAdmin, ApproveOfferRequest request) {
        Offer offer = findOwnedOffer(companyId, offerId);
        if (offer.getStatus() != OfferStatus.PENDING_APPROVAL) {
            throw new OfferConflictException(ErrorCode.OFFER_NOT_APPROVABLE, "This offer is not pending approval");
        }
        if (!callerIsCompanyAdmin && (offer.getApproverId() == null || !offer.getApproverId().equals(callerId))) {
            throw new AccessDeniedApiException("Only the designated approver or a company admin can approve this offer");
        }

        if (!request.isApprove()) {
            offer.setApprovalStatus(ApprovalStatus.REJECTED);
            offer.setApprovalNotes(request.getNotes());
            offer.setStatus(OfferStatus.DRAFT);
            offer = offerRepository.save(offer);
            logActivity(offer.getId(), ActivityEventType.APPROVAL_REJECTED, callerId, request.getNotes());
            return toDetailResponse(offer);
        }

        offer.setApprovalStatus(ApprovalStatus.APPROVED);
        offer.setApprovedAt(LocalDateTime.now());
        offer.setApprovalNotes(request.getNotes());
        offer = offerRepository.save(offer);
        logActivity(offer.getId(), ActivityEventType.APPROVED, callerId, request.getNotes());
        eventProducer.publishOfferApproved(OfferApprovedEvent.builder()
                .offerId(offer.getId()).applicationId(offer.getApplicationId()).approverId(callerId).build());

        return doSend(offer);
    }

    @Override
    @Transactional
    public OfferDetailResponse send(UUID companyId, UUID offerId, UUID recruiterId) {
        Offer offer = findOwnedOffer(companyId, offerId);
        if (offer.getStatus() == OfferStatus.SENT) {
            logActivity(offer.getId(), ActivityEventType.REMINDER_SENT, recruiterId, "Reminder sent to candidate");
            return toDetailResponse(offer);
        }
        if (offer.getStatus() != OfferStatus.DRAFT && offer.getStatus() != OfferStatus.PENDING_APPROVAL) {
            throw new OfferConflictException(ErrorCode.OFFER_NOT_SENDABLE, "This offer cannot be sent in its current state");
        }
        return doSend(offer);
    }

    @Override
    @Transactional
    public OfferDetailResponse withdraw(UUID companyId, UUID offerId, UUID recruiterId, WithdrawOfferRequest request) {
        Offer offer = findOwnedOffer(companyId, offerId);
        if (offer.getStatus() != OfferStatus.SENT) {
            throw new OfferConflictException(ErrorCode.OFFER_NOT_WITHDRAWABLE, "Only a sent offer awaiting response can be withdrawn");
        }
        offer.setStatus(OfferStatus.WITHDRAWN);
        offer.setWithdrawnAt(LocalDateTime.now());
        offer.setWithdrawReason(request != null ? request.getReason() : null);
        offer = offerRepository.save(offer);
        logActivity(offer.getId(), ActivityEventType.WITHDRAWN, recruiterId, offer.getWithdrawReason());
        return toDetailResponse(offer);
    }

    /** Shared by both the direct "send" path and the "approve & send" combined action. */
    private OfferDetailResponse doSend(Offer offer) {
        offer.setStatus(OfferStatus.SENT);
        offer.setSentAt(LocalDateTime.now());
        offer = offerRepository.save(offer);
        logActivity(offer.getId(), ActivityEventType.SENT, offer.getCreatedByRecruiterId(), "Offer sent to candidate");

        eventProducer.publishOfferSent(OfferSentEvent.builder()
                .offerId(offer.getId()).applicationId(offer.getApplicationId()).candidateId(offer.getCandidateId())
                .candidateEmail(offer.getCandidateEmail()).build());
        // The real integration value: bridges onto application-service's own
        // already-live consumer, which only transitions status when current
        // status is exactly BACKGROUND_VERIFICATION -- see README.
        eventProducer.publishApplicationOfferReleased(ApplicationOfferReleasedEvent.builder()
                .applicationId(offer.getApplicationId()).offerId(offer.getId()).build());

        return toDetailResponse(offer);
    }

    private void logActivity(UUID offerId, ActivityEventType type, UUID actorId, String details) {
        offerActivityLogRepository.save(OfferActivityLog.builder()
                .offerId(offerId).eventType(type).actorId(actorId).details(details).build());
    }

    private OfferDetailResponse toDetailResponse(Offer offer) {
        OfferDetailResponse response = offerMapper.toDetailResponse(offer);
        response.setDocumentGenerated(offerDocumentRepository.findFirstByOfferIdOrderByGeneratedAtDesc(offer.getId()).isPresent());
        response.setTimeline(offerActivityLogRepository.findAllByOfferIdOrderByOccurredAtDesc(offer.getId()).stream()
                .map(activityLogMapper::toResponse).toList());
        response.setNegotiations(offerNegotiationRepository.findAllByOfferIdOrderByRequestedAtDesc(offer.getId()).stream()
                .map(negotiationMapper::toResponse).toList());
        return response;
    }

    private Offer findOwnedOffer(UUID companyId, UUID offerId) {
        Offer offer = offerRepository.findById(offerId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OFFER_NOT_FOUND, "Offer not found: " + offerId));
        if (offer.getCompanyId() != null && !offer.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.OFFER_NOT_FOUND, "Offer not found: " + offerId);
        }
        return offer;
    }
}
