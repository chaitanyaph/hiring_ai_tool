package com.cadence.authservice.dto.request;

import com.cadence.authservice.constant.UserType;
import com.cadence.authservice.validation.StrongPassword;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "Payload to register a new platform user (candidate, recruiter, or company admin)")
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 150, message = "Full name must not exceed 150 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 180)
    private String email;

    @NotBlank(message = "Password is required")
    @StrongPassword
    private String password;

    private String phoneNumber;

    @NotNull(message = "User type is required")
    private UserType userType;

    @Schema(description = "Required when registering a RECRUITER/COMPANY_ADMIN who belongs to an existing company")
    private UUID companyId;

    @Size(max = 150, message = "Company name must not exceed 150 characters")
    @Schema(description = "Required when userType is COMPANY_ADMIN -- creates a new company workspace and links this user as its admin")
    private String companyName;
}
