package com.cadence.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VerifyEmailRequest {

    @NotBlank(message = "Verification token is required")
    private String token;
}
