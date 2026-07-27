package com.cadence.companyservice.controller;

import com.cadence.companyservice.dto.request.CreateCompanyRequest;
import com.cadence.companyservice.dto.request.UpdateCompanyRequest;
import com.cadence.companyservice.dto.response.ApiResponse;
import com.cadence.companyservice.dto.response.CompanyResponse;
import com.cadence.companyservice.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/companies")
@RequiredArgsConstructor
@Tag(name = "Companies", description = "Company profile management")
public class CompanyController {

    private final CompanyService companyService;

    @PostMapping
    @Operation(summary = "Create a company", description = "Called during COMPANY_ADMIN registration by Auth Service")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Company created"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Validation failed"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Company name or slug already exists")
    })
    public ResponseEntity<ApiResponse<CompanyResponse>> createCompany(
            @Valid @RequestBody CreateCompanyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        CompanyResponse response = companyService.createCompany(request, actor);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok("Company created", response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get company details")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "OK"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<ApiResponse<CompanyResponse>> getCompany(@PathVariable UUID id) {
        return ResponseEntity.ok(ApiResponse.ok("OK", companyService.getCompany(id)));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Update company details")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company updated"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Company name already exists")
    })
    public ResponseEntity<ApiResponse<CompanyResponse>> updateCompany(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateCompanyRequest request,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        return ResponseEntity.ok(ApiResponse.ok("Company updated", companyService.updateCompany(id, request, actor)));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Soft-delete a company")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Company deleted"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Company not found")
    })
    public ResponseEntity<ApiResponse<Void>> deleteCompany(
            @PathVariable UUID id,
            @RequestHeader(value = "X-User-Id", required = false) String actor) {
        companyService.deleteCompany(id, actor);
        return ResponseEntity.ok(ApiResponse.ok("Company deleted"));
    }
}
