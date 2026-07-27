package com.cadence.analyticsservice.constants;

import java.time.LocalDate;

public enum PeriodType {
    ALL_TIME,
    DAILY,
    MONTHLY;

    /** Sentinel date used whenever period_date has no natural meaning (ALL_TIME). */
    public static final LocalDate ALL_TIME_DATE = LocalDate.of(1970, 1, 1);
}
