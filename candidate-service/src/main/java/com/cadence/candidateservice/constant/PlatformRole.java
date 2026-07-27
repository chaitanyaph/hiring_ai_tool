package com.cadence.candidateservice.constant;

/**
 * Candidate-self-service endpoints (profile, applications, saved jobs)
 * are restricted to CANDIDATE. The recruiting-side roles below are only
 * used to authorize the one endpoint that isn't candidate-self-service:
 * advancing an application's pipeline stage -- mirrors the same 6 roles
 * Job Service recognizes for its own hiring workflows.
 */
public final class PlatformRole {
    private PlatformRole() {}

    public static final String CANDIDATE = "CANDIDATE";

    public static final String COMPANY_ADMIN = "COMPANY_ADMIN";
    public static final String HR_MANAGER = "HR_MANAGER";
    public static final String HR_RECRUITER = "HR_RECRUITER";
    public static final String TECHNICAL_RECRUITER = "TECHNICAL_RECRUITER";
    public static final String TALENT_ACQUISITION_MANAGER = "TALENT_ACQUISITION_MANAGER";
    public static final String HIRING_MANAGER = "HIRING_MANAGER";
}
