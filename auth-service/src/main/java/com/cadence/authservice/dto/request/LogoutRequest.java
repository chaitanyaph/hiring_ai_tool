package com.cadence.authservice.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LogoutRequest {

    @NotBlank(message = "Refresh token is required to invalidate the session")
    private String refreshToken;

    @Builder.Default
    private boolean allDevices = false;
}
