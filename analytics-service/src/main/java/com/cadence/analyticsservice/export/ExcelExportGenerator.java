package com.cadence.analyticsservice.export;

import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.dto.response.MonthlyPointResponse;
import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;
import com.cadence.analyticsservice.dto.response.ReportResponse;
import com.cadence.analyticsservice.exception.ReportGenerationException;
import org.apache.poi.hssf.usermodel.HSSFWorkbook;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

/**
 * poi-ooxml (true .xlsx) is not cached in this offline build environment -- only base
 * poi (legacy HSSF binary format) is available. Produces a real .xls file via
 * HSSFWorkbook, not a mislabeled .xlsx, flagged in README.
 */
@Component
public class ExcelExportGenerator {

    public byte[] generate(ReportResponse report) {
        try (HSSFWorkbook workbook = new HSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            CellStyle headerStyle = headerStyle(workbook);

            Sheet summarySheet = workbook.createSheet("Summary");
            int r = writeRow(summarySheet, 0, headerStyle, "Report Type", "Period", "Generated At");
            r = writeRow(summarySheet, r, null, report.getReportType(), report.getPeriodLabel(), String.valueOf(report.getGeneratedAt()));
            r++;
            r = writeRow(summarySheet, r, headerStyle, "Metric", "Value");
            r = writeRow(summarySheet, r, null, "Total Applications", String.valueOf(report.getTotalApplications()));
            r = writeRow(summarySheet, r, null, "Total Hires", String.valueOf(report.getTotalHires()));
            r = writeRow(summarySheet, r, null, "Offers Sent", String.valueOf(report.getOffersSent()));
            r = writeRow(summarySheet, r, null, "Offers Accepted", String.valueOf(report.getOffersAccepted()));
            r = writeRow(summarySheet, r, null, "Offers Rejected", String.valueOf(report.getOffersRejected()));
            writeRow(summarySheet, r, null, "Offer Acceptance Rate %", nvl(report.getOfferAcceptanceRatePercent()));
            autoSizeColumns(summarySheet, 3);

            Sheet funnelSheet = workbook.createSheet("Funnel");
            r = writeRow(funnelSheet, 0, headerStyle, "Stage", "Count", "% of First Stage", "Conversion from Previous %");
            for (FunnelStageResponse stage : report.getFunnel()) {
                r = writeRow(funnelSheet, r, null, stage.getStage(), String.valueOf(stage.getCount()),
                        nvl(stage.getPercentOfFirstStage()), nvl(stage.getConversionFromPreviousStage()));
            }
            autoSizeColumns(funnelSheet, 4);

            Sheet trendSheet = workbook.createSheet("Hiring Trend");
            r = writeRow(trendSheet, 0, headerStyle, "Month", "Hires");
            for (MonthlyPointResponse point : report.getHiringTrend()) {
                r = writeRow(trendSheet, r, null, point.getMonthLabel(), String.valueOf(point.getValue()));
            }
            autoSizeColumns(trendSheet, 2);

            Sheet recruiterSheet = workbook.createSheet("Recruiter Performance");
            r = writeRow(recruiterSheet, 0, headerStyle, "Recruiter", "Open Reqs", "Applications Reviewed",
                    "Hires", "Avg Time To Hire (days)", "Avg Interview Rating", "Avg Offer Acceptance %");
            for (RecruiterPerformanceResponse recruiter : report.getRecruiterPerformance()) {
                r = writeRow(recruiterSheet, r, null,
                        nvl(recruiter.getRecruiterName()),
                        String.valueOf(recruiter.getOpenReqs()),
                        String.valueOf(recruiter.getApplicationsReviewed()),
                        String.valueOf(recruiter.getHiresCount()),
                        nvl(recruiter.getAvgTimeToHireDays()),
                        nvl(recruiter.getAvgInterviewRating()),
                        nvl(recruiter.getAvgOfferAcceptancePct()));
            }
            autoSizeColumns(recruiterSheet, 7);

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new ReportGenerationException("Failed to generate Excel report", e);
        }
    }

    private int writeRow(Sheet sheet, int rowIndex, CellStyle style, String... values) {
        Row row = sheet.createRow(rowIndex);
        for (int i = 0; i < values.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(nvl(values[i]));
            if (style != null) {
                cell.setCellStyle(style);
            }
        }
        return rowIndex + 1;
    }

    private void autoSizeColumns(Sheet sheet, int columnCount) {
        for (int i = 0; i < columnCount; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private CellStyle headerStyle(Workbook workbook) {
        Font boldFont = workbook.createFont();
        boldFont.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(boldFont);
        return style;
    }

    private String nvl(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
