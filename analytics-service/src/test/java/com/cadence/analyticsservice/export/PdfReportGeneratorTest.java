package com.cadence.analyticsservice.export;

import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;
import com.cadence.analyticsservice.dto.response.ReportResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfReportGeneratorTest {

    private final PdfReportGenerator generator = new PdfReportGenerator();

    @Test
    void generate_shouldProduceNonEmptyPdfBytes() {
        ReportResponse report = ReportResponse.builder()
                .reportType("MONTHLY")
                .periodLabel("Jul 2026")
                .generatedAt(LocalDateTime.now())
                .totalApplications(100)
                .totalHires(10)
                .offersSent(20)
                .offersAccepted(15)
                .offersRejected(5)
                .offerAcceptanceRatePercent(75.0)
                .funnel(List.of(FunnelStageResponse.builder().stage("APPLIED").count(100).build()))
                .hiringTrend(List.of())
                .recruiterPerformance(List.of(RecruiterPerformanceResponse.builder()
                        .recruiterId(UUID.randomUUID()).recruiterName("Jane Doe").openReqs(3).applicationsReviewed(40).build()))
                .build();

        byte[] pdf = generator.generate(report);

        assertThat(pdf).isNotEmpty();
        // Every valid PDF file starts with the "%PDF-" magic bytes.
        assertThat(new String(pdf, 0, 5, StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
