package com.cadence.companyservice.controller;

import com.cadence.companyservice.dto.request.OfficeRequest;
import com.cadence.companyservice.dto.response.ApiResponse;
import com.cadence.companyservice.dto.response.OfficeResponse;
import com.cadence.companyservice.dto.response.PagedResponse;
import com.cadence.companyservice.service.OfficeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequiredArgsConstructor
@Tag(name = "Offices", description = "Office location management within a company")
public class OfficeController {

    private final OfficeService officeService;

    @PostMapping("/api/v1/companies/{companyId}/offices")
    @Operation(summary = "Create an office")
    public ResponseEntity<ApiResponse<OfficeResponse>> createOffice(
            @PathVariable UUID companyId,
            @Valid @RequestBody OfficeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        OfficeResponse response = officeService.createOffice(companyId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Office created", response));
    }

    @GetMapping("/api/v1/companies/{companyId}/offices")
    @Operation(summary = "List offices for a company", description = "Supports pagination and sorting, e.g. ?page=0&size=20&sort=officeName,asc")
    public ResponseEntity<ApiResponse<PagedResponse<OfficeResponse>>> listOffices(
            @PathVariable UUID companyId,
            @PageableDefault(size = 20, sort = "officeName") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", officeService.listOffices(companyId, pageable)));
    }

    @GetMapping("/api/v1/offices/{id}")
    @Operation(summary = "Get an office by id")
    public ResponseEntity<ApiResponse<OfficeResponse>> getOffice(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", officeService.getOffice(id)));
    }

    @PutMapping("/api/v1/offices/{id}")
    @Operation(summary = "Update an office")
    public ResponseEntity<ApiResponse<OfficeResponse>> updateOffice(
            @PathVariable UUID id,
            @Valid @RequestBody OfficeRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        return ResponseEntity.ok(ApiResponse.ok("Office updated", officeService.updateOffice(id, request, actor)));
    }

    @DeleteMapping("/api/v1/offices/{id}")
    @Operation(summary = "Soft-delete an office")
    public ResponseEntity<ApiResponse<Void>> deleteOffice(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        officeService.deleteOffice(id, actor);
        return ResponseEntity.ok(ApiResponse.ok("Office deleted"));
    }
}
