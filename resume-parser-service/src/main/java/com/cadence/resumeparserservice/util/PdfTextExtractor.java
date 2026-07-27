package com.cadence.resumeparserservice.util;

import com.cadence.resumeparserservice.exception.ResumeParsingPipelineException;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
public class PdfTextExtractor {

    public String extractText(byte[] pdfBytes) {
        try (PDDocument document = Loader.loadPDF(pdfBytes)) {
            String text = new PDFTextStripper().getText(document);
            if (text == null || text.isBlank()) {
                throw new ResumeParsingPipelineException(
                        "No selectable text layer found in the PDF -- it may be a scanned image without OCR-ready text.");
            }
            return text;
        } catch (IOException e) {
            log.error("Failed to extract text from PDF: {}", e.getMessage(), e);
            throw new ResumeParsingPipelineException("Failed to extract text from PDF: " + e.getMessage(), e);
        }
    }
}
