package com.cadence.resumeparserservice.service;

import com.cadence.resumeparserservice.dto.response.*;

import java.util.List;
import java.util.UUID;

/** Read-only side backing the drawer's main view and every sub-section. */
public interface ParsedResumeQueryService {

    ParsedResumeResponse getAggregate(UUID resumeId);

    List<SkillResponse> getSkills(UUID resumeId);

    List<ExperienceResponse> getExperience(UUID resumeId);

    List<EducationResponse> getEducation(UUID resumeId);

    List<ProjectResponse> getProjects(UUID resumeId);

    List<CertificationResponse> getCertifications(UUID resumeId);

    ParsingStatusResponse getStatus(UUID resumeId);

    List<ParserLogResponse> getLogs(UUID resumeId);
}
