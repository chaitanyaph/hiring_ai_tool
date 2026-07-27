package com.cadence.companyservice.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OfficeRequest {

    @NotBlank(message = "Office name is required")
    @Size(max = 120, message = "Office name must not exceed 120 characters")
    private String officeName;

    @Size(max = 100)
    private String country;

    @Size(max = 100)
    private String state;

    @Size(max = 100)
    private String city;

    @Size(max = 255)
    private String address;

    @Size(max = 20)
    private String postalCode;

    @Size(max = 60)
    private String timezone;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private BigDecimal latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private BigDecimal longitude;

    @Builder.Default
    private boolean isPrimaryOffice = false;
}
