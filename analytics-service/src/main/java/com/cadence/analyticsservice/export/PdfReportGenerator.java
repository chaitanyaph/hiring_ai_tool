package com.cadence.analyticsservice.export;

import com.cadence.analyticsservice.dto.response.FunnelStageResponse;
import com.cadence.analyticsservice.dto.response.MonthlyPointResponse;
import com.cadence.analyticsservice.dto.response.RecruiterPerformanceResponse;
import com.cadence.analyticsservice.dto.response.ReportResponse;
import com.cadence.analyticsservice.exception.ReportGenerationException;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.List;

/** Mirrors OfferLetterPdfGenerator's OpenPDF table/paragraph pattern -- the only PDF library confirmed cached offline. */
@Component
public class PdfReportGenerator {

    public byte[] generate(ReportResponse report) {
        Document document = new Document(PageSize.A4, 40, 40, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

            document.add(new Paragraph("Cadence Analytics Report", titleFont));
            document.add(new Paragraph(report.getReportType() + " -- " + report.getPeriodLabel(), headingFont));
            document.add(new Paragraph("Generated at: " + report.getGeneratedAt(), normalFont));
            document.add(spacer());

            document.add(new Paragraph("Summary", headingFont));
            document.add(summaryTable(report, normalFont));
            document.add(spacer());

            if (!report.getFunnel().isEmpty()) {
                document.add(new Paragraph("Funnel", headingFont));
                document.add(funnelTable(report.getFunnel(), normalFont));
                document.add(spacer());
            }

            if (!report.getHiringTrend().isEmpty()) {
                document.add(new Paragraph("Hiring Trend", headingFont));
                document.add(hiringTrendTable(report.getHiringTrend(), normalFont));
                document.add(spacer());
            }

            if (!report.getRecruiterPerformance().isEmpty()) {
                document.add(new Paragraph("Recruiter Performance", headingFont));
                document.add(recruiterTable(report.getRecruiterPerformance(), normalFont));
            }

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new ReportGenerationException("Failed to generate PDF report", e);
        }
    }

    private PdfPTable summaryTable(ReportResponse report, Font font) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addRow(table, font, "Total Applications", String.valueOf(report.getTotalApplications()));
        addRow(table, font, "Total Hires", String.valueOf(report.getTotalHires()));
        addRow(table, font, "Offers Sent", String.valueOf(report.getOffersSent()));
        addRow(table, font, "Offers Accepted", String.valueOf(report.getOffersAccepted()));
        addRow(table, font, "Offers Rejected", String.valueOf(report.getOffersRejected()));
        addRow(table, font, "Offer Acceptance Rate %", nvl(report.getOfferAcceptanceRatePercent()));
        return table;
    }

    private PdfPTable funnelTable(List<FunnelStageResponse> stages, Font font) {
        PdfPTable table = new PdfPTable(4);
        table.setWidthPercentage(100);
        addRow(table, font, "Stage", "Count", "% of First", "Conversion %");
        for (FunnelStageResponse stage : stages) {
            addRow(table, font, stage.getStage(), String.valueOf(stage.getCount()),
                    nvl(stage.getPercentOfFirstStage()), nvl(stage.getConversionFromPreviousStage()));
        }
        return table;
    }

    private PdfPTable hiringTrendTable(List<MonthlyPointResponse> points, Font font) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addRow(table, font, "Month", "Hires");
        for (MonthlyPointResponse point : points) {
            addRow(table, font, point.getMonthLabel(), String.valueOf(point.getValue()));
        }
        return table;
    }

    private PdfPTable recruiterTable(List<RecruiterPerformanceResponse> recruiters, Font font) {
        PdfPTable table = new PdfPTable(5);
        table.setWidthPercentage(100);
        addRow(table, font, "Recruiter", "Open Reqs", "Applications Reviewed", "Hires", "Avg Offer Acceptance %");
        for (RecruiterPerformanceResponse recruiter : recruiters) {
            addRow(table, font, nvl(recruiter.getRecruiterName()), String.valueOf(recruiter.getOpenReqs()),
                    String.valueOf(recruiter.getApplicationsReviewed()), String.valueOf(recruiter.getHiresCount()),
                    nvl(recruiter.getAvgOfferAcceptancePct()));
        }
        return table;
    }

    private void addRow(PdfPTable table, Font font, String... values) {
        for (String value : values) {
            table.addCell(new PdfPCell(new Phrase(value, font)));
        }
    }

    private Paragraph spacer() {
        return new Paragraph(" ");
    }

    private String nvl(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
