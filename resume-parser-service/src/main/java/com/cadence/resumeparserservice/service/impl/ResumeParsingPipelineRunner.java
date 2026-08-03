package com.cadence.resumeparserservice.service.impl;

import com.cadence.resumeparserservice.constants.AiProvider;
import com.cadence.resumeparserservice.constants.LogLevel;
import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.constants.SkillCategory;
import com.cadence.resumeparserservice.entity.*;
import com.cadence.resumeparserservice.exception.ResumeParsingPipelineException;
import com.cadence.resumeparserservice.feign.CandidateServiceClient;
import com.cadence.resumeparserservice.feign.ResumeServiceClient;
import com.cadence.resumeparserservice.feign.dto.CandidateDto;
import com.cadence.resumeparserservice.feign.dto.ResumeMetadataDto;
import com.cadence.resumeparserservice.feign.dto.ResumeObjectDetailsDto;
import com.cadence.resumeparserservice.kafka.event.ResumeParsedEvent;
import com.cadence.resumeparserservice.kafka.event.ResumeParsingFailedEvent;
import com.cadence.resumeparserservice.kafka.producer.ResumeParserEventProducer;
import com.cadence.resumeparserservice.provider.ParsedResumeData;
import com.cadence.resumeparserservice.provider.ResumeParserProvider;
import com.cadence.resumeparserservice.repository.*;
import com.cadence.resumeparserservice.service.ResumeMatchAnalysisService;
import com.cadence.resumeparserservice.strategy.ResumeParserProviderFactory;
import com.cadence.resumeparserservice.util.MinioObjectReader;
import com.cadence.resumeparserservice.util.PdfTextExtractor;
import com.cadence.resumeparserservice.util.TextCleaner;
import com.cadence.resumeparserservice.validation.ParsedDataValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The actual pipeline (candidate validation -> metadata -> download ->
 * extract -> clean -> AI parse -> validate -> persist -> publish),
 * running off the calling thread via @Async. A separate bean from
 * ResumeParsingServiceImpl on purpose -- see that class's Javadoc.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeParsingPipelineRunner {

    private final ParsedResumeRepository parsedResumeRepository;
    private final CandidateSkillRepository skillRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateProjectRepository projectRepository;
    private final CandidateEducationRepository educationRepository;
    private final CandidateCertificationRepository certificationRepository;
    private final CandidateAchievementRepository achievementRepository;
    private final CandidateLanguageRepository languageRepository;
    private final ParserLogRepository parserLogRepository;

    private final CandidateServiceClient candidateServiceClient;
    private final ResumeServiceClient resumeServiceClient;

    private final MinioObjectReader minioObjectReader;
    private final PdfTextExtractor pdfTextExtractor;
    private final TextCleaner textCleaner;
    private final ResumeParserProviderFactory providerFactory;
    private final ParsedDataValidator parsedDataValidator;

    private final ResumeParserEventProducer eventProducer;
    private final StringRedisTemplate redisTemplate;
    private final ResumeMatchAnalysisService resumeMatchAnalysisService;

    @Async
    public void run(UUID parsedResumeId, String lockKey) {
        try {
            parsedResumeRepository.findById(parsedResumeId).ifPresent(this::runPipeline);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void runPipeline(ParsedResume parsedResume) {
        UUID resumeId = parsedResume.getResumeId();
        try {
            CandidateDto candidate = candidateServiceClient.getCandidateSummary(parsedResume.getCandidateId()).getData();
            if (candidate == null || !"ACTIVE".equalsIgnoreCase(candidate.getStatus())) {
                throw new ResumeParsingPipelineException("Candidate is not ACTIVE");
            }

            ResumeMetadataDto metadata = resumeServiceClient.getResumeMetadata(resumeId).getData();
            if (metadata == null) {
                throw new ResumeParsingPipelineException("Resume metadata not found in Resume Service");
            }
            ResumeObjectDetailsDto objectDetails = resumeServiceClient.getObjectDetails(resumeId).getData();
            if (objectDetails == null) {
                throw new ResumeParsingPipelineException("Resume object coordinates not found in Resume Service");
            }

            parsedResume = updateStatus(parsedResume, ParsingStatus.EXTRACTING_TEXT);
            writeLog(parsedResume, LogLevel.INFO, "Downloading PDF from storage");
            byte[] pdfBytes = minioObjectReader.readObject(objectDetails.getBucketName(), objectDetails.getObjectName());
            String rawText = pdfTextExtractor.extractText(pdfBytes);
            String cleanedText = textCleaner.clean(rawText);
            writeLog(parsedResume, LogLevel.INFO, "Text extracted (" + cleanedText.length() + " characters)");

            parsedResume = updateStatus(parsedResume, ParsingStatus.PARSING_FIELDS);
            ResumeParserProvider provider = providerFactory.getActiveProvider();
            writeLog(parsedResume, LogLevel.INFO, "Sending resume text to " + provider.getProviderName());
            ParsedResumeData data = provider.parse(cleanedText);
            parsedDataValidator.validate(data);
            writeLog(parsedResume, LogLevel.INFO, "Structured data received and validated");

            persistParsedData(parsedResume, data);
            AiProvider providerUsed = AiProvider.valueOf(provider.getProviderName());
            parsedResume.setProviderUsed(providerUsed);
            parsedResume.setStatus(ParsingStatus.PARSED);
            parsedResume.setParsedAt(LocalDateTime.now());
            parsedResume.setFailureReason(null);
            try {
                parsedResumeRepository.save(parsedResume);
            } catch (ObjectOptimisticLockingFailureException conflict) {
                log.warn("Version conflict on final save for ParsedResume {} -- re-fetching and retrying once", parsedResume.getId());
                ParsedResume fresh = parsedResumeRepository.findById(parsedResume.getId()).orElseThrow(() -> conflict);
                fresh.setProviderUsed(providerUsed);
                fresh.setStatus(ParsingStatus.PARSED);
                fresh.setParsedAt(LocalDateTime.now());
                fresh.setFailureReason(null);
                parsedResumeRepository.save(fresh);
            }
            writeLog(parsedResume, LogLevel.INFO, "Parsing completed successfully");

            eventProducer.publishResumeParsed(ResumeParsedEvent.builder()
                    .resumeId(resumeId)
                    .candidateId(parsedResume.getCandidateId())
                    .parsedResumeId(parsedResume.getId())
                    .occurredAt(LocalDateTime.now())
                    .build());

            // Resumes any resume_match rows parked in AWAITING_PARSE for
            // this resumeId now that parsing has actually finished.
            resumeMatchAnalysisService.onResumeParsed(resumeId);

        } catch (Throwable e) {
            // Throwable, not Exception: an OutOfMemoryError (or any other Error) here would
            // otherwise skip this whole block silently -- the async thread just dies with no
            // log line and the row sits at EXTRACTING_TEXT/PARSING_FIELDS forever with no
            // failure recorded at all. (This is exactly what happened before the JVM heap
            // for this service was sized up -- see docker-compose.yml's jvm-medium comment.)
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Parsing failed for resume {}: {}", resumeId, reason, e);
            parsedResume.setStatus(ParsingStatus.FAILED);
            parsedResume.setFailureReason(reason);
            try {
                parsedResumeRepository.save(parsedResume);
            } catch (ObjectOptimisticLockingFailureException conflict) {
                ParsedResume fresh = parsedResumeRepository.findById(parsedResume.getId()).orElse(null);
                if (fresh != null) {
                    fresh.setStatus(ParsingStatus.FAILED);
                    fresh.setFailureReason(reason);
                    parsedResumeRepository.save(fresh);
                }
            }
            writeLog(parsedResume, LogLevel.ERROR, "Parsing failed: " + reason);

            eventProducer.publishResumeParsingFailed(ResumeParsingFailedEvent.builder()
                    .resumeId(resumeId)
                    .candidateId(parsedResume.getCandidateId())
                    .reason(reason)
                    .occurredAt(LocalDateTime.now())
                    .build());
        }
    }

    @Transactional
    protected void persistParsedData(ParsedResume parsedResume, ParsedResumeData data) {
        UUID id = parsedResume.getId();
        // A retry fully replaces the previous parse's child rows rather
        // than merging with them -- there is no partial-edit concept for
        // system-generated data.
        skillRepository.deleteAllByParsedResumeId(id);
        experienceRepository.deleteAllByParsedResumeId(id);
        projectRepository.deleteAllByParsedResumeId(id);
        educationRepository.deleteAllByParsedResumeId(id);
        certificationRepository.deleteAllByParsedResumeId(id);
        achievementRepository.deleteAllByParsedResumeId(id);
        languageRepository.deleteAllByParsedResumeId(id);

        parsedResume.setFullName(data.fullName());
        parsedResume.setEmail(data.email());
        parsedResume.setPhone(data.phone());
        parsedResume.setLocation(data.location());
        parsedResume.setLinkedinUrl(data.linkedinUrl());
        parsedResume.setGithubUrl(data.githubUrl());
        parsedResume.setPortfolioUrl(data.portfolioUrl());
        parsedResume.setProfessionalSummary(data.professionalSummary());
        parsedResume.setCurrentCompany(data.currentCompany());
        parsedResume.setCurrentDesignation(data.currentDesignation());
        parsedResume.setTotalExperienceYears(data.totalExperienceYears() != null
                ? BigDecimal.valueOf(data.totalExperienceYears()) : null);
        parsedResume.setNoticePeriod(data.noticePeriod());
        parsedResume.setExpectedSalary(data.expectedSalary());

        if (data.skills() != null) {
            List<CandidateSkill> skills = data.skills().stream()
                    .map(s -> CandidateSkill.builder()
                            .parsedResumeId(id)
                            .skillName(s.name())
                            .skillCategory(toSkillCategory(s.category()))
                            .build())
                    .toList();
            skillRepository.saveAll(skills);
        }
        if (data.experience() != null) {
            List<CandidateExperience> experience = new java.util.ArrayList<>();
            int order = 0;
            for (var e : data.experience()) {
                experience.add(CandidateExperience.builder()
                        .parsedResumeId(id).companyName(e.companyName()).designation(e.designation())
                        .startDate(e.startDate()).endDate(e.endDate()).current(e.isCurrent())
                        .description(e.description()).displayOrder(order++).build());
            }
            experienceRepository.saveAll(experience);
        }
        if (data.projects() != null) {
            List<CandidateProject> projects = new java.util.ArrayList<>();
            int order = 0;
            for (var p : data.projects()) {
                projects.add(CandidateProject.builder()
                        .parsedResumeId(id).projectName(p.name()).description(p.description())
                        .technologies(p.technologies()).displayOrder(order++).build());
            }
            projectRepository.saveAll(projects);
        }
        if (data.education() != null) {
            List<CandidateEducation> education = new java.util.ArrayList<>();
            int order = 0;
            for (var ed : data.education()) {
                education.add(CandidateEducation.builder()
                        .parsedResumeId(id).institutionName(ed.institutionName()).degree(ed.degree())
                        .fieldOfStudy(ed.fieldOfStudy()).startDate(ed.startDate()).endDate(ed.endDate())
                        .grade(ed.grade()).displayOrder(order++).build());
            }
            educationRepository.saveAll(education);
        }
        if (data.certifications() != null) {
            List<CandidateCertification> certifications = new java.util.ArrayList<>();
            int order = 0;
            for (var c : data.certifications()) {
                certifications.add(CandidateCertification.builder()
                        .parsedResumeId(id).certificationName(c.name()).issuingOrganization(c.issuingOrganization())
                        .issuedDate(c.issuedDate()).expiryDate(c.expiryDate()).credentialId(c.credentialId())
                        .displayOrder(order++).build());
            }
            certificationRepository.saveAll(certifications);
        }
        if (data.achievements() != null) {
            List<CandidateAchievement> achievements = new java.util.ArrayList<>();
            int order = 0;
            for (var a : data.achievements()) {
                achievements.add(CandidateAchievement.builder()
                        .parsedResumeId(id).title(a.title()).description(a.description())
                        .displayOrder(order++).build());
            }
            achievementRepository.saveAll(achievements);
        }
        if (data.languages() != null) {
            List<CandidateLanguage> languages = data.languages().stream()
                    .map(l -> CandidateLanguage.builder()
                            .parsedResumeId(id).languageName(l.name()).proficiency(l.proficiency()).build())
                    .toList();
            languageRepository.saveAll(languages);
        }
    }

    private SkillCategory toSkillCategory(String raw) {
        if (raw == null) {
            return SkillCategory.TECHNICAL;
        }
        try {
            return SkillCategory.valueOf(raw.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return SkillCategory.TECHNICAL;
        }
    }

    /**
     * Returns the saved (possibly re-fetched) entity -- callers must keep using
     * this return value, not their old reference. On a version conflict
     * (observed in practice on this row across repeated retries/requeues), a
     * stale in-memory copy is re-read fresh from the DB and the same status
     * change re-applied once, rather than let the whole pipeline run die on
     * what's ultimately just a "someone else already advanced this row"
     * situation with nothing left to actually lose.
     */
    private ParsedResume updateStatus(ParsedResume parsedResume, ParsingStatus status) {
        parsedResume.setStatus(status);
        try {
            return parsedResumeRepository.save(parsedResume);
        } catch (ObjectOptimisticLockingFailureException e) {
            log.warn("Version conflict updating ParsedResume {} to {} -- re-fetching and retrying once", parsedResume.getId(), status);
            ParsedResume fresh = parsedResumeRepository.findById(parsedResume.getId())
                    .orElseThrow(() -> e);
            fresh.setStatus(status);
            return parsedResumeRepository.save(fresh);
        }
    }

    private void writeLog(ParsedResume parsedResume, LogLevel level, String message) {
        parserLogRepository.save(ParserLog.builder()
                .parsedResumeId(parsedResume.getId())
                .resumeId(parsedResume.getResumeId())
                .logLevel(level)
                .message(message)
                .build());
    }
}
