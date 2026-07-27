package com.cadence.authservice.controller;

import com.cadence.authservice.dto.response.ApiResponse;
import com.cadence.authservice.dto.response.AuditLogResponse;
import com.cadence.authservice.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/auth/audit-logs")
@RequiredArgsConstructor
@Tag(name = "Audit Logs", description = "Security event history for the current user")
public class AuditLogController {

    private final AuditLogService auditLogService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Paginated audit log history for the current user")
    public ResponseEntity<ApiResponse<Page<AuditLogResponse>>> getMyAuditLogs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            Authentication authentication) {
        UUID userId = UUID.fromString(authentication.getName());
        Page<AuditLogResponse> logs = auditLogService.getLogsForUser(userId, PageRequest.of(page, size));
        return ResponseEntity.ok(ApiResponse.ok("OK", logs));
    }
}
