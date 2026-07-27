package com.cadence.applicationservice.constant;

import java.util.Set;

/**
 * Candidate-self-service endpoints (apply/withdraw/track) are
 * restricted to CANDIDATE. The recruiting-side roles below authorize
 * every other endpoint -- viewing/searching applications, assigning
 * recruiters/hiring managers, changing status, adding notes -- scoped
 * to the caller's own companyId from the JWT. Mirrors Job Service's own
 * "Hiring Manager is view-only unless granted an edit permission" rule,
 * since a Hiring Manager routinely needs to see a pipeline without
 * being able to reassign or reject on their own.
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

    public static final Set<String> ALLOWED_ROLES = Set.of(
            COMPANY_ADMIN, HR_MANAGER, HR_RECRUITER, TECHNICAL_RECRUITER,
            TALENT_ACQUISITION_MANAGER, HIRING_MANAGER
    );

    /** Everyone except Hiring Manager can write; Hiring Manager needs the APPLICATION_EDIT permission claim too. */
    public static final Set<String> FULL_WRITE_ROLES = Set.of(
            COMPANY_ADMIN, HR_MANAGER, HR_RECRUITER, TECHNICAL_RECRUITER, TALENT_ACQUISITION_MANAGER
    );

    public static final String APPLICATION_EDIT_PERMISSION = "APPLICATION_EDIT";
}
