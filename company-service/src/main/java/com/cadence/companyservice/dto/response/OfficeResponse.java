package com.cadence.companyservice.dto.response;

import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficeResponse {
    private UUID id;
    private UUID companyId;
    private String officeName;
    private String country;
    private String state;
    private String city;
    private String address;
    private String postalCode;
    private String timezone;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private boolean isPrimaryOffice;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    private Long version;
}
