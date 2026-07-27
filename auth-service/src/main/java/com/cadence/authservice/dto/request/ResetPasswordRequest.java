package com.cadence.authservice.dto.request;

import com.cadence.authservice.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ResetPasswordRequest {

    @NotBlank(message = "Reset token is required")
    private String token;

    @NotBlank
    @StrongPassword
    private String newPassword;
}
