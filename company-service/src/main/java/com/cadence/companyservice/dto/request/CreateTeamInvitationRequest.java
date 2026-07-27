package com.cadence.companyservice.dto.request;

import com.cadence.companyservice.constant.TeamRole;
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
public class CreateTeamInvitationRequest {

    private UUID departmentId;

    @NotBlank(message = "Email is required")
    @Email(message = "Email must be a valid email address")
    @Size(max = 180)
    private String email;

    @Size(max = 100)
    private String firstName;

    @Size(max = 100)
    private String lastName;

    @NotNull(message = "Role is required")
    private TeamRole role;
}
