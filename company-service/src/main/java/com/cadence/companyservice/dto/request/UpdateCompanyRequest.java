package com.cadence.companyservice.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import org.hibernate.validator.constraints.URL;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateCompanyRequest {

    @NotBlank(message = "Company name is required")
    @Size(max = 150, message = "Company name must not exceed 150 characters")
    private String companyName;

    @Size(max = 120)
    private String industry;

    @URL(message = "Website must be a valid URL")
    @Size(max = 255)
    private String website;

    @Email(message = "Company email must be a valid email address")
    @Size(max = 180)
    private String companyEmail;

    @Pattern(regexp = "^\\+?[0-9\\-() ]{7,30}$", message = "Company phone must be a valid phone number")
    private String companyPhone;

    @Size(max = 200)
    private String headquarters;

    private String description;

    @Size(max = 500)
    private String companyLogo;

    @Size(max = 60)
    private String subscriptionPlan;
}
