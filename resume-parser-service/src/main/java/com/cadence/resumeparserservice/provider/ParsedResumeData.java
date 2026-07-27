package com.cadence.resumeparserservice.provider;

import java.util.List;

/**
 * The one fixed contract every ResumeParserProvider implementation
 * must produce, regardless of which LLM answered the prompt. This is
 * the seam between "talking to an LLM" (provider/ package) and
 * "persisting structured data" (service layer) -- the service layer
 * never sees raw LLM JSON, only this record.
 */
public record ParsedResumeData(
        String fullName,
        String email,
        String phone,
        String location,
        String linkedinUrl,
        String githubUrl,
        String portfolioUrl,
        String professionalSummary,
        String currentCompany,
        String currentDesignation,
        Double totalExperienceYears,
        String noticePeriod,
        String expectedSalary,
        List<SkillItem> skills,
        List<ExperienceItem> experience,
        List<ProjectItem> projects,
        List<EducationItem> education,
        List<CertificationItem> certifications,
        List<AchievementItem> achievements,
        List<LanguageItem> languages
) {
    public record SkillItem(String name, String category) {}

    public record ExperienceItem(String companyName, String designation, String startDate,
                                  String endDate, boolean isCurrent, String description) {}

    public record ProjectItem(String name, String description, String technologies) {}

    public record EducationItem(String institutionName, String degree, String fieldOfStudy,
                                 String startDate, String endDate, String grade) {}

    public record CertificationItem(String name, String issuingOrganization,
                                     String issuedDate, String expiryDate, String credentialId) {}

    public record AchievementItem(String title, String description) {}

    public record LanguageItem(String name, String proficiency) {}
}
