package com.cadence.candidateservice.service.impl;

import com.cadence.candidateservice.config.RedisConfig;
import com.cadence.candidateservice.dto.request.*;
import com.cadence.candidateservice.dto.response.CandidateProfileResponse;
import com.cadence.candidateservice.dto.response.CandidateSummaryResponse;
import com.cadence.candidateservice.dto.response.ResumeUploadResponse;
import com.cadence.candidateservice.entity.*;
import com.cadence.candidateservice.exception.ErrorCode;
import com.cadence.candidateservice.exception.ResourceNotFoundException;
import com.cadence.candidateservice.kafka.event.ProfileCreatedEvent;
import com.cadence.candidateservice.kafka.event.ProfileUpdatedEvent;
import com.cadence.candidateservice.kafka.event.ResumeUploadedEvent;
import com.cadence.candidateservice.kafka.producer.CandidateEventProducer;
import com.cadence.candidateservice.mapper.CandidateMapper;
import com.cadence.candidateservice.repository.*;
import com.cadence.candidateservice.security.CurrentUser;
import com.cadence.candidateservice.service.CandidateProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CandidateProfileServiceImpl implements CandidateProfileService {

    private final CandidateRepository candidateRepository;
    private final CandidateEducationRepository educationRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateSkillRepository skillRepository;
    private final CandidateProjectRepository projectRepository;
    private final CandidateCertificationRepository certificationRepository;
    private final CandidateLanguageRepository languageRepository;
    private final CandidateJobPreferenceRepository jobPreferenceRepository;
    private final CandidatePortfolioLinkRepository portfolioLinkRepository;
    private final CandidateMapper candidateMapper;
    private final CandidateEventProducer eventProducer;
    private final CacheManager cacheManager;

    @Value("${app.candidate.resume-storage-path}")
    private String resumeStoragePath;

    private static final int TOTAL_WIZARD_SECTIONS = 10;

    @Override
    @Transactional(readOnly = true)
    public CandidateProfileResponse getProfile(CurrentUser user) {
        return assembleProfile(findCandidateOrThrow(user.getUserId()));
    }

    @Override
    @Transactional(readOnly = true)
    public CandidateSummaryResponse getSummary(UUID candidateId) {
        Candidate candidate = findCandidateOrThrow(candidateId);
        return CandidateSummaryResponse.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .email(candidate.getEmail())
                .resumeUploaded(candidate.getResumeUrl() != null && !candidate.getResumeUrl().isBlank())
                .profileCompletionPercent(candidate.getProfileCompletionPercent())
                .status(candidate.getStatus())
                .build();
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateBasicInfo(CurrentUser user, BasicInfoRequest request) {
        boolean isNew = !candidateRepository.existsById(user.getUserId());
        Candidate candidate = candidateRepository.findById(user.getUserId())
                .orElseGet(() -> Candidate.builder()
                        .id(user.getUserId())
                        .email(user.getEmail())
                        .build());

        candidate.setFullName(request.getFullName().trim());
        candidate.setHeadline(request.getHeadline());
        candidate.setPhone(request.getPhone());
        candidate.setLocation(request.getLocation());
        candidate.setCurrentCompany(request.getCurrentCompany());
        candidate.setNoticePeriodDays(request.getNoticePeriodDays());
        candidate.setCreatedBy(user.getUserId());
        candidate.setUpdatedBy(user.getUserId());
        candidate = candidateRepository.save(candidate);

        recomputeCompletion(candidate);

        if (isNew) {
            eventProducer.publishProfileCreated(ProfileCreatedEvent.builder()
                    .candidateId(candidate.getId()).fullName(candidate.getFullName())
                    .email(candidate.getEmail()).occurredAt(LocalDateTime.now()).build());
        } else {
            publishUpdated(candidate);
        }
        evictProfileCache(candidate.getId());

        return assembleProfile(candidate);
    }

    @Override
    @Transactional
    public ResumeUploadResponse uploadResume(CurrentUser user, MultipartFile file) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        try {
            Path storageDir = Path.of(resumeStoragePath);
            Files.createDirectories(storageDir);
            String safeFilename = candidate.getId() + "-" + System.currentTimeMillis() + "-" + sanitizeFilename(file.getOriginalFilename());
            Path target = storageDir.resolve(safeFilename);
            Files.copy(file.getInputStream(), target, StandardCopyOption.REPLACE_EXISTING);

            candidate.setResumeUrl(target.toString());
            candidate.setResumeFilename(file.getOriginalFilename());
            candidate.setResumeParsedAt(null);
            candidate.setUpdatedBy(user.getUserId());
            candidate = candidateRepository.save(candidate);
        } catch (IOException e) {
            throw new RuntimeException("Failed to store resume file", e);
        }

        recomputeCompletion(candidate);
        eventProducer.publishResumeUploaded(ResumeUploadedEvent.builder()
                .candidateId(candidate.getId()).resumeUrl(candidate.getResumeUrl())
                .resumeFilename(candidate.getResumeFilename()).occurredAt(LocalDateTime.now()).build());
        evictProfileCache(candidate.getId());

        return ResumeUploadResponse.builder()
                .resumeUrl(candidate.getResumeUrl())
                .resumeFilename(candidate.getResumeFilename())
                .uploadedAt(candidate.getUpdatedAt())
                .profileCompletionPercent(candidate.getProfileCompletionPercent())
                .build();
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateEducation(CurrentUser user, UpdateEducationRequest request) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        List<CandidateEducation> existing = educationRepository.findAllByCandidateIdOrderByDisplayOrderAsc(candidate.getId());
        Map<UUID, CandidateEducation> existingById = existing.stream().collect(Collectors.toMap(CandidateEducation::getId, e -> e));
        Set<UUID> keptIds = new HashSet<>();

        List<EducationItemRequest> items = request.getItems() == null ? List.of() : request.getItems();
        for (int i = 0; i < items.size(); i++) {
            EducationItemRequest item = items.get(i);
            CandidateEducation entity;
            if (item.getId() != null && existingById.containsKey(item.getId())) {
                entity = existingById.get(item.getId());
            } else {
                entity = CandidateEducation.builder().candidateId(candidate.getId()).build();
            }
            entity.setDegree(item.getDegree().trim());
            entity.setInstitution(item.getInstitution().trim());
            entity.setStartYear(item.getStartYear());
            entity.setEndYear(item.getEndYear());
            entity.setGrade(item.getGrade());
            entity.setDisplayOrder(i);
            entity.setCreatedBy(user.getUserId());
            entity.setUpdatedBy(user.getUserId());
            entity = educationRepository.save(entity);
            keptIds.add(entity.getId());
        }
        existing.stream().filter(e -> !keptIds.contains(e.getId())).forEach(e -> {
            e.markDeleted();
            educationRepository.save(e);
        });

        return finishWizardStep(candidate, user);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateExperience(CurrentUser user, UpdateExperienceRequest request) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        List<CandidateExperience> existing = experienceRepository.findAllByCandidateIdOrderByDisplayOrderAsc(candidate.getId());
        Map<UUID, CandidateExperience> existingById = existing.stream().collect(Collectors.toMap(CandidateExperience::getId, e -> e));
        Set<UUID> keptIds = new HashSet<>();

        List<ExperienceItemRequest> items = request.getItems() == null ? List.of() : request.getItems();
        for (int i = 0; i < items.size(); i++) {
            ExperienceItemRequest item = items.get(i);
            CandidateExperience entity;
            if (item.getId() != null && existingById.containsKey(item.getId())) {
                entity = existingById.get(item.getId());
            } else {
                entity = CandidateExperience.builder().candidateId(candidate.getId()).build();
            }
            entity.setJobTitle(item.getJobTitle().trim());
            entity.setCompanyName(item.getCompanyName().trim());
            entity.setStartDate(item.getStartDate());
            entity.setEndDate(item.getEndDate());
            entity.setCurrentlyWorking(item.isCurrentlyWorking());
            entity.setAchievements(item.getAchievements());
            entity.setDisplayOrder(i);
            entity.setCreatedBy(user.getUserId());
            entity.setUpdatedBy(user.getUserId());
            entity = experienceRepository.save(entity);
            keptIds.add(entity.getId());
        }
        existing.stream().filter(e -> !keptIds.contains(e.getId())).forEach(e -> {
            e.markDeleted();
            experienceRepository.save(e);
        });

        return finishWizardStep(candidate, user);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateSkills(CurrentUser user, UpdateSkillsRequest request) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        skillRepository.deleteAllByCandidateId(candidate.getId());
        List<String> skills = request.getSkills() == null ? List.of() : request.getSkills();
        List<CandidateSkill> toSave = skills.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(s -> CandidateSkill.builder().candidateId(candidate.getId()).skillName(s.trim()).build())
                .toList();
        if (!toSave.isEmpty()) {
            skillRepository.saveAll(toSave);
        }

        return finishWizardStep(candidate, user);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateProjects(CurrentUser user, UpdateProjectsRequest request) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        List<CandidateProject> existing = projectRepository.findAllByCandidateIdOrderByDisplayOrderAsc(candidate.getId());
        Map<UUID, CandidateProject> existingById = existing.stream().collect(Collectors.toMap(CandidateProject::getId, p -> p));
        Set<UUID> keptIds = new HashSet<>();

        List<ProjectItemRequest> items = request.getItems() == null ? List.of() : request.getItems();
        for (int i = 0; i < items.size(); i++) {
            ProjectItemRequest item = items.get(i);
            CandidateProject entity;
            if (item.getId() != null && existingById.containsKey(item.getId())) {
                entity = existingById.get(item.getId());
            } else {
                entity = CandidateProject.builder().candidateId(candidate.getId()).build();
            }
            entity.setTitle(item.getTitle().trim());
            entity.setDescription(item.getDescription());
            entity.setProjectUrl(item.getProjectUrl());
            entity.setDisplayOrder(i);
            entity.setCreatedBy(user.getUserId());
            entity.setUpdatedBy(user.getUserId());
            entity = projectRepository.save(entity);
            keptIds.add(entity.getId());
        }
        existing.stream().filter(p -> !keptIds.contains(p.getId())).forEach(p -> {
            p.markDeleted();
            projectRepository.save(p);
        });

        return finishWizardStep(candidate, user);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateCertifications(CurrentUser user, UpdateCertificationsRequest request) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        List<CandidateCertification> existing = certificationRepository.findAllByCandidateIdOrderByDisplayOrderAsc(candidate.getId());
        Map<UUID, CandidateCertification> existingById = existing.stream().collect(Collectors.toMap(CandidateCertification::getId, c -> c));
        Set<UUID> keptIds = new HashSet<>();

        List<CertificationItemRequest> items = request.getItems() == null ? List.of() : request.getItems();
        for (int i = 0; i < items.size(); i++) {
            CertificationItemRequest item = items.get(i);
            CandidateCertification entity;
            if (item.getId() != null && existingById.containsKey(item.getId())) {
                entity = existingById.get(item.getId());
            } else {
                entity = CandidateCertification.builder().candidateId(candidate.getId()).build();
            }
            entity.setName(item.getName().trim());
            entity.setIssuedBy(item.getIssuedBy());
            entity.setIssueDate(item.getIssueDate());
            entity.setCredentialUrl(item.getCredentialUrl());
            entity.setDisplayOrder(i);
            entity.setCreatedBy(user.getUserId());
            entity.setUpdatedBy(user.getUserId());
            entity = certificationRepository.save(entity);
            keptIds.add(entity.getId());
        }
        existing.stream().filter(c -> !keptIds.contains(c.getId())).forEach(c -> {
            c.markDeleted();
            certificationRepository.save(c);
        });

        return finishWizardStep(candidate, user);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateLanguages(CurrentUser user, UpdateLanguagesRequest request) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        languageRepository.deleteAllByCandidateId(candidate.getId());
        List<String> languages = request.getLanguages() == null ? List.of() : request.getLanguages();
        List<CandidateLanguage> toSave = languages.stream()
                .filter(l -> l != null && !l.isBlank())
                .map(l -> CandidateLanguage.builder().candidateId(candidate.getId()).languageName(l.trim()).build())
                .toList();
        if (!toSave.isEmpty()) {
            languageRepository.saveAll(toSave);
        }

        return finishWizardStep(candidate, user);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updateJobPreferences(CurrentUser user, JobPreferencesRequest request) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        CandidateJobPreference prefs = jobPreferenceRepository.findByCandidateId(candidate.getId())
                .orElseGet(() -> CandidateJobPreference.builder().candidateId(candidate.getId()).build());
        prefs.setPreferredWorkType(request.getPreferredWorkType());
        prefs.setPreferredEmploymentType(request.getPreferredEmploymentType());
        prefs.setExpectedSalary(request.getExpectedSalary());
        if (request.getSalaryCurrency() != null && !request.getSalaryCurrency().isBlank()) {
            prefs.setSalaryCurrency(request.getSalaryCurrency());
        }
        prefs.setNoticePeriod(request.getNoticePeriod());
        prefs.setPreferredLocations(request.getPreferredLocations());
        jobPreferenceRepository.save(prefs);

        return finishWizardStep(candidate, user);
    }

    @Override
    @Transactional
    public CandidateProfileResponse updatePortfolio(CurrentUser user, PortfolioRequest request) {
        Candidate candidate = findCandidateOrThrow(user.getUserId());

        CandidatePortfolioLink portfolio = portfolioLinkRepository.findByCandidateId(candidate.getId())
                .orElseGet(() -> CandidatePortfolioLink.builder().candidateId(candidate.getId()).build());
        portfolio.setWebsiteUrl(request.getWebsiteUrl());
        portfolio.setLinkedinUrl(request.getLinkedinUrl());
        portfolio.setGithubUrl(request.getGithubUrl());
        portfolioLinkRepository.save(portfolio);

        return finishWizardStep(candidate, user);
    }

    // ------------------------------------------------------------------

    private CandidateProfileResponse finishWizardStep(Candidate candidate, CurrentUser user) {
        recomputeCompletion(candidate);
        candidate.setUpdatedBy(user.getUserId());
        candidate = candidateRepository.save(candidate);
        publishUpdated(candidate);
        evictProfileCache(candidate.getId());
        return assembleProfile(candidate);
    }

    private void publishUpdated(Candidate candidate) {
        eventProducer.publishProfileUpdated(ProfileUpdatedEvent.builder()
                .candidateId(candidate.getId())
                .profileCompletionPercent(candidate.getProfileCompletionPercent())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private Candidate findCandidateOrThrow(UUID candidateId) {
        return candidateRepository.findById(candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_NOT_FOUND,
                        "Complete Step 1 (Basic Info) before continuing the profile wizard"));
    }

    /**
     * 10 sections mirror the 10 wizard steps 1:1. Recomputed after every
     * wizard-step write rather than cached separately, since it's cheap
     * (a handful of count queries) and must never drift from reality.
     */
    private void recomputeCompletion(Candidate candidate) {
        int completed = 1; // Step 1 (basic info) is always done once this row exists
        if (candidate.getResumeUrl() != null && !candidate.getResumeUrl().isBlank()) completed++;
        if (educationRepository.countByCandidateId(candidate.getId()) > 0) completed++;
        if (experienceRepository.countByCandidateId(candidate.getId()) > 0) completed++;
        if (skillRepository.countByCandidateId(candidate.getId()) > 0) completed++;
        if (projectRepository.countByCandidateId(candidate.getId()) > 0) completed++;
        if (certificationRepository.countByCandidateId(candidate.getId()) > 0) completed++;
        if (languageRepository.countByCandidateId(candidate.getId()) > 0) completed++;
        if (jobPreferenceRepository.findByCandidateId(candidate.getId()).isPresent()) completed++;
        if (portfolioLinkRepository.findByCandidateId(candidate.getId())
                .filter(this::hasAnyPortfolioLink).isPresent()) completed++;

        candidate.setProfileCompletionPercent(Math.round(completed * 100f / TOTAL_WIZARD_SECTIONS));
    }

    private boolean hasAnyPortfolioLink(CandidatePortfolioLink link) {
        return notBlank(link.getWebsiteUrl()) || notBlank(link.getLinkedinUrl()) || notBlank(link.getGithubUrl());
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private CandidateProfileResponse assembleProfile(Candidate candidate) {
        List<String> skills = skillRepository.findAllByCandidateId(candidate.getId()).stream()
                .map(CandidateSkill::getSkillName).toList();
        List<String> languages = languageRepository.findAllByCandidateId(candidate.getId()).stream()
                .map(CandidateLanguage::getLanguageName).toList();

        return CandidateProfileResponse.builder()
                .id(candidate.getId())
                .fullName(candidate.getFullName())
                .headline(candidate.getHeadline())
                .email(candidate.getEmail())
                .phone(candidate.getPhone())
                .location(candidate.getLocation())
                .currentCompany(candidate.getCurrentCompany())
                .noticePeriodDays(candidate.getNoticePeriodDays())
                .profilePhotoUrl(candidate.getProfilePhotoUrl())
                .resumeUrl(candidate.getResumeUrl())
                .resumeFilename(candidate.getResumeFilename())
                .resumeParsedAt(candidate.getResumeParsedAt())
                .aiResumeScore(candidate.getAiResumeScore())
                .profileCompletionPercent(candidate.getProfileCompletionPercent())
                .status(candidate.getStatus())
                .education(candidateMapper.toEducationResponseList(educationRepository.findAllByCandidateIdOrderByDisplayOrderAsc(candidate.getId())))
                .experience(candidateMapper.toExperienceResponseList(experienceRepository.findAllByCandidateIdOrderByDisplayOrderAsc(candidate.getId())))
                .skills(skills)
                .projects(candidateMapper.toProjectResponseList(projectRepository.findAllByCandidateIdOrderByDisplayOrderAsc(candidate.getId())))
                .certifications(candidateMapper.toCertificationResponseList(certificationRepository.findAllByCandidateIdOrderByDisplayOrderAsc(candidate.getId())))
                .languages(languages)
                .jobPreferences(jobPreferenceRepository.findByCandidateId(candidate.getId()).map(candidateMapper::toResponse).orElse(null))
                .portfolio(portfolioLinkRepository.findByCandidateId(candidate.getId()).map(candidateMapper::toResponse).orElse(null))
                .createdAt(candidate.getCreatedAt())
                .updatedAt(candidate.getUpdatedAt())
                .version(candidate.getVersion())
                .build();
    }

    private void evictProfileCache(UUID candidateId) {
        var cache = cacheManager.getCache(RedisConfig.PROFILE_CACHE);
        if (cache != null) cache.evict(candidateId);
        var dashboardCache = cacheManager.getCache(RedisConfig.DASHBOARD_CACHE);
        if (dashboardCache != null) dashboardCache.evict(candidateId);
    }

    private String sanitizeFilename(String original) {
        if (original == null) return "resume";
        return original.replaceAll("[^a-zA-Z0-9._-]", "_");
    }
}
