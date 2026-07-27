package com.cadence.analyticsservice.export;

import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.dto.response.MonthlyPointResponse;
import com.cadence.analyticsservice.dto.response.ReportResponse;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CsvExportGeneratorTest {

    private final CsvExportGenerator generator = new CsvExportGenerator();

    @Test
    void generate_shouldProduceCsvWithHeadersAndRfc4180QuotedValues() {
        ReportResponse report = ReportResponse.builder()
                .reportType("MONTHLY")
                .periodLabel("Jul 2026")
                .generatedAt(LocalDateTime.of(2026, 7, 21, 10, 0))
                .totalApplications(100)
                .totalHires(10)
                .offersSent(20)
                .offersAccepted(15)
                .offersRejected(5)
                .offerAcceptanceRatePercent(75.0)
                .funnel(List.of(FunnelStageResponse.builder()
                        .stage("Applied, Screened").count(100).percentOfFirstStage(100.0).conversionFromPreviousStage(100.0).build()))
                .hiringTrend(List.of(MonthlyPointResponse.builder().monthLabel("Jul 2026").value(10).build()))
                .recruiterPerformance(List.of())
                .build();

        String csv = new String(generator.generate(report), StandardCharsets.UTF_8);

        assertThat(csv).contains("Report Type,Period,Generated At");
        assertThat(csv).contains("MONTHLY,Jul 2026");
        assertThat(csv).contains("Total Applications,100");
        assertThat(csv).contains("Offer Acceptance Rate %,75.0");
        // A comma inside a field forces RFC 4180 quoting.
        assertThat(csv).contains("\"Applied, Screened\"");
    }

    @Test
    void generate_shouldEscapeEmbeddedQuotesByDoublingThem() {
        ReportResponse report = ReportResponse.builder()
                .reportType("DAILY")
                .periodLabel("2026-07-21")
                .generatedAt(LocalDateTime.now())
                .funnel(List.of(FunnelStageResponse.builder().stage("\"Elite\" Track").count(5).build()))
                .hiringTrend(List.of())
                .recruiterPerformance(List.of())
                .build();

        String csv = new String(generator.generate(report), StandardCharsets.UTF_8);

        assertThat(csv).contains("\"\"\"Elite\"\" Track\"");
    }
}
