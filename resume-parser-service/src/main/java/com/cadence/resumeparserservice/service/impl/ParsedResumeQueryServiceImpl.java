package com.cadence.resumeparserservice.service.impl;

import com.cadence.resumeparserservice.dto.response.*;
import com.cadence.resumeparserservice.entity.ParsedResume;
import com.cadence.resumeparserservice.exception.ErrorCode;
import com.cadence.resumeparserservice.exception.ResourceNotFoundException;
import com.cadence.resumeparserservice.mapper.ParsedResumeMapper;
import com.cadence.resumeparserservice.repository.*;
import com.cadence.resumeparserservice.service.ParsedResumeQueryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ParsedResumeQueryServiceImpl implements ParsedResumeQueryService {

    private final ParsedResumeRepository parsedResumeRepository;
    private final CandidateSkillRepository skillRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateEducationRepository educationRepository;
    private final CandidateProjectRepository projectRepository;
    private final CandidateCertificationRepository certificationRepository;
    private final CandidateAchievementRepository achievementRepository;
    private final CandidateLanguageRepository languageRepository;
    private final ParserLogRepository parserLogRepository;
    private final ParsedResumeMapper mapper;

    @Override
    public ParsedResumeResponse getAggregate(UUID resumeId) {
        ParsedResume parsedResume = findByResumeIdOrThrow(resumeId);
        ParsedResumeResponse response = mapper.toResponse(parsedResume);
        UUID id = parsedResume.getId();
        response.setSkills(mapper.toSkillResponseList(skillRepository.findAllByParsedResumeId(id)));
        response.setExperience(mapper.toExperienceResponseList(experienceRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id)));
        response.setEducation(mapper.toEducationResponseList(educationRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id)));
        response.setProjects(mapper.toProjectResponseList(projectRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id)));
        response.setCertifications(mapper.toCertificationResponseList(certificationRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id)));
        response.setAchievements(mapper.toAchievementResponseList(achievementRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id)));
        response.setLanguages(mapper.toLanguageResponseList(languageRepository.findAllByParsedResumeId(id)));
        return response;
    }

    @Override
    public List<SkillResponse> getSkills(UUID resumeId) {
        UUID id = findByResumeIdOrThrow(resumeId).getId();
        return mapper.toSkillResponseList(skillRepository.findAllByParsedResumeId(id));
    }

    @Override
    public List<ExperienceResponse> getExperience(UUID resumeId) {
        UUID id = findByResumeIdOrThrow(resumeId).getId();
        return mapper.toExperienceResponseList(experienceRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id));
    }

    @Override
    public List<EducationResponse> getEducation(UUID resumeId) {
        UUID id = findByResumeIdOrThrow(resumeId).getId();
        return mapper.toEducationResponseList(educationRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id));
    }

    @Override
    public List<ProjectResponse> getProjects(UUID resumeId) {
        UUID id = findByResumeIdOrThrow(resumeId).getId();
        return mapper.toProjectResponseList(projectRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id));
    }

    @Override
    public List<CertificationResponse> getCertifications(UUID resumeId) {
        UUID id = findByResumeIdOrThrow(resumeId).getId();
        return mapper.toCertificationResponseList(certificationRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id));
    }

    @Override
    public ParsingStatusResponse getStatus(UUID resumeId) {
        return mapper.toStatusResponse(findByResumeIdOrThrow(resumeId));
    }

    @Override
    public List<ParserLogResponse> getLogs(UUID resumeId) {
        UUID id = findByResumeIdOrThrow(resumeId).getId();
        return mapper.toLogResponseList(parserLogRepository.findAllByParsedResumeIdOrderByCreatedAtAsc(id));
    }

    private ParsedResume findByResumeIdOrThrow(UUID resumeId) {
        return parsedResumeRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESUME_NOT_FOUND,
                        "No parsing record found for resume " + resumeId));
    }
}
