package com.cadence.authservice.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Login credentials")
public class LoginRequest {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    private String password;

    @Builder.Default
    @Schema(description = "If true, issues a long-lived refresh token (30 days) instead of the default 7 days")
    private boolean rememberMe = false;
}
