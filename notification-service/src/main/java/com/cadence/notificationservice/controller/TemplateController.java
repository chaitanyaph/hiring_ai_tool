package com.cadence.notificationservice.controller;

import com.cadence.notificationservice.dto.request.CreateTemplateRequest;
import com.cadence.notificationservice.dto.request.UpdateTemplateRequest;
import com.cadence.notificationservice.dto.response.ApiResponse;
import com.cadence.notificationservice.dto.response.TemplatePreviewResponse;
import com.cadence.notificationservice.dto.response.TemplateResponse;
import com.cadence.notificationservice.service.NotificationTemplateService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/** Backs the Figma's "Email templates" admin tab (§A3). */
@RestController
@RequestMapping("/api/v1/templates")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Notification Templates", description = "Email template CRUD + preview")
public class TemplateController {

    private final NotificationTemplateService templateService;

    @PostMapping
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Create an email template")
    public ResponseEntity<ApiResponse<TemplateResponse>> create(@Valid @RequestBody CreateTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Template created", templateService.createTemplate(request)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Update an email template")
    public ResponseEntity<ApiResponse<TemplateResponse>> update(@PathVariable UUID id, @Valid @RequestBody UpdateTemplateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok("Template updated", templateService.updateTemplate(id, request)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','ADMIN')")
    @Operation(summary = "Delete an email template")
    public ResponseEntity<ApiResponse<Void>> delete(@PathVariable UUID id) {
        templateService.deleteTemplate(id);
        return ResponseEntity.ok(ApiResponse.ok("Template deleted"));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get an email template")
    public ResponseEntity<ApiResponse<TemplateResponse>> get(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", templateService.getTemplate(id)));
    }

    @GetMapping
    @Operation(summary = "List email templates")
    public ResponseEntity<ApiResponse<List<TemplateResponse>>> list() {
        return ResponseEntity.ok(ApiResponse.ok("OK", templateService.listTemplates()));
    }

    @GetMapping("/{id}/preview")
    @Operation(summary = "Preview a template rendered with sample data")
    public ResponseEntity<ApiResponse<TemplatePreviewResponse>> preview(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", templateService.previewTemplate(id)));
    }
}
