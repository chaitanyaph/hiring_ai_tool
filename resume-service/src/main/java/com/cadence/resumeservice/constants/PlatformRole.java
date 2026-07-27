package com.cadence.resumeservice.constants;

import java.util.Set;

/**
 * CANDIDATE owns full self-service (upload/delete/rename/set-default/
 * download/preview their own resumes). The 6 recruiting-side roles may
 * only preview/download -- never upload, edit or delete -- and only
 * for a candidate who has actually applied to their own company (see
 * ResumeServiceImpl.requireRecruiterAccess). ADMIN bypasses all
 * company-scoping for platform support/back-office use.
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
