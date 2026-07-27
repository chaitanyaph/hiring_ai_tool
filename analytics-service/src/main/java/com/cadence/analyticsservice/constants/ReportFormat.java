package com.cadence.analyticsservice.constants;

/** ExcelExportGenerator produces real .xls (HSSFWorkbook) not .xlsx -- poi-ooxml isn't available offline, flagged in README. */
public enum ReportFormat {
    CSV,
    EXCEL,
    PDF
}
