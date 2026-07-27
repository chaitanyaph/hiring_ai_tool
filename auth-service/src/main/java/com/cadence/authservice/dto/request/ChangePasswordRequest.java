package com.cadence.authservice.dto.request;

import com.cadence.authservice.validation.StrongPassword;
import jakarta.validation.constraints.NotBlank;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ChangePasswordRequest {

    @NotBlank
    private String currentPassword;

    @NotBlank
    @StrongPassword
    private String newPassword;
}
