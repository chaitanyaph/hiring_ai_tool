package com.cadence.companyservice.controller;

import com.cadence.companyservice.dto.request.DepartmentRequest;
import com.cadence.companyservice.dto.response.ApiResponse;
import com.cadence.companyservice.dto.response.DepartmentResponse;
import com.cadence.companyservice.dto.response.PagedResponse;
import com.cadence.companyservice.service.DepartmentService;
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
@Tag(name = "Departments", description = "Department management within a company")
public class DepartmentController {

    private final DepartmentService departmentService;

    @PostMapping("/api/v1/companies/{companyId}/departments")
    @Operation(summary = "Create a department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> createDepartment(
            @PathVariable UUID companyId,
            @Valid @RequestBody DepartmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        DepartmentResponse response = departmentService.createDepartment(companyId, request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Department created", response));
    }

    @GetMapping("/api/v1/companies/{companyId}/departments")
    @Operation(summary = "List departments for a company", description = "Supports pagination and sorting, e.g. ?page=0&size=20&sort=departmentName,asc")
    public ResponseEntity<ApiResponse<PagedResponse<DepartmentResponse>>> listDepartments(
            @PathVariable UUID companyId,
            @PageableDefault(size = 20, sort = "departmentName") Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok("OK", departmentService.listDepartments(companyId, pageable)));
    }

    @GetMapping("/api/v1/departments/{id}")
    @Operation(summary = "Get a department by id")
    public ResponseEntity<ApiResponse<DepartmentResponse>> getDepartment(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", departmentService.getDepartment(id)));
    }

    @PutMapping("/api/v1/departments/{id}")
    @Operation(summary = "Update a department")
    public ResponseEntity<ApiResponse<DepartmentResponse>> updateDepartment(
            @PathVariable UUID id,
            @Valid @RequestBody DepartmentRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        return ResponseEntity.ok(ApiResponse.ok("Department updated", departmentService.updateDepartment(id, request, actor)));
    }

    @DeleteMapping("/api/v1/departments/{id}")
    @Operation(summary = "Soft-delete a department")
    public ResponseEntity<ApiResponse<Void>> deleteDepartment(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        departmentService.deleteDepartment(id, actor);
        return ResponseEntity.ok(ApiResponse.ok("Department deleted"));
    }
}
