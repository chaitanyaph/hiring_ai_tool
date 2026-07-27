package com.cadence.authservice.service;

import com.cadence.authservice.dto.response.RoleResponse;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface RoleService {
    List<RoleResponse> getAllRoles();
    void assignRoles(UUID userId, Set<String> roleNames, UUID actorUserId);
    void revokeRoles(UUID userId, Set<String> roleNames, UUID actorUserId);
}
