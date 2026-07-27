package com.cadence.offermanagementservice.service.impl;

import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.dto.request.ApproveOfferRequest;
import com.cadence.offermanagementservice.dto.request.WithdrawOfferRequest;
import com.cadence.offermanagementservice.entity.Offer;
import com.cadence.offermanagementservice.exception.AccessDeniedApiException;
import com.cadence.offermanagementservice.exception.OfferConflictException;
import com.cadence.offermanagementservice.kafka.producer.OfferEventProducer;
import com.cadence.offermanagementservice.mapper.ActivityLogMapper;
import com.cadence.offermanagementservice.mapper.ActivityLogMapperImpl;
import com.cadence.offermanagementservice.mapper.NegotiationMapper;
import com.cadence.offermanagementservice.mapper.NegotiationMapperImpl;
import com.cadence.offermanagementservice.mapper.OfferMapper;
import com.cadence.offermanagementservice.mapper.OfferMapperImpl;
import com.cadence.offermanagementservice.repository.OfferActivityLogRepository;
import com.cadence.offermanagementservice.repository.OfferDocumentRepository;
import com.cadence.offermanagementservice.repository.OfferNegotiationRepository;
import com.cadence.offermanagementservice.repository.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferApprovalServiceImplTest {

    @Mock private OfferRepository offerRepository;
    @Mock private OfferActivityLogRepository offerActivityLogRepository;
    @Mock private OfferDocumentRepository offerDocumentRepository;
    @Mock private OfferNegotiationRepository offerNegotiationRepository;
    @Mock private OfferEventProducer eventProducer;

    private final OfferMapper offerMapper = new OfferMapperImpl();
    private final ActivityLogMapper activityLogMapper = new ActivityLogMapperImpl();
    private final NegotiationMapper negotiationMapper = new NegotiationMapperImpl();

    private OfferApprovalServiceImpl approvalService;

    private UUID companyId;
    private UUID approverId;
    private final AtomicReference<Offer> saved = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        approvalService = new OfferApprovalServiceImpl(offerRepository, offerActivityLogRepository,
                offerDocumentRepository, offerNegotiationRepository, offerMapper, activityLogMapper, negotiationMapper, eventProducer);
        companyId = UUID.randomUUID();
        approverId = UUID.randomUUID();

        lenient().when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer o = inv.getArgument(0);
            saved.set(o);
            return o;
        });
        lenient().when(offerRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(saved.get()));
        lenient().when(offerActivityLogRepository.findAllByOfferIdOrderByOccurredAtDesc(any())).thenReturn(List.of());
        lenient().when(offerNegotiationRepository.findAllByOfferIdOrderByRequestedAtDesc(any())).thenReturn(List.of());
        lenient().when(offerDocumentRepository.findFirstByOfferIdOrderByGeneratedAtDesc(any())).thenReturn(Optional.empty());
    }

    private Offer pendingApprovalOffer(UUID offerId) {
        return Offer.builder().id(offerId).companyId(companyId).applicationId(UUID.randomUUID())
                .candidateId(UUID.randomUUID()).approverId(approverId).status(OfferStatus.PENDING_APPROVAL).build();
    }

    @Test
    void submitForApproval_shouldTransitionDraftToPendingApproval() {
        UUID offerId = UUID.randomUUID();
        saved.set(Offer.builder().id(offerId).companyId(companyId).applicationId(UUID.randomUUID()).status(OfferStatus.DRAFT).build());

        var response = approvalService.submitForApproval(companyId, offerId, UUID.randomUUID());

        assertThat(response.getStatus()).isEqualTo(OfferStatus.PENDING_APPROVAL);
    }

    @Test
    void approve_shouldTransitionToSentAndPublishBridgeEvent_whenApproveTrue() {
        UUID offerId = UUID.randomUUID();
        saved.set(pendingApprovalOffer(offerId));

        var response = approvalService.approve(companyId, offerId, approverId, false,
                ApproveOfferRequest.builder().approve(true).build());

        assertThat(response.getStatus()).isEqualTo(OfferStatus.SENT);
        verify(eventProducer).publishOfferApproved(any());
        verify(eventProducer).publishOfferSent(any());
        verify(eventProducer).publishApplicationOfferReleased(any());
    }

    @Test
    void approve_shouldThrow_whenCallerNeitherApproverNorCompanyAdmin() {
        UUID offerId = UUID.randomUUID();
        saved.set(pendingApprovalOffer(offerId));

        assertThatThrownBy(() -> approvalService.approve(companyId, offerId, UUID.randomUUID(), false,
                ApproveOfferRequest.builder().approve(true).build()))
                .isInstanceOf(AccessDeniedApiException.class);
    }

    @Test
    void approve_shouldRevertToDraft_whenApproveFalse() {
        UUID offerId = UUID.randomUUID();
        saved.set(pendingApprovalOffer(offerId));

        var response = approvalService.approve(companyId, offerId, approverId, false,
                ApproveOfferRequest.builder().approve(false).notes("Comp too high").build());

        assertThat(response.getStatus()).isEqualTo(OfferStatus.DRAFT);
        verifyNoInteractions(eventProducer);
    }

    @Test
    void send_shouldLogReminderOnly_whenAlreadySent() {
        UUID offerId = UUID.randomUUID();
        saved.set(Offer.builder().id(offerId).companyId(companyId).applicationId(UUID.randomUUID()).status(OfferStatus.SENT).build());

        approvalService.send(companyId, offerId, UUID.randomUUID());

        verify(offerActivityLogRepository).save(argThat(l -> l.getEventType().name().equals("REMINDER_SENT")));
        verifyNoInteractions(eventProducer);
    }

    @Test
    void withdraw_shouldThrow_whenOfferIsNotSent() {
        UUID offerId = UUID.randomUUID();
        saved.set(Offer.builder().id(offerId).companyId(companyId).applicationId(UUID.randomUUID()).status(OfferStatus.DRAFT).build());

        assertThatThrownBy(() -> approvalService.withdraw(companyId, offerId, UUID.randomUUID(), WithdrawOfferRequest.builder().build()))
                .isInstanceOf(OfferConflictException.class);
    }
}
