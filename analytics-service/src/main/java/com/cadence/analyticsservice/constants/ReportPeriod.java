package com.cadence.analyticsservice.constants;

/** Matches the literal API list exactly (/reports/daily, /monthly, /yearly) -- weekly/quarterly named in the prose module list but absent from both the Figma and the literal API list, not built. */
public enum ReportPeriod {
    DAILY,
    MONTHLY,
    YEARLY
}
