package com.cadence.resumeparserservice.validation;

import com.cadence.resumeparserservice.exception.ResumeParsingPipelineException;
import com.cadence.resumeparserservice.provider.ParsedResumeData;
import org.springframework.stereotype.Component;

/**
 * A minimal structural sanity check on whatever the LLM returned --
 * this is not re-validating business rules (that already happened in
 * Resume Service at upload time), just confirming the model actually
 * extracted a usable resume rather than hallucinating an empty shell.
 */
@Component
public class ParsedDataValidator {

    public void validate(ParsedResumeData data) {
        if (data.fullName() == null || data.fullName().isBlank()) {
            throw new ResumeParsingPipelineException("Parsed data is missing a candidate name");
        }
        boolean hasEmail = data.email() != null && !data.email().isBlank();
        boolean hasPhone = data.phone() != null && !data.phone().isBlank();
        if (!hasEmail && !hasPhone) {
            throw new ResumeParsingPipelineException("Parsed data has neither an email nor a phone number");
        }
    }
}
