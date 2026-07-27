package com.cadence.resumeparserservice.validation;

import com.cadence.resumeparserservice.exception.ResumeParsingPipelineException;
import com.cadence.resumeparserservice.provider.ParsedResumeData;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ParsedDataValidatorTest {

    private final ParsedDataValidator validator = new ParsedDataValidator();

    private ParsedResumeData dataWith(String name, String email, String phone) {
        return new ParsedResumeData(name, email, phone, null, null, null, null, null, null, null,
                null, null, null, List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }

    @Test
    void validate_shouldThrow_whenNameIsMissing() {
        ParsedResumeData data = dataWith(null, "a@b.com", null);
        assertThatThrownBy(() -> validator.validate(data)).isInstanceOf(ResumeParsingPipelineException.class);
    }

    @Test
    void validate_shouldThrow_whenNeitherEmailNorPhonePresent() {
        ParsedResumeData data = dataWith("Jane Doe", null, null);
        assertThatThrownBy(() -> validator.validate(data)).isInstanceOf(ResumeParsingPipelineException.class);
    }

    @Test
    void validate_shouldPass_whenNameAndEmailPresent() {
        ParsedResumeData data = dataWith("Jane Doe", "jane@doe.com", null);
        assertThatCode(() -> validator.validate(data)).doesNotThrowAnyException();
    }

    @Test
    void validate_shouldPass_whenNameAndPhonePresent() {
        ParsedResumeData data = dataWith("Jane Doe", null, "+1234567890");
        assertThatCode(() -> validator.validate(data)).doesNotThrowAnyException();
    }
}
