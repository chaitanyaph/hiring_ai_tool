package com.cadence.authservice.controller;

import com.cadence.authservice.dto.request.AssignRoleRequest;
import com.cadence.authservice.dto.response.ApiResponse;
import com.cadence.authservice.dto.response.RoleResponse;
import com.cadence.authservice.service.RoleService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * RBAC administration. Restricted to ROLE_ADMIN / ROLE_COMPANY_ADMIN via
 * method-level @PreAuthorize -- kept separate from AuthController since
 * this is an authorization-management concern, not an identity concern.
 */
@RestController
@RequestMapping("/api/v1/auth/roles")
@RequiredArgsConstructor
@Tag(name = "Role & Permission Management", description = "RBAC administration")
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN')")
    @Operation(summary = "List all roles and their permissions")
    public ResponseEntity<ApiResponse<List<RoleResponse>>> getAllRoles() {
        return ResponseEntity.ok(ApiResponse.ok("OK", roleService.getAllRoles()));
    }

    @PostMapping("/{userId}/assign")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN')")
    @Operation(summary = "Assign one or more roles to a user")
    public ResponseEntity<ApiResponse<Void>> assignRoles(@PathVariable UUID userId,
                                                          @Valid @RequestBody AssignRoleRequest request,
                                                          Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        roleService.assignRoles(userId, request.getRoleNames(), actorId);
        return ResponseEntity.ok(ApiResponse.ok("Roles assigned successfully"));
    }

    @PostMapping("/{userId}/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','COMPANY_ADMIN')")
    @Operation(summary = "Revoke one or more roles from a user")
    public ResponseEntity<ApiResponse<Void>> revokeRoles(@PathVariable UUID userId,
                                                          @Valid @RequestBody AssignRoleRequest request,
                                                          Authentication authentication) {
        UUID actorId = UUID.fromString(authentication.getName());
        roleService.revokeRoles(userId, request.getRoleNames(), actorId);
        return ResponseEntity.ok(ApiResponse.ok("Roles revoked successfully"));
    }
}
