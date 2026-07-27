package com.cadence.notificationservice.controller;

import com.cadence.notificationservice.constants.NotificationStatus;
import com.cadence.notificationservice.dto.request.UpdatePreferencesRequest;
import com.cadence.notificationservice.dto.response.*;
import com.cadence.notificationservice.security.CurrentUserProvider;
import com.cadence.notificationservice.service.NotificationPreferenceService;
import com.cadence.notificationservice.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * Personal in-app notification CRUD, generic across any authenticated
 * role (§5/§10 of the architecture doc). Backs the candidate dashboard's
 * #csec-notifications list today; the recruiter-side bell is currently
 * wired to a fixed toast in the Figma, not this API -- see README.
 */
@RestController
@RequestMapping("/api/v1/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Personal in-app notifications, history, unread count, and preferences")
public class NotificationController {

    private final NotificationService notificationService;
    private final NotificationPreferenceService preferenceService;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping
    @Operation(summary = "List my active (non-archived) notifications")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> list(
            @RequestParam(required = false) NotificationStatus status, @PageableDefault(size = 20) Pageable pageable) {
        var recipientId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", notificationService.listNotifications(recipientId, status, pageable)));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get a single notification")
    public ResponseEntity<ApiResponse<NotificationResponse>> get(@PathVariable UUID id) {
        var recipientId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", notificationService.getNotification(recipientId, id)));
    }

    @PutMapping("/{id}/read")
    @Operation(summary = "Mark a notification as read")
    public ResponseEntity<ApiResponse<Void>> markRead(@PathVariable UUID id) {
        var recipientId = currentUserProvider.getCurrentUser().getUserId();
        notificationService.markRead(recipientId, id);
        return ResponseEntity.ok(ApiResponse.ok("Marked as read"));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Mark all my notifications as read")
    public ResponseEntity<ApiResponse<Void>> markAllRead() {
        var recipientId = currentUserProvider.getCurrentUser().getUserId();
        notificationService.markAllRead(recipientId);
        return ResponseEntity.ok(ApiResponse.ok("All notifications marked as read"));
    }

    @PutMapping("/{id}/archive")
    @Operation(summary = "Archive a notification")
    public ResponseEntity<ApiResponse<Void>> archive(@PathVariable UUID id) {
        var recipientId = currentUserProvider.getCurrentUser().getUserId();
        notificationService.archive(recipientId, id);
        return ResponseEntity.ok(ApiResponse.ok("Notification archived"));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete a notification")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        var recipientId = currentUserProvider.getCurrentUser().getUserId();
        notificationService.delete(recipientId, id);
        return ResponseEntity.ok(ApiResponse.ok("Notification deleted"));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Get my unread notification count")
    public ResponseEntity<ApiResponse<Long>> unreadCount() {
        var recipientId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", notificationService.getUnreadCount(recipientId)));
    }

    @GetMapping("/history")
    @Operation(summary = "Get my full notification history (including read/archived)")
    public ResponseEntity<ApiResponse<PagedResponse<NotificationResponse>>> history(@PageableDefault(size = 20) Pageable pageable) {
        var recipientId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", notificationService.listNotifications(recipientId, null, pageable)));
    }

    @GetMapping("/preferences")
    @Operation(summary = "Get my notification preferences")
    public ResponseEntity<ApiResponse<List<PreferenceResponse>>> getPreferences() {
        var userId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("OK", preferenceService.getPreferences(userId)));
    }

    @PutMapping("/preferences")
    @Operation(summary = "Update my notification preferences")
    public ResponseEntity<ApiResponse<List<PreferenceResponse>>> updatePreferences(@Valid @RequestBody UpdatePreferencesRequest request) {
        var userId = currentUserProvider.getCurrentUser().getUserId();
        return ResponseEntity.ok(ApiResponse.ok("Preferences updated", preferenceService.updatePreferences(userId, request)));
    }
}
