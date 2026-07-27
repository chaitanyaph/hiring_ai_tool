package com.cadence.companyservice.dto.response;

import com.cadence.companyservice.constant.InvitationStatus;
import com.cadence.companyservice.constant.TeamRole;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamInvitationResponse {
    private UUID id;
    private UUID companyId;
    private UUID departmentId;
    private String email;
    private String firstName;
    private String lastName;
    private TeamRole role;
    private String inviteToken;
    private LocalDateTime expiryDate;
    private InvitationStatus status;
    private String createdBy;
    private LocalDateTime acceptedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long version;
}
