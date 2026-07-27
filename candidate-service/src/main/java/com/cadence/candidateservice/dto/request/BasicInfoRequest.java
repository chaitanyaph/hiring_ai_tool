package com.cadence.candidateservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

/** Wizard Step 1. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BasicInfoRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150)
    private String fullName;

    @Size(max = 150)
    private String headline;

    @Size(max = 20)
    private String phone;

    @Size(max = 200)
    private String location;

    @Size(max = 150)
    private String currentCompany;

    private Integer noticePeriodDays;
}
