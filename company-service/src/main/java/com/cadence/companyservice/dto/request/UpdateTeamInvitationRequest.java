package com.cadence.companyservice.dto.request;

import com.cadence.companyservice.constant.TeamRole;
import lombok.*;

import java.util.UUID;

/**
 * Mutable fields on a still-PENDING invitation only -- role and which
 * department it's for. Cancelling is a separate action (DELETE), not a
 * status value accepted here.
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTeamInvitationRequest {

    private UUID departmentId;

    private TeamRole role;
}
