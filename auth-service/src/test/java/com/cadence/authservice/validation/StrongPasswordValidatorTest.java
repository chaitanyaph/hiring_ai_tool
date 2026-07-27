package com.cadence.authservice.validation;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.assertj.core.api.Assertions.assertThat;

class StrongPasswordValidatorTest {

    private final StrongPasswordValidator validator = new StrongPasswordValidator();

    @ParameterizedTest
    @CsvSource({
            "Str0ng!Pass, true",
            "short1!, false",
            "alllowercase1!, false",
            "ALLUPPERCASE1!, false",
            "NoDigitsHere!, false",
            "NoSpecialChar1, false",
            "Valid#Pass9, true"
    })
    void validatesPasswordStrength(String password, boolean expectedValid) {
        assertThat(validator.isValid(password, null)).isEqualTo(expectedValid);
    }

    @Test
    void shouldRejectNullPassword() {
        assertThat(validator.isValid(null, null)).isFalse();
    }
}
