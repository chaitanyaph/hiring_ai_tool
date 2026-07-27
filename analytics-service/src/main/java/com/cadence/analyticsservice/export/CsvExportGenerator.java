package com.cadence.analyticsservice.export;

import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.dto.response.MonthlyPointResponse;
import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;
import com.cadence.analyticsservice.dto.response.ReportResponse;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

/**
 * No CSV library (commons-csv, opencsv) is cached in this offline build environment --
 * hand-rolled with PrintWriter/StringBuilder, RFC 4180-aware quoting done manually.
 */
@Component
public class CsvExportGenerator {

    private static final String CRLF = "\r\n";

    public byte[] generate(ReportResponse report) {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try (PrintWriter writer = new PrintWriter(out, false, StandardCharsets.UTF_8)) {
            writeRow(writer, "Report Type", "Period", "Generated At");
            writeRow(writer, report.getReportType(), report.getPeriodLabel(), String.valueOf(report.getGeneratedAt()));
            writer.print(CRLF);

            writeRow(writer, "Metric", "Value");
            writeRow(writer, "Total Applications", String.valueOf(report.getTotalApplications()));
            writeRow(writer, "Total Hires", String.valueOf(report.getTotalHires()));
            writeRow(writer, "Offers Sent", String.valueOf(report.getOffersSent()));
            writeRow(writer, "Offers Accepted", String.valueOf(report.getOffersAccepted()));
            writeRow(writer, "Offers Rejected", String.valueOf(report.getOffersRejected()));
            writeRow(writer, "Offer Acceptance Rate %", nvl(report.getOfferAcceptanceRatePercent()));
            writer.print(CRLF);

            writeRow(writer, "Funnel Stage", "Count", "% of First Stage", "Conversion from Previous %");
            for (FunnelStageResponse stage : report.getFunnel()) {
                writeRow(writer, stage.getStage(), String.valueOf(stage.getCount()),
                        nvl(stage.getPercentOfFirstStage()), nvl(stage.getConversionFromPreviousStage()));
            }
            writer.print(CRLF);

            writeRow(writer, "Month", "Hires");
            for (MonthlyPointResponse point : report.getHiringTrend()) {
                writeRow(writer, point.getMonthLabel(), String.valueOf(point.getValue()));
            }
            writer.print(CRLF);

            writeRow(writer, "Recruiter", "Open Reqs", "Applications Reviewed", "Hires",
                    "Avg Time To Hire (days)", "Avg Interview Rating", "Avg Offer Acceptance %");
            for (RecruiterPerformanceResponse recruiter : report.getRecruiterPerformance()) {
                writeRow(writer,
                        nvl(recruiter.getRecruiterName()),
                        String.valueOf(recruiter.getOpenReqs()),
                        String.valueOf(recruiter.getApplicationsReviewed()),
                        String.valueOf(recruiter.getHiresCount()),
                        nvl(recruiter.getAvgTimeToHireDays()),
                        nvl(recruiter.getAvgInterviewRating()),
                        nvl(recruiter.getAvgOfferAcceptancePct()));
            }
        }
        return out.toByteArray();
    }

    private void writeRow(PrintWriter writer, String... fields) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < fields.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(quote(fields[i]));
        }
        writer.print(sb);
        writer.print(CRLF);
    }

    private String quote(String value) {
        if (value == null) {
            return "";
        }
        boolean needsQuoting = value.contains(",") || value.contains("\"") || value.contains("\n") || value.contains("\r");
        String escaped = value.replace("\"", "\"\"");
        return needsQuoting ? "\"" + escaped + "\"" : escaped;
    }

    private String nvl(Object value) {
        return value == null ? "" : String.valueOf(value);
    }
}
