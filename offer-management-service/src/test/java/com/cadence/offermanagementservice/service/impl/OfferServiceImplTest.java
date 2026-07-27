package com.cadence.offermanagementservice.service.impl;

import com.cadence.offermanagementservice.constants.EmploymentType;
import com.cadence.offermanagementservice.constants.OfferStatus;
import com.cadence.offermanagementservice.constants.SendMode;
import com.cadence.offermanagementservice.dto.request.CreateOrUpdateOfferRequest;
import com.cadence.offermanagementservice.entity.Offer;
import com.cadence.offermanagementservice.exception.OfferConflictException;
import com.cadence.offermanagementservice.feign.ApplicationServiceClient;
import com.cadence.offermanagementservice.feign.CandidateServiceClient;
import com.cadence.offermanagementservice.feign.CompanyServiceClient;
import com.cadence.offermanagementservice.mapper.ActivityLogMapper;
import com.cadence.offermanagementservice.mapper.ActivityLogMapperImpl;
import com.cadence.offermanagementservice.mapper.NegotiationMapper;
import com.cadence.offermanagementservice.mapper.NegotiationMapperImpl;
import com.cadence.offermanagementservice.mapper.OfferMapper;
import com.cadence.offermanagementservice.mapper.OfferMapperImpl;
import com.cadence.offermanagementservice.kafka.producer.OfferEventProducer;
import com.cadence.offermanagementservice.pdf.OfferLetterPdfGenerator;
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
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfferServiceImplTest {

    @Mock private OfferRepository offerRepository;
    @Mock private OfferDocumentRepository offerDocumentRepository;
    @Mock private OfferNegotiationRepository offerNegotiationRepository;
    @Mock private OfferActivityLogRepository offerActivityLogRepository;
    @Mock private CandidateServiceClient candidateServiceClient;
    @Mock private CompanyServiceClient companyServiceClient;
    @Mock private ApplicationServiceClient applicationServiceClient;
    @Mock private OfferLetterPdfGenerator pdfGenerator;
    @Mock private OfferEventProducer eventProducer;

    private final OfferMapper offerMapper = new OfferMapperImpl();
    private final ActivityLogMapper activityLogMapper = new ActivityLogMapperImpl();
    private final NegotiationMapper negotiationMapper = new NegotiationMapperImpl();

    private OfferServiceImpl offerService;

    private UUID companyId;
    private UUID recruiterId;
    private final AtomicReference<Offer> saved = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        offerService = new OfferServiceImpl(offerRepository, offerDocumentRepository, offerNegotiationRepository,
                offerActivityLogRepository, offerMapper, activityLogMapper, negotiationMapper,
                candidateServiceClient, companyServiceClient, applicationServiceClient, pdfGenerator, eventProducer);
        companyId = UUID.randomUUID();
        recruiterId = UUID.randomUUID();

        lenient().when(offerRepository.save(any(Offer.class))).thenAnswer(inv -> {
            Offer o = inv.getArgument(0);
            saved.set(o);
            return o;
        });
        lenient().when(offerRepository.findById(any())).thenAnswer(inv -> Optional.ofNullable(saved.get()));
        lenient().when(candidateServiceClient.getCandidateSummary(any())).thenThrow(new RuntimeException("unreachable in test"));
        lenient().when(companyServiceClient.getCompany(any())).thenThrow(new RuntimeException("unreachable in test"));
        lenient().when(applicationServiceClient.getApplicationsByJob(any())).thenThrow(new RuntimeException("unreachable in test"));
        lenient().when(offerActivityLogRepository.findAllByOfferIdOrderByOccurredAtDesc(any())).thenReturn(List.of());
        lenient().when(offerNegotiationRepository.findAllByOfferIdOrderByRequestedAtDesc(any())).thenReturn(List.of());
        lenient().when(offerDocumentRepository.findFirstByOfferIdOrderByGeneratedAtDesc(any())).thenReturn(Optional.empty());
    }

    private CreateOrUpdateOfferRequest baseRequest() {
        return CreateOrUpdateOfferRequest.builder()
                .applicationId(UUID.randomUUID()).jobId(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .employmentType(EmploymentType.FULL_TIME).startDate(LocalDate.now().plusMonths(1))
                .baseSalary(new BigDecimal("18")).variableBonus(new BigDecimal("2")).esopEquity(BigDecimal.ZERO)
                .benefits(List.of("Health insurance")).expiryDate(LocalDate.now().plusDays(14))
                .sendMode(SendMode.DRAFT).build();
    }

    @Test
    void upsertDraftFromCandidateSelected_shouldCreateDraft_whenNoneExists() {
        UUID applicationId = UUID.randomUUID();
        UUID candidateId = UUID.randomUUID();
        when(offerRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        offerService.upsertDraftFromCandidateSelected(applicationId, candidateId);

        verify(offerRepository).save(argThat(o -> o.getStatus() == OfferStatus.DRAFT && o.getApplicationId().equals(applicationId)));
    }

    @Test
    void upsertDraftFromCandidateSelected_shouldNoOp_whenDraftAlreadyExists() {
        UUID applicationId = UUID.randomUUID();
        when(offerRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(Offer.builder().id(UUID.randomUUID()).applicationId(applicationId).build()));

        offerService.upsertDraftFromCandidateSelected(applicationId, UUID.randomUUID());

        verify(offerRepository, never()).save(any());
    }

    @Test
    void createOffer_shouldComputeTotalCtc() {
        when(offerRepository.findByApplicationId(any())).thenReturn(Optional.empty());

        var response = offerService.createOffer(companyId, recruiterId, baseRequest());

        assertThat(response.getTotalCtc()).isEqualByComparingTo("20");
    }

    @Test
    void createOffer_shouldThrow_whenNonDraftOfferAlreadyExistsForApplication() {
        CreateOrUpdateOfferRequest request = baseRequest();
        Offer existing = Offer.builder().id(UUID.randomUUID()).applicationId(request.getApplicationId())
                .jobId(UUID.randomUUID()).status(OfferStatus.SENT).build();
        when(offerRepository.findByApplicationId(request.getApplicationId())).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> offerService.createOffer(companyId, recruiterId, request))
                .isInstanceOf(OfferConflictException.class);
    }

    @Test
    void updateOffer_shouldThrow_whenOfferIsNotEditable() {
        UUID offerId = UUID.randomUUID();
        Offer sent = Offer.builder().id(offerId).companyId(companyId).applicationId(UUID.randomUUID())
                .status(OfferStatus.SENT).build();
        saved.set(sent);

        assertThatThrownBy(() -> offerService.updateOffer(companyId, offerId, baseRequest()))
                .isInstanceOf(OfferConflictException.class);
    }

    @Test
    void deleteDraft_shouldThrow_whenOfferIsNotDraft() {
        UUID offerId = UUID.randomUUID();
        Offer sent = Offer.builder().id(offerId).companyId(companyId).applicationId(UUID.randomUUID())
                .status(OfferStatus.SENT).build();
        saved.set(sent);

        assertThatThrownBy(() -> offerService.deleteDraft(companyId, offerId))
                .isInstanceOf(OfferConflictException.class);
        verify(offerRepository, never()).delete(any());
    }

    @Test
    void generateDocument_shouldPersistDocumentAndPublishEvent() {
        UUID offerId = UUID.randomUUID();
        Offer offer = Offer.builder().id(offerId).companyId(companyId).applicationId(UUID.randomUUID())
                .candidateId(UUID.randomUUID()).status(OfferStatus.DRAFT).build();
        saved.set(offer);
        when(pdfGenerator.generate(any(), any())).thenReturn(new byte[]{1, 2, 3});

        byte[] result = offerService.generateDocument(companyId, offerId);

        assertThat(result).isEqualTo(new byte[]{1, 2, 3});
        verify(offerDocumentRepository).save(argThat(doc -> doc.getOfferId().equals(offerId) && doc.getSizeBytes() == 3));
        verify(eventProducer).publishOfferGenerated(any());
    }
}
