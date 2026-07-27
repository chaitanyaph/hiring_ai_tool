package com.cadence.jobservice.constant;

import java.util.Set;

/**
 * Mirrors the role names Auth Service issues in the JWT `role` claim.
 * Job Service doesn't own these roles, it just recognizes the ones
 * relevant to job management -- everything else (INTERVIEWER, CANDIDATE,
 * etc.) is implicitly denied by not appearing in ALLOWED_ROLES.
 */
public final class PlatformRole {
    private PlatformRole() {}

    public static final String COMPANY_ADMIN = "COMPANY_ADMIN";
    public static final String HR_MANAGER = "HR_MANAGER";
    public static final String HR_RECRUITER = "HR_RECRUITER";
    public static final String TECHNICAL_RECRUITER = "TECHNICAL_RECRUITER";
    public static final String TALENT_ACQUISITION_MANAGER = "TALENT_ACQUISITION_MANAGER";
    public static final String HIRING_MANAGER = "HIRING_MANAGER";

    public static final Set<String> ALLOWED_ROLES = Set.of(
            COMPANY_ADMIN, HR_MANAGER, HR_RECRUITER, TECHNICAL_RECRUITER,
            TALENT_ACQUISITION_MANAGER, HIRING_MANAGER
    );

    /** Everyone except Hiring Manager can write; Hiring Manager needs the JOB_EDIT permission claim too. */
    public static final Set<String> FULL_WRITE_ROLES = Set.of(
            COMPANY_ADMIN, HR_MANAGER, HR_RECRUITER, TECHNICAL_RECRUITER, TALENT_ACQUISITION_MANAGER
    );

    public static final String JOB_EDIT_PERMISSION = "JOB_EDIT";
}
