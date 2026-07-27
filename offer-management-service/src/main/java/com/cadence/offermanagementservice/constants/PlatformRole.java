package com.cadence.offermanagementservice.constants;

import java.util.Set;

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

    public static final Set<String> RECRUITING_WRITE_ROLES = Set.of(
            COMPANY_ADMIN, HR_MANAGER, HR_RECRUITER, TECHNICAL_RECRUITER, TALENT_ACQUISITION_MANAGER
    );
}
