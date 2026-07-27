package com.cadence.companyservice.constant;

public final class KafkaTopics {
    private KafkaTopics() {}

    public static final String COMPANY_CREATED = "company.company.created";
    public static final String COMPANY_UPDATED = "company.company.updated";
    public static final String DEPARTMENT_CREATED = "company.department.created";
    public static final String DEPARTMENT_UPDATED = "company.department.updated";
    public static final String DEPARTMENT_DELETED = "company.department.deleted";
    public static final String OFFICE_CREATED = "company.office.created";
    public static final String OFFICE_UPDATED = "company.office.updated";
    public static final String OFFICE_DELETED = "company.office.deleted";
    public static final String TEAM_INVITATION_CREATED = "company.team-invitation.created";
    public static final String TEAM_INVITATION_CANCELLED = "company.team-invitation.cancelled";

    // Consumed
    public static final String USER_CREATED = "auth.user.created";
    public static final String INVITATION_ACCEPTED = "auth.invitation.accepted";
}
