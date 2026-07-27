package com.cadence.authservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

/** Used both to confirm MFA setup and to challenge MFA during a login flow. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaVerifyRequest {

    @Schema(description = "Short-lived session token issued after password verification, identifying the pending login awaiting MFA")
    private String mfaSessionToken;

    @NotBlank
    @Pattern(regexp = "\\d{6}", message = "OTP code must be exactly 6 digits")
    private String code;
}
