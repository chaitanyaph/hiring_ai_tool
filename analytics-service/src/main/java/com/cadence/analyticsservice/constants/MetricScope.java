package com.cadence.analyticsservice.constants;

import java.util.UUID;

/** GLOBAL uses the sentinel NO_SCOPE_ID (never a real UUID) -- see MetricSnapshot javadoc. */
public enum MetricScope {
    GLOBAL,
    COMPANY,
    JOB,
    RECRUITER,
    DEPARTMENT;

    public static final UUID NO_SCOPE_ID = new UUID(0L, 0L);
}
