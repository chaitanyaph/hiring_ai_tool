package com.cadence.analyticsservice.export;

import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.dto.response.ReportResponse;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExcelExportGeneratorTest {

    private final ExcelExportGenerator generator = new ExcelExportGenerator();

    @Test
    void generate_shouldProduceParsableXlsWorkbookWithExpectedSheets() throws IOException {
        ReportResponse report = ReportResponse.builder()
                .reportType("YEARLY")
                .periodLabel("2026")
                .generatedAt(LocalDateTime.now())
                .totalApplications(500)
                .totalHires(50)
                .offersSent(80)
                .offersAccepted(50)
                .offersRejected(30)
                .offerAcceptanceRatePercent(62.5)
                .funnel(List.of(FunnelStageResponse.builder().stage("APPLIED").count(500).build()))
                .hiringTrend(List.of())
                .recruiterPerformance(List.of())
                .build();

        byte[] bytes = generator.generate(report);
        assertThat(bytes).isNotEmpty();

        try (HSSFWorkbook workbook = new HSSFWorkbook(new ByteArrayInputStream(bytes))) {
            assertThat(workbook.getNumberOfSheets()).isEqualTo(4);
            assertThat(workbook.getSheet("Summary")).isNotNull();
            assertThat(workbook.getSheet("Funnel")).isNotNull();
            assertThat(workbook.getSheet("Hiring Trend")).isNotNull();
            assertThat(workbook.getSheet("Recruiter Performance")).isNotNull();
            assertThat(workbook.getSheet("Funnel").getRow(1).getCell(0).getStringCellValue()).isEqualTo("APPLIED");
        }
    }
}
