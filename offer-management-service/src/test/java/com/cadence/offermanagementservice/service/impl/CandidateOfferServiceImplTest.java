package com.cadence.offermanagementservice.service.impl;

import com.cadence.offermanagementservice.constants.DeclineReason;
import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.dto.request.CandidateDeclineRequest;
import com.cadence.offermanagementservice.dto.request.CandidateNegotiationRequest;
import com.cadence.offermanagementservice.entity.Offer;
import com.cadence.offermanagementservice.exception.AccessDeniedApiException;
import com.cadence.offermanagementservice.exception.OfferConflictException;
import com.cadence.offermanagementservice.feign.CompanyServiceClient;
import com.cadence.offermanagementservice.kafka.producer.OfferEventProducer;
import com.cadence.offermanagementservice.repository.OfferActivityLogRepository;
import com.cadence.offermanagementservice.repository.OfferDocumentRepository;
import com.cadence.offermanagementservice.repository.OfferNegotiationRepository;
import com.cadence.offermanagementservice.repository.OfferRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateOfferServiceImplTest {

    @Mock private OfferRepository offerRepository;
    @Mock private OfferDocumentRepository offerDocumentRepository;
    @Mock private OfferNegotiationRepository offerNegotiationRepository;
    @Mock private OfferActivityLogRepository offerActivityLogRepository;
    @Mock private CompanyServiceClient companyServiceClient;
    @Mock private OfferEventProducer eventProducer;

    private CandidateOfferServiceImpl candidateOfferService;

    private UUID candidateId;
    private final AtomicReference<Offer> saved = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        candidateOfferService = new CandidateOfferServiceImpl(offerRepository, offerDocumentRepository,
                offerNegotiationRepository, offerActivityLogRepository, companyServiceClient, eventProducer);
        candidateId = UUID.randomUUID();

        lenient().when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer o = inv.getArgument(0);
            saved.set(o);
            return o;
        });
        lenient().when(offerRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(saved.get()));
        lenient().when(companyServiceClient.getCompany(any())).thenThrow(new RuntimeException("unreachable in test"));
        lenient().when(offerNegotiationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Offer sentOffer(UUID offerId) {
        return Offer.builder().id(offerId).companyId(UUID.randomUUID()).applicationId(UUID.randomUUID())
                .jobId(UUID.randomUUID()).candidateId(candidateId).status(OfferStatus.SENT)
                .baseSalary(new BigDecimal("18")).totalCtc(new BigDecimal("20")).build();
    }

    @Test
    void accept_shouldTransitionAndPublishAllThreeEvents() {
        UUID offerId = UUID.randomUUID();
        saved.set(sentOffer(offerId));

        var response = candidateOfferService.accept(candidateId, offerId);

        assertThat(response.getStatus()).isEqualTo(OfferStatus.ACCEPTED);
        verify(eventProducer).publishOfferAccepted(any());
        verify(eventProducer).publishCandidateOnboardingStarted(any());
        verify(eventProducer).publishApplicationOfferAccepted(any());
    }

    @Test
    void accept_shouldThrow_whenOfferIsNotInSentStatus() {
        UUID offerId = UUID.randomUUID();
        Offer draft = sentOffer(offerId);
        draft.setStatus(OfferStatus.DRAFT);
        saved.set(draft);

        assertThatThrownBy(() -> candidateOfferService.accept(candidateId, offerId))
                .isInstanceOf(OfferConflictException.class);
    }

    @Test
    void accept_shouldThrow_whenOfferBelongsToAnotherCandidate() {
        UUID offerId = UUID.randomUUID();
        Offer other = sentOffer(offerId);
        other.setCandidateId(UUID.randomUUID());
        saved.set(other);

        assertThatThrownBy(() -> candidateOfferService.accept(candidateId, offerId))
                .isInstanceOf(AccessDeniedApiException.class);
    }

    @Test
    void reject_shouldSetDeclineReasonAndPublishBridgeEvent() {
        UUID offerId = UUID.randomUUID();
        saved.set(sentOffer(offerId));

        var response = candidateOfferService.reject(candidateId, offerId,
                CandidateDeclineRequest.builder().reason(DeclineReason.COMPENSATION_MISMATCH).build());

        assertThat(response.getStatus()).isEqualTo(OfferStatus.DECLINED);
        verify(eventProducer).publishOfferRejected(any());
        verify(eventProducer).publishApplicationOfferRejected(any());
    }

    @Test
    void requestNegotiation_shouldPersistAndPublishEvent() {
        UUID offerId = UUID.randomUUID();
        saved.set(sentOffer(offerId));

        candidateOfferService.requestNegotiation(candidateId, offerId,
                CandidateNegotiationRequest.builder().proposedCtc(new BigDecimal("22")).message("Hoping for a bit more").build());

        verify(offerNegotiationRepository).save(argThat(n -> n.getProposedCtc().compareTo(new BigDecimal("22")) == 0));
        verify(eventProducer).publishOfferNegotiationRequested(any());
    }
}
