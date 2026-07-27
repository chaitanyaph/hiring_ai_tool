package com.cadence.resumeparserservice.provider;

import com.cadence.resumeparserservice.exception.ResumeParsingPipelineException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Shared prompt template + JSON-extraction logic for every provider --
 * factored out so the "LLMs sometimes wrap JSON in markdown fences"
 * problem is solved once, not three times.
 */
public abstract class AbstractResumeParserProvider implements ResumeParserProvider {

    protected static final ObjectMapper OBJECT_MAPPER = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    private static final String PROMPT_TEMPLATE = """
            You are a resume parsing engine. Extract structured information from the resume text below and return ONLY a single valid JSON object matching this exact schema -- no markdown, no explanation, no code fences, just the raw JSON object.

            {
              "fullName": string or null,
              "email": string or null,
              "phone": string or null,
              "location": string or null,
              "linkedinUrl": string or null,
              "githubUrl": string or null,
              "portfolioUrl": string or null,
              "professionalSummary": string or null,
              "currentCompany": string or null,
              "currentDesignation": string or null,
              "totalExperienceYears": number or null,
              "noticePeriod": string or null,
              "expectedSalary": string or null,
              "skills": [{"name": string, "category": one of TECHNICAL, SOFT, PROGRAMMING_LANGUAGE, FRAMEWORK, LIBRARY, DATABASE, CLOUD, DEVOPS}],
              "experience": [{"companyName": string, "designation": string, "startDate": string, "endDate": string or null, "isCurrent": boolean, "description": string}],
              "projects": [{"name": string, "description": string, "technologies": string}],
              "education": [{"institutionName": string, "degree": string, "fieldOfStudy": string, "startDate": string, "endDate": string, "grade": string}],
              "certifications": [{"name": string, "issuingOrganization": string, "issuedDate": string, "expiryDate": string or null, "credentialId": string or null}],
              "achievements": [{"title": string, "description": string}],
              "languages": [{"name": string, "proficiency": string or null}]
            }

            Rules:
            - Return ONLY the JSON object, nothing else -- no markdown code fences, no commentary.
            - If a field cannot be determined from the text, use null (or an empty array for list fields).
            - Preserve dates exactly as they appear in the resume text (e.g. "Jan 2023", "2021").

            Resume text:
            \"\"\"
            %s
            \"\"\"
            """;

    private static final String MATCH_PROMPT_TEMPLATE = """
            You are a resume-to-job matching engine for a hiring platform. Compare the candidate's parsed resume data against the job's requirements below and return ONLY a single valid JSON object matching this exact schema -- no markdown, no explanation, no code fences, just the raw JSON object.

            {
              "overallMatchScore": integer 0-100,
              "technicalSkillScore": integer 0-100,
              "programmingLanguageScore": integer 0-100,
              "frameworkScore": integer 0-100,
              "databaseScore": integer 0-100,
              "cloudScore": integer 0-100,
              "devopsScore": integer 0-100,
              "experienceMatchLabel": one of STRONG, ADEQUATE, BELOW_RANGE,
              "projectMatchScore": integer 0-100,
              "educationMatchLabel": one of MATCH, PARTIAL, MISMATCH,
              "certificationMatchLabel": one of MATCH, PARTIAL, NONE,
              "communicationPrediction": integer 0-100,
              "problemSolvingPrediction": integer 0-100,
              "learningAbilityPrediction": integer 0-100,
              "matchedSkills": [{"name": string, "category": one of TECHNICAL, SOFT, PROGRAMMING_LANGUAGE, FRAMEWORK, LIBRARY, DATABASE, CLOUD, DEVOPS}],
              "missingSkills": [{"name": string, "category": same categories as above, "required": boolean}],
              "strengths": [string, ...],
              "weaknesses": [string, ...],
              "recommendedLearningTopics": string (a short comma-separated list, or null),
              "hiringRecommendation": one of PROCEED, HOLD, REJECT,
              "overallAiSummary": string (2-4 sentences)
            }

            Rules:
            - Base every score and label strictly on the data provided below -- do not invent skills, experience, or education not present.
            - communicationPrediction/problemSolvingPrediction/learningAbilityPrediction are inferred estimates from resume tone/structure/project complexity, not measured facts -- keep them conservative.
            - Return ONLY the JSON object, nothing else.

            Job: %s
            Required experience: %s-%s years
            Required skills: %s
            Preferred skills: %s
            Required education: %s
            Required certifications: %s

            Candidate: %s
            Total experience: %s years
            Professional summary: %s
            Skills: %s
            Experience: %s
            Education: %s
            Certifications: %s
            Projects: %s
            """;

    protected final String buildPrompt(String resumeText) {
        return PROMPT_TEMPLATE.formatted(resumeText);
    }

    protected final String buildMatchPrompt(ParsedResumeSnapshot resume, JobRequirementsSnapshot job) {
        return MATCH_PROMPT_TEMPLATE.formatted(
                nullToNone(job.jobTitle()), nullToNone(job.minExperienceYears()), nullToNone(job.maxExperienceYears()),
                joinOrNone(job.requiredSkills()), joinOrNone(job.preferredSkills()),
                nullToNone(job.education()), nullToNone(job.certifications()),
                nullToNone(resume.fullName()), nullToNone(resume.totalExperienceYears()),
                nullToNone(resume.professionalSummary()), joinOrNone(resume.skills()),
                joinOrNone(resume.experienceSummaries()), joinOrNone(resume.educationSummaries()),
                joinOrNone(resume.certificationNames()), joinOrNone(resume.projectSummaries()));
    }

    private String nullToNone(Object value) {
        return value == null ? "Not specified" : String.valueOf(value);
    }

    private String joinOrNone(java.util.List<String> values) {
        return (values == null || values.isEmpty()) ? "None listed" : String.join("; ", values);
    }

    protected final ParsedResumeData extractJson(String rawOutput) {
        return extractJson(rawOutput, ParsedResumeData.class);
    }

    protected final MatchAnalysisData extractMatchJson(String rawOutput) {
        return extractJson(rawOutput, MatchAnalysisData.class);
    }

    private <T> T extractJson(String rawOutput, Class<T> type) {
        String cleaned = stripMarkdownFences(rawOutput);
        try {
            return OBJECT_MAPPER.readValue(cleaned, type);
        } catch (Exception e) {
            throw new ResumeParsingPipelineException(
                    "Provider " + getProviderName() + " returned invalid JSON: " + e.getMessage(), e);
        }
    }

    private String stripMarkdownFences(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int firstNewline = trimmed.indexOf('\n');
            int lastFence = trimmed.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return trimmed.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return trimmed;
    }
}
