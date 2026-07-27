package com.cadence.authservice.dto.response;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MfaSetupResponse {
    private String secret;
    private String qrCodeImageBase64;
    private String otpAuthUrl;
    private List<String> recoveryCodes;
}
