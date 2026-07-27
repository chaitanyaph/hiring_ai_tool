package com.cadence.companyservice.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompanyResponse {
    private UUID id;
    private String companyName;
    private String companySlug;
    private String industry;
    private String website;
    private String companyEmail;
    private String companyPhone;
    private String headquarters;
    private String description;
    private String companyLogo;
    private String subscriptionPlan;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
