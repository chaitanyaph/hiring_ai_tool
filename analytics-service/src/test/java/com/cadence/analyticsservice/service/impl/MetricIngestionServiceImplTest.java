package com.cadence.analyticsservice.service.impl;

import com.cadence.analyticsservice.constants.MetricKey;
import com.cadence.analyticsservice.constants.MetricScope;
import com.cadence.analyticsservice.constants.PeriodType;
import com.cadence.analyticsservice.entity.MetricSnapshot;
import com.cadence.analyticsservice.entity.RecruiterPerformanceSnapshot;
import com.cadence.analyticsservice.kafka.event.ApplicationStatusChangedEvent;
import com.cadence.analyticsservice.kafka.event.CompanyCreatedEvent;
import com.cadence.analyticsservice.kafka.event.RecruiterAssignedEvent;
import com.cadence.analyticsservice.repository.AnalyticsActivityLogRepository;
import com.cadence.analyticsservice.repository.MetricSnapshotRepository;
import com.cadence.analyticsservice.repository.RecruiterPerformanceSnapshotRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MetricIngestionServiceImplTest {

    @Mock
    private MetricSnapshotRepository metricSnapshotRepository;
    @Mock
    private RecruiterPerformanceSnapshotRepository recruiterPerformanceSnapshotRepository;
    @Mock
    private AnalyticsActivityLogRepository analyticsActivityLogRepository;

    private MetricIngestionServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new MetricIngestionServiceImpl(metricSnapshotRepository, recruiterPerformanceSnapshotRepository, analyticsActivityLogRepository);
        lenient().when(metricSnapshotRepository.findByScopeAndScopeIdAndMetricKeyAndDimensionAndPeriodTypeAndPeriodDate(
                        any(), any(), any(), any(), any(), any()))
                .thenReturn(Optional.empty());
        lenient().when(metricSnapshotRepository.save(any(MetricSnapshot.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void onCompanyCreated_shouldIncrementTotalAndActiveCompaniesAtGlobalScope() {
        UUID companyId = UUID.randomUUID();
        service.onCompanyCreated(CompanyCreatedEvent.builder().companyId(companyId).companyName("Acme").build());

        ArgumentCaptor<MetricSnapshot> captor = ArgumentCaptor.forClass(MetricSnapshot.class);
        verify(metricSnapshotRepository, times(2)).save(captor.capture());

        assertThat(captor.getAllValues())
                .allMatch(s -> s.getScope() == MetricScope.GLOBAL && s.getScopeId().equals(MetricScope.NO_SCOPE_ID))
                .extracting(MetricSnapshot::getMetricKey)
                .containsExactlyInAnyOrder(MetricKey.TOTAL_COMPANIES, MetricKey.ACTIVE_COMPANIES);
    }

    @Test
    void onApplicationStatusChanged_toHired_shouldIncrementFunnelHiresAtBothScopesPlusMonthlyBucket() {
        UUID companyId = UUID.randomUUID();
        ApplicationStatusChangedEvent event = ApplicationStatusChangedEvent.builder()
                .applicationId(UUID.randomUUID()).companyId(companyId).jobId(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .fromStatus("OFFER_EXTENDED").toStatus("HIRED").occurredAt(LocalDateTime.now())
                .build();

        service.onApplicationStatusChanged(event);

        ArgumentCaptor<MetricSnapshot> captor = ArgumentCaptor.forClass(MetricSnapshot.class);
        verify(metricSnapshotRepository, times(5)).save(captor.capture());

        List<MetricSnapshot> saved = captor.getAllValues();
        assertThat(saved).filteredOn(s -> s.getMetricKey() == MetricKey.FUNNEL_STAGE)
                .hasSize(2)
                .allMatch(s -> "HIRED".equals(s.getDimension()));
        assertThat(saved).filteredOn(s -> s.getMetricKey() == MetricKey.HIRES && s.getPeriodType() == PeriodType.ALL_TIME)
                .hasSize(2);
        assertThat(saved).filteredOn(s -> s.getMetricKey() == MetricKey.HIRES && s.getPeriodType() == PeriodType.MONTHLY)
                .hasSize(1)
                .allMatch(s -> s.getScope() == MetricScope.COMPANY && s.getScopeId().equals(companyId));
    }

    @Test
    void onApplicationStatusChanged_nonHiredStage_shouldOnlyIncrementFunnelStage() {
        ApplicationStatusChangedEvent event = ApplicationStatusChangedEvent.builder()
                .applicationId(UUID.randomUUID()).companyId(UUID.randomUUID()).jobId(UUID.randomUUID()).candidateId(UUID.randomUUID())
                .fromStatus("APPLIED").toStatus("SCREENING").occurredAt(LocalDateTime.now())
                .build();

        service.onApplicationStatusChanged(event);

        verify(metricSnapshotRepository, times(2)).save(any(MetricSnapshot.class));
    }

    @Test
    void onRecruiterAssigned_shouldUpsertApplicationsReviewedCount() {
        UUID companyId = UUID.randomUUID();
        UUID recruiterId = UUID.randomUUID();
        RecruiterAssignedEvent event = RecruiterAssignedEvent.builder()
                .applicationId(UUID.randomUUID()).companyId(companyId).recruiterId(recruiterId).occurredAt(LocalDateTime.now())
                .build();

        when(recruiterPerformanceSnapshotRepository.findByRecruiterIdAndPeriodDate(recruiterId, PeriodType.ALL_TIME_DATE))
                .thenReturn(Optional.empty());
        when(recruiterPerformanceSnapshotRepository.save(any(RecruiterPerformanceSnapshot.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        service.onRecruiterAssigned(event);

        ArgumentCaptor<RecruiterPerformanceSnapshot> captor = ArgumentCaptor.forClass(RecruiterPerformanceSnapshot.class);
        verify(recruiterPerformanceSnapshotRepository).save(captor.capture());
        assertThat(captor.getValue().getApplicationsReviewed()).isEqualTo(1);
        assertThat(captor.getValue().getRecruiterId()).isEqualTo(recruiterId);
    }
}
