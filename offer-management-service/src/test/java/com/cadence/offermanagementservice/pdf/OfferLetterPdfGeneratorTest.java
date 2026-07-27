package com.cadence.offermanagementservice.pdf;

import com.cadence.offermanagementservice.constants.EmploymentType;
import com.cadence.offermanagementservice.entity.Offer;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class OfferLetterPdfGeneratorTest {

    private final OfferLetterPdfGenerator generator = new OfferLetterPdfGenerator();

    @Test
    void generate_shouldProduceNonEmptyPdfBytes() {
        Offer offer = Offer.builder()
                .id(UUID.randomUUID())
                .candidateName("Rohan Mehta")
                .jobTitle("Senior DevOps Engineer")
                .department("Infrastructure")
                .employmentType(EmploymentType.FULL_TIME)
                .startDate(LocalDate.now().plusMonths(1))
                .baseSalary(new BigDecimal("18"))
                .variableBonus(new BigDecimal("2"))
                .esopEquity(BigDecimal.ZERO)
                .totalCtc(new BigDecimal("20"))
                .benefits("Health insurance,Provident fund")
                .expiryDate(LocalDate.now().plusDays(14))
                .build();

        byte[] pdf = generator.generate(offer, "Acme Corp");

        assertThat(pdf).isNotEmpty();
        // Every valid PDF file starts with the "%PDF-" magic bytes.
        assertThat(new String(pdf, 0, 5, java.nio.charset.StandardCharsets.US_ASCII)).isEqualTo("%PDF-");
    }
}
