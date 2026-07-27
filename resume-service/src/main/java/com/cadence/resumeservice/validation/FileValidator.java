package com.cadence.resumeservice.validation;

import com.cadence.resumeservice.exception.ErrorCode;
import com.cadence.resumeservice.exception.ResumeValidationException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;

/**
 * Validates by *content*, not just by trusting the client-supplied
 * filename extension or Content-Type header -- either of those can be
 * spoofed, so the real signal is the file's own magic bytes ("%PDF-"
 * at the start of every valid PDF).
 */
@Component
public class FileValidator {

    private static final byte[] PDF_MAGIC_BYTES = {0x25, 0x50, 0x44, 0x46, 0x2D}; // "%PDF-"

    @Value("${app.resume.max-file-size-bytes}")
    private long maxFileSizeBytes;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResumeValidationException(ErrorCode.EMPTY_FILE, "The uploaded file is empty");
        }
        if (file.getSize() > maxFileSizeBytes) {
            throw new ResumeValidationException(ErrorCode.FILE_TOO_LARGE,
                    "Maximum file size is " + (maxFileSizeBytes / (1024 * 1024)) + "MB");
        }
        validateExtension(file.getOriginalFilename());
        validateMimeType(file.getContentType());
        validateMagicBytes(file);
    }

    private void validateExtension(String originalFilename) {
        if (originalFilename == null || !originalFilename.toLowerCase(Locale.ROOT).endsWith(".pdf")) {
            throw new ResumeValidationException(ErrorCode.INVALID_FILE_TYPE, "Only PDF files are supported");
        }
    }

    private void validateMimeType(String contentType) {
        if (contentType == null || !contentType.equalsIgnoreCase("application/pdf")) {
            throw new ResumeValidationException(ErrorCode.INVALID_FILE_TYPE, "Only PDF files are supported");
        }
    }

    private void validateMagicBytes(MultipartFile file) {
        try {
            byte[] header = file.getInputStream().readNBytes(PDF_MAGIC_BYTES.length);
            for (int i = 0; i < PDF_MAGIC_BYTES.length; i++) {
                if (header.length <= i || header[i] != PDF_MAGIC_BYTES[i]) {
                    throw new ResumeValidationException(ErrorCode.INVALID_FILE_TYPE,
                            "The file content does not match a valid PDF");
                }
            }
        } catch (java.io.IOException e) {
            throw new ResumeValidationException(ErrorCode.INVALID_FILE_TYPE, "Could not read the uploaded file");
        }
    }
}
