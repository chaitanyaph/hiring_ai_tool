package com.cadence.analyticsservice.controller;

import com.cadence.analyticsservice.constants.ReportFormat;
import com.cadence.analyticsservice.constants.ReportPeriod;
import com.cadence.analyticsservice.dto.response.ApiResponse;
import com.cadence.analyticsservice.dto.response.ReportResponse;
import com.cadence.analyticsservice.entity.ReportExportLog;
import com.cadence.analyticsservice.export.CsvExportGenerator;
import com.cadence.analyticsservice.export.ExcelExportGenerator;
import com.cadence.analyticsservice.export.PdfReportGenerator;
import com.cadence.analyticsservice.repository.ReportExportLogRepository;
import com.cadence.analyticsservice.security.CurrentUser;
import com.cadence.analyticsservice.security.CurrentUserProvider;
import com.cadence.analyticsservice.service.ReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

/** Backs /reports/daily, /monthly, /yearly plus the CSV/Excel/PDF export endpoints (§13). */
@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('COMPANY_ADMIN','HR_MANAGER','HR_RECRUITER','TECHNICAL_RECRUITER','TALENT_ACQUISITION_MANAGER','HIRING_MANAGER','ADMIN')")
@Tag(name = "Reports", description = "Daily/monthly/yearly reports and CSV/Excel/PDF exports")
public class ReportController {

    private final ReportService reportService;
    private final CsvExportGenerator csvExportGenerator;
    private final ExcelExportGenerator excelExportGenerator;
    private final PdfReportGenerator pdfReportGenerator;
    private final ReportExportLogRepository reportExportLogRepository;
    private final CurrentUserProvider currentUserProvider;

    @GetMapping("/daily")
    @Operation(summary = "Daily report for the caller's company (defaults to today)")
    public ResponseEntity<ApiResponse<ReportResponse>> daily(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        LocalDate reportDate = date != null ? date : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok("OK", reportService.getDailyReport(companyId, reportDate)));
    }

    @GetMapping("/monthly")
    @Operation(summary = "Monthly report for the caller's company (defaults to the current month)")
    public ResponseEntity<ApiResponse<ReportResponse>> monthly(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate month) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        LocalDate reportMonth = month != null ? month : LocalDate.now();
        return ResponseEntity.ok(ApiResponse.ok("OK", reportService.getMonthlyReport(companyId, reportMonth)));
    }

    @GetMapping("/yearly")
    @Operation(summary = "Yearly report for the caller's company (defaults to the current year)")
    public ResponseEntity<ApiResponse<ReportResponse>> yearly(@RequestParam(required = false) Integer year) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        int reportYear = year != null ? year : LocalDate.now().getYear();
        return ResponseEntity.ok(ApiResponse.ok("OK", reportService.getYearlyReport(companyId, reportYear)));
    }

    @GetMapping("/export/csv")
    @Operation(summary = "Export a report as CSV")
    public ResponseEntity<byte[]> exportCsv(
            @RequestParam ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year) {
        ReportResponse report = resolveReport(period, date, year);
        byte[] bytes = csvExportGenerator.generate(report);
        logExport(period, ReportFormat.CSV, bytes.length);
        return fileResponse(bytes, "analytics-report.csv", MediaType.parseMediaType("text/csv"));
    }

    @GetMapping("/export/excel")
    @Operation(summary = "Export a report as Excel (.xls -- poi-ooxml unavailable offline, see README)")
    public ResponseEntity<byte[]> exportExcel(
            @RequestParam ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year) {
        ReportResponse report = resolveReport(period, date, year);
        byte[] bytes = excelExportGenerator.generate(report);
        logExport(period, ReportFormat.EXCEL, bytes.length);
        return fileResponse(bytes, "analytics-report.xls", MediaType.parseMediaType("application/vnd.ms-excel"));
    }

    @GetMapping("/export/pdf")
    @Operation(summary = "Export a report as PDF")
    public ResponseEntity<byte[]> exportPdf(
            @RequestParam ReportPeriod period,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(required = false) Integer year) {
        ReportResponse report = resolveReport(period, date, year);
        byte[] bytes = pdfReportGenerator.generate(report);
        logExport(period, ReportFormat.PDF, bytes.length);
        return fileResponse(bytes, "analytics-report.pdf", MediaType.APPLICATION_PDF);
    }

    private ReportResponse resolveReport(ReportPeriod period, LocalDate date, Integer year) {
        var companyId = currentUserProvider.getCurrentUser().getCompanyId();
        return switch (period) {
            case DAILY -> reportService.getDailyReport(companyId, date != null ? date : LocalDate.now());
            case MONTHLY -> reportService.getMonthlyReport(companyId, date != null ? date : LocalDate.now());
            case YEARLY -> reportService.getYearlyReport(companyId, year != null ? year : LocalDate.now().getYear());
        };
    }

    private void logExport(ReportPeriod period, ReportFormat format, long fileSizeBytes) {
        CurrentUser user = currentUserProvider.getCurrentUser();
        reportExportLogRepository.save(ReportExportLog.builder()
                .reportType(period.name())
                .format(format)
                .requestedBy(user.getUserId())
                .companyId(user.getCompanyId())
                .fileSizeBytes(fileSizeBytes)
                .build());
    }

    private ResponseEntity<byte[]> fileResponse(byte[] bytes, String fileName, MediaType mediaType) {
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + "\"")
                .body(bytes);
    }
}
