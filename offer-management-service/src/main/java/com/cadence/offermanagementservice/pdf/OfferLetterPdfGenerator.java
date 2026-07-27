package com.cadence.offermanagementservice.pdf;

import com.cadence.offermanagementservice.entity.Offer;
import com.cadence.offermanagementservice.exception.ErrorCode;
import com.cadence.offermanagementservice.exception.OfferManagementServiceException;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Builds the offer letter using OpenPDF 1.3.8 (the only PDF-generation
 * library confirmed cached in this offline build environment -- iText
 * isn't available, PDFBox 3.0.3 is also cached but OpenPDF's table/
 * paragraph API is a more natural fit for a letter document). Company
 * logo is a text placeholder (company name only) -- no image-asset
 * pipeline exists for this service.
 */
@Component
public class OfferLetterPdfGenerator {

    private static final String TERMS_TEXT =
            "This offer is contingent upon successful completion of any background verification checks. "
            + "The compensation and benefits described above are subject to the company's standard policies "
            + "as may be amended from time to time. This letter, once accepted, along with the company's "
            + "employee handbook, constitutes the full terms of your employment.";

    public byte[] generate(Offer offer, String companyName) {
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            PdfWriter.getInstance(document, out);
            document.open();

            Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
            Font headingFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
            Font normalFont = FontFactory.getFont(FontFactory.HELVETICA, 10);
            Font smallFont = FontFactory.getFont(FontFactory.HELVETICA, 8);

            document.add(new Paragraph(nvl(companyName, "Company"), titleFont));
            document.add(spacer());
            document.add(new Paragraph("Offer Letter", headingFont));
            document.add(new Paragraph("Offer Number: " + offer.getId(), normalFont));
            document.add(new Paragraph("Date: " + LocalDate.now(), normalFont));
            document.add(spacer());

            document.add(new Paragraph("Dear " + nvl(offer.getCandidateName(), "Candidate") + ",", normalFont));
            document.add(spacer());
            document.add(new Paragraph(
                    "We are pleased to offer you the position of " + nvl(offer.getJobTitle(), "the role")
                            + (offer.getDepartment() != null ? " in the " + offer.getDepartment() + " department" : "")
                            + ", with a proposed start date of " + offer.getStartDate() + ".",
                    normalFont));
            document.add(spacer());

            document.add(new Paragraph("Compensation Details", headingFont));
            document.add(compensationTable(offer, normalFont));
            document.add(spacer());

            if (offer.getBenefits() != null && !offer.getBenefits().isBlank()) {
                document.add(new Paragraph("Benefits: " + offer.getBenefits(), normalFont));
                document.add(spacer());
            }

            document.add(new Paragraph("Terms & Conditions", headingFont));
            document.add(new Paragraph(TERMS_TEXT, smallFont));
            document.add(spacer());

            if (offer.getExpiryDate() != null) {
                document.add(new Paragraph("This offer is valid until " + offer.getExpiryDate() + ".", normalFont));
                document.add(spacer());
            }

            document.add(spacer());
            document.add(new Paragraph("_______________________", normalFont));
            document.add(new Paragraph("HR Signature, " + nvl(companyName, "Company"), smallFont));

            document.close();
            return out.toByteArray();
        } catch (DocumentException e) {
            throw new OfferManagementServiceException(ErrorCode.INTERNAL_ERROR, "Failed to generate offer letter PDF", HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    private PdfPTable compensationTable(Offer offer, Font font) {
        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        addRow(table, "Base Salary", formatCurrency(offer.getBaseSalary()), font);
        addRow(table, "Variable / Bonus", formatCurrency(offer.getVariableBonus()), font);
        addRow(table, "ESOP / Equity", formatCurrency(offer.getEsopEquity()), font);
        addRow(table, "Total CTC", formatCurrency(offer.getTotalCtc()), font);
        return table;
    }

    private void addRow(PdfPTable table, String label, String value, Font font) {
        table.addCell(new PdfPCell(new Phrase(label, font)));
        table.addCell(new PdfPCell(new Phrase(value, font)));
    }

    private String formatCurrency(BigDecimal amount) {
        return amount == null ? "-" : "₹ " + amount.stripTrailingZeros().toPlainString() + " LPA";
    }

    private Paragraph spacer() {
        return new Paragraph(" ");
    }

    private String nvl(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
