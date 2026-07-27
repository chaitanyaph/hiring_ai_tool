package com.cadence.notificationservice.controller;

import com.cadence.notificationservice.dto.response.ApiResponse;
import com.cadence.notificationservice.dto.response.NotificationLogResponse;
import com.cadence.notificationservice.dto.response.PagedResponse;
import com.cadence.notificationservice.service.NotificationLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Backs the Figma's "Notification logs" tab (§A3) -- raw log line viewer. Added beyond the literal API list, justified by this exact Figma tab. */
@RestController
@RequestMapping("/api/v1/notification-logs")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Notification Logs", description = "Raw event/dispatch log lines")
public class NotificationLogController {

    private final NotificationLogService notificationLogService;

    @GetMapping
    @Operation(summary = "List notification logs, optionally filtered by source service")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationLogResponse>>> list(
            @RequestParam(required = false) String source, @PageableDefault(size = 30) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", notificationLogService.listLogs(source, pageable)));
    }
}
