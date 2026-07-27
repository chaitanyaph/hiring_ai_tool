package com.cadence.offermanagementservice.service.impl;

import com.cadence.offermanagementservice.constants.ActivityEventType;
import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.dto.request.CreateOrUpdateOfferRequest;
import com.cadence.offermanagementservice.dto.response.*;
import com.cadence.offermanagementservice.entity.Offer;
import com.cadence.offermanagementservice.entity.OfferActivityLog;
import com.cadence.offermanagementservice.entity.OfferDocument;
import com.cadence.offermanagementservice.exception.ErrorCode;
import com.cadence.offermanagementservice.exception.OfferConflictException;
import com.cadence.offermanagementservice.exception.ResourceNotFoundException;
import com.cadence.offermanagementservice.feign.ApplicationServiceClient;
import com.cadence.offermanagementservice.feign.CandidateServiceClient;
import com.cadence.offermanagementservice.feign.CompanyServiceClient;
import com.cadence.offermanagementservice.feign.dto.ApplicationSummaryDto;
import com.cadence.offermanagementservice.kafka.event.OfferGeneratedEvent;
import com.cadence.offermanagementservice.kafka.producer.OfferEventProducer;
import com.cadence.offermanagementservice.mapper.ActivityLogMapper;
import com.cadence.offermanagementservice.mapper.NegotiationMapper;
import com.cadence.offermanagementservice.mapper.OfferMapper;
import com.cadence.offermanagementservice.pdf.OfferLetterPdfGenerator;
import com.cadence.offermanagementservice.repository.OfferActivityLogRepository;
import com.cadence.offermanagementservice.repository.OfferDocumentRepository;
import com.cadence.offermanagementservice.repository.OfferNegotiationRepository;
import com.cadence.offermanagementservice.repository.OfferRepository;
import com.cadence.offermanagementservice.service.OfferService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OfferServiceImpl implements OfferService {

    private final OfferRepository offerRepository;
    private final OfferDocumentRepository offerDocumentRepository;
    private final OfferNegotiationRepository offerNegotiationRepository;
    private final OfferActivityLogRepository offerActivityLogRepository;
    private final OfferMapper offerMapper;
    private final ActivityLogMapper activityLogMapper;
    private final NegotiationMapper negotiationMapper;
    private final CandidateServiceClient candidateServiceClient;
    private final CompanyServiceClient companyServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final OfferLetterPdfGenerator pdfGenerator;
    private final OfferEventProducer eventProducer;

    @Override
    @Transactional
    public void upsertDraftFromCandidateSelected(UUID applicationId, UUID candidateId) {
        if (offerRepository.findByApplicationId(applicationId).isPresent()) {
            return;
        }
        Offer draft = Offer.builder()
                .applicationId(applicationId)
                .candidateId(candidateId)
                .status(OfferStatus.DRAFT)
                .build();
        draft = offerRepository.save(draft);
        logActivity(draft.getId(), ActivityEventType.DRAFTED, null, "Auto-drafted on candidate selection");
    }

    @Override
    @Transactional
    public OfferDetailResponse createOffer(UUID companyId, UUID recruiterId, CreateOrUpdateOfferRequest request) {
        Optional<Offer> existing = offerRepository.findByApplicationId(request.getApplicationId());
        if (existing.isPresent() && existing.get().getJobId() != null) {
            throw new OfferConflictException(ErrorCode.OFFER_NOT_EDITABLE, "An offer already exists for this application");
        }
        Offer offer = existing.orElseGet(() -> Offer.builder().applicationId(request.getApplicationId()).status(OfferStatus.DRAFT).build());
        offer.setCompanyId(companyId);
        offer.setCreatedByRecruiterId(recruiterId);
        applyRequest(offer, request);
        enrichSnapshotFields(offer);
        offer = offerRepository.save(offer);
        logActivity(offer.getId(), ActivityEventType.DRAFTED, recruiterId, "Offer created");
        return toDetailResponse(offer);
    }

    @Override
    @Transactional
    public OfferDetailResponse updateOffer(UUID companyId, UUID offerId, CreateOrUpdateOfferRequest request) {
        Offer offer = findOwnedOffer(companyId, offerId);
        requireEditable(offer);
        applyRequest(offer, request);
        enrichSnapshotFields(offer);
        offer = offerRepository.save(offer);
        logActivity(offer.getId(), ActivityEventType.UPDATED, offer.getCreatedByRecruiterId(), "Offer updated");
        return toDetailResponse(offer);
    }

    @Override
    @Transactional
    public void deleteDraft(UUID companyId, UUID offerId) {
        Offer offer = findOwnedOffer(companyId, offerId);
        if (offer.getStatus() != OfferStatus.DRAFT) {
            throw new OfferConflictException(ErrorCode.OFFER_NOT_EDITABLE, "Only a draft offer can be deleted");
        }
        offerRepository.delete(offer);
    }

    @Override
    public PagedResponse<OfferListItemResponse> listOffers(UUID companyId, OfferStatus status, Pageable pageable) {
        Page<Offer> page = status != null
                ? offerRepository.findAllByCompanyIdAndStatus(companyId, status, pageable)
                : offerRepository.findAllByCompanyId(companyId, pageable);
        return PagedResponse.from(page.map(offerMapper::toListItemResponse));
    }

    @Override
    public OfferDetailResponse getOffer(UUID companyId, UUID offerId) {
        return toDetailResponse(findOwnedOffer(companyId, offerId));
    }

    @Override
    public OfferDashboardStatsResponse getDashboardStats(UUID companyId) {
        List<Offer> offers = offerRepository.findAllByCompanyId(companyId, org.springframework.data.domain.Pageable.unpaged()).getContent();
        long sent = offers.stream().filter(o -> o.getSentAt() != null).count();
        long accepted = offers.stream().filter(o -> o.getStatus() == OfferStatus.ACCEPTED).count();
        long declined = offers.stream().filter(o -> o.getStatus() == OfferStatus.DECLINED).count();
        long responded = accepted + declined;
        double acceptanceRate = responded == 0 ? 0.0 : (accepted * 100.0) / responded;
        double avgDays = offers.stream()
                .filter(o -> o.getStatus() == OfferStatus.ACCEPTED && o.getSentAt() != null && o.getAcceptedAt() != null)
                .mapToDouble(o -> java.time.Duration.between(o.getSentAt(), o.getAcceptedAt()).toHours() / 24.0)
                .average().orElse(0.0);
        long pendingApproval = offers.stream().filter(o -> o.getStatus() == OfferStatus.PENDING_APPROVAL).count();

        return OfferDashboardStatsResponse.builder()
                .offersSent(sent)
                .acceptanceRatePercent(Math.round(acceptanceRate * 10) / 10.0)
                .avgTimeToAcceptDays(Math.round(avgDays * 10) / 10.0)
                .pendingApprovalCount(pendingApproval)
                .build();
    }

    @Override
    @Transactional
    public byte[] generateDocument(UUID companyId, UUID offerId) {
        Offer offer = findOwnedOffer(companyId, offerId);
        String companyName = safeCompanyName(offer.getCompanyId());
        byte[] bytes = pdfGenerator.generate(offer, companyName);

        String offerNumber = "OFR-" + offer.getId().toString().substring(0, 8).toUpperCase();
        offerDocumentRepository.save(OfferDocument.builder()
                .offerId(offer.getId())
                .offerNumber(offerNumber)
                .fileName(offerNumber + ".pdf")
                .sizeBytes(bytes.length)
                .content(bytes)
                .build());

        logActivity(offer.getId(), ActivityEventType.GENERATED, offer.getCreatedByRecruiterId(), "Offer letter generated");
        eventProducer.publishOfferGenerated(OfferGeneratedEvent.builder()
                .offerId(offer.getId()).applicationId(offer.getApplicationId()).candidateId(offer.getCandidateId())
                .offerNumber(offerNumber).build());

        return bytes;
    }

    @Override
    public byte[] previewDocument(UUID companyId, UUID offerId) {
        Offer offer = findOwnedOffer(companyId, offerId);
        return pdfGenerator.generate(offer, safeCompanyName(offer.getCompanyId()));
    }

    @Override
    public byte[] downloadDocument(UUID companyId, UUID offerId) {
        findOwnedOffer(companyId, offerId);
        return offerDocumentRepository.findFirstByOfferIdOrderByGeneratedAtDesc(offerId)
                .map(OfferDocument::getContent)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.OFFER_DOCUMENT_NOT_FOUND,
                        "No offer letter has been generated yet for this offer"));
    }

    @Override
    public List<ActivityLogResponse> getActivity(UUID companyId, UUID offerId) {
        findOwnedOffer(companyId, offerId);
        return offerActivityLogRepository.findAllByOfferIdOrderByOccurredAtDesc(offerId).stream()
                .map(activityLogMapper::toResponse)
                .toList();
    }

    private void applyRequest(Offer offer, CreateOrUpdateOfferRequest request) {
        offer.setJobId(request.getJobId());
        offer.setCandidateId(request.getCandidateId());
        offer.setDepartment(request.getDepartment());
        offer.setEmploymentType(request.getEmploymentType());
        offer.setStartDate(request.getStartDate());
        offer.setBaseSalary(request.getBaseSalary());
        offer.setVariableBonus(request.getVariableBonus());
        offer.setEsopEquity(request.getEsopEquity());
        offer.setTotalCtc(sum(request.getBaseSalary(), request.getVariableBonus(), request.getEsopEquity()));
        offer.setBenefits(request.getBenefits() == null ? null : String.join(",", request.getBenefits()));
        offer.setApproverId(request.getApproverId());
        offer.setExpiryDate(request.getExpiryDate());
    }

    private BigDecimal sum(BigDecimal... values) {
        BigDecimal total = BigDecimal.ZERO;
        for (BigDecimal value : values) {
            if (value != null) {
                total = total.add(value);
            }
        }
        return total;
    }

    private void enrichSnapshotFields(Offer offer) {
        try {
            var candidate = candidateServiceClient.getCandidateSummary(offer.getCandidateId()).getData();
            if (candidate != null) {
                offer.setCandidateName(candidate.getFullName());
                offer.setCandidateEmail(candidate.getEmail());
            }
        } catch (Exception ignored) {
            // safe-degrade: keep whatever snapshot already exists
        }
        try {
            List<ApplicationSummaryDto> apps = applicationServiceClient.getApplicationsByJob(offer.getJobId()).getData();
            if (apps != null) {
                apps.stream().filter(a -> offer.getApplicationId().equals(a.getId())).findFirst()
                        .ifPresent(a -> offer.setJobTitle(a.getJobTitleSnapshot()));
            }
        } catch (Exception ignored) {
            // safe-degrade
        }
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

    private void requireEditable(Offer offer) {
        if (offer.getStatus() != OfferStatus.DRAFT && offer.getStatus() != OfferStatus.PENDING_APPROVAL) {
            throw new OfferConflictException(ErrorCode.OFFER_NOT_EDITABLE, "This offer can no longer be edited");
        }
    }

    private void logActivity(UUID offerId, ActivityEventType type, UUID actorId, String details) {
        offerActivityLogRepository.save(OfferActivityLog.builder()
                .offerId(offerId).eventType(type).actorId(actorId).details(details).build());
    }

    private OfferDetailResponse toDetailResponse(Offer offer) {
        OfferDetailResponse response = offerMapper.toDetailResponse(offer);
        response.setCompanyName(safeCompanyName(offer.getCompanyId()));
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
