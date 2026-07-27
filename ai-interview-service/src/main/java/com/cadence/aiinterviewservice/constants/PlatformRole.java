package com.cadence.aiinterviewservice.constants;

import java.util.Set;

/**
 * Same role set as every other Cadence service. Read-mostly for
 * recruiting roles (shortlist/ranking/queue/report views), with the
 * decision-changing actions (shortlist/reject/manual-review/move-to-
 * coding/recalculate) also open to those same roles except
 * HIRING_MANAGER, which stays view-only across this platform.
 */
public final class PlatformRole {
    private PlatformRole() {}

    public static final String CANDIDATE = "CANDIDATE";
    public static final String ADMIN = "ADMIN";

    public static final String COMPANY_ADMIN = "COMPANY_ADMIN";
    public static final String HR_MANAGER = "HR_MANAGER";
    public static final String HR_RECRUITER = "HR_RECRUITER";
    public static final String TECHNICAL_RECRUITER = "TECHNICAL_RECRUITER";
    public static final String TALENT_ACQUISITION_MANAGER = "TALENT_ACQUISITION_MANAGER";
    public static final String HIRING_MANAGER = "HIRING_MANAGER";

    public static final Set<String> RECRUITING_ROLES = Set.of(
            COMPANY_ADMIN, HR_MANAGER, HR_RECRUITER, TECHNICAL_RECRUITER,
            TALENT_ACQUISITION_MANAGER, HIRING_MANAGER
    );
}
