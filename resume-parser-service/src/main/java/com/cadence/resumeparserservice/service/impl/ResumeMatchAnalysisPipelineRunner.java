package com.cadence.resumeparserservice.service.impl;

import com.cadence.resumeparserservice.constants.*;
import com.cadence.resumeparserservice.entity.*;
import com.cadence.resumeparserservice.exception.ResumeParsingPipelineException;
import com.cadence.resumeparserservice.feign.JobServiceClient;
import com.cadence.resumeparserservice.feign.dto.JobDetailDto;
import com.cadence.resumeparserservice.feign.dto.JobSkillDto;
import com.cadence.resumeparserservice.kafka.event.ResumeAnalysisFailedEvent;
import com.cadence.resumeparserservice.kafka.event.ResumeAnalyzedEvent;
import com.cadence.resumeparserservice.kafka.producer.ResumeParserEventProducer;
import com.cadence.resumeparserservice.provider.JobRequirementsSnapshot;
import com.cadence.resumeparserservice.provider.MatchAnalysisData;
import com.cadence.resumeparserservice.provider.ParsedResumeSnapshot;
import com.cadence.resumeparserservice.provider.ResumeParserProvider;
import com.cadence.resumeparserservice.repository.*;
import com.cadence.resumeparserservice.strategy.ResumeParserProviderFactory;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * The actual matching pipeline (fetch parsed resume -> fetch job
 * requirements -> AI compare -> persist 5 tables -> publish), running
 * off the calling thread via @Async. A separate bean from
 * ResumeMatchAnalysisServiceImpl on purpose -- see that class's
 * Javadoc, same reasoning as ResumeParsingPipelineRunner.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeMatchAnalysisPipelineRunner {

    private final ResumeMatchRepository resumeMatchRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final CandidateSkillRepository skillRepository;
    private final CandidateExperienceRepository experienceRepository;
    private final CandidateProjectRepository projectRepository;
    private final CandidateEducationRepository educationRepository;
    private final CandidateCertificationRepository certificationRepository;

    private final SkillMatchRepository skillMatchRepository;
    private final MissingSkillRepository missingSkillRepository;
    private final AiRecommendationRepository aiRecommendationRepository;
    private final ResumeMatchNoteRepository resumeMatchNoteRepository;

    private final JobServiceClient jobServiceClient;
    private final ResumeParserProviderFactory providerFactory;
    private final ResumeParserEventProducer eventProducer;
    private final StringRedisTemplate redisTemplate;

    @Async
    public void run(UUID resumeMatchId, String lockKey) {
        try {
            resumeMatchRepository.findById(resumeMatchId).ifPresent(this::runPipeline);
        } finally {
            redisTemplate.delete(lockKey);
        }
    }

    private void runPipeline(ResumeMatch resumeMatch) {
        UUID applicationId = resumeMatch.getApplicationId();
        try {
            ParsedResume parsedResume = parsedResumeRepository.findById(resumeMatch.getParsedResumeId())
                    .orElseThrow(() -> new ResumeParsingPipelineException("Parsed resume not found: " + resumeMatch.getParsedResumeId()));
            ParsedResumeSnapshot resumeSnapshot = buildResumeSnapshot(parsedResume);

            JobDetailDto jobDetail = jobServiceClient.getJobDetail(resumeMatch.getJobId()).getData();
            if (jobDetail == null) {
                throw new ResumeParsingPipelineException("Job not found in Job Service: " + resumeMatch.getJobId());
            }
            resumeMatch.setDepartmentId(jobDetail.getDepartmentId());
            JobRequirementsSnapshot jobSnapshot = buildJobSnapshot(jobDetail);

            ResumeParserProvider provider = providerFactory.getActiveProvider();
            MatchAnalysisData data = provider.analyzeMatch(resumeSnapshot, jobSnapshot);

            persistMatchData(resumeMatch, data);
            resumeMatch.setProviderUsed(AiProvider.valueOf(provider.getProviderName()));
            resumeMatch.setStatus(ResumeMatchStatus.ANALYZED);
            resumeMatch.setAnalyzedAt(LocalDateTime.now());
            resumeMatch.setFailureReason(null);
            resumeMatchRepository.save(resumeMatch);

            eventProducer.publishResumeAnalyzed(ResumeAnalyzedEvent.builder()
                    .applicationId(applicationId)
                    .jobId(resumeMatch.getJobId())
                    .candidateId(resumeMatch.getCandidateId())
                    .resumeMatchId(resumeMatch.getId())
                    .overallMatchScore(resumeMatch.getOverallMatchScore())
                    .occurredAt(LocalDateTime.now())
                    .build());

        } catch (Throwable e) {
            // Throwable, not Exception: an OutOfMemoryError (or any other Error) here would
            // otherwise skip this whole block silently -- the async thread just dies with no
            // log line and the row sits at ANALYZING/AWAITING_PARSE forever with no failure
            // recorded at all.
            String reason = e.getMessage() != null ? e.getMessage() : e.getClass().getSimpleName();
            log.warn("Match analysis failed for application {}: {}", applicationId, reason, e);
            resumeMatch.setStatus(ResumeMatchStatus.FAILED);
            resumeMatch.setFailureReason(reason);
            resumeMatchRepository.save(resumeMatch);

            eventProducer.publishResumeAnalysisFailed(ResumeAnalysisFailedEvent.builder()
                    .applicationId(applicationId)
                    .jobId(resumeMatch.getJobId())
                    .candidateId(resumeMatch.getCandidateId())
                    .reason(reason)
                    .occurredAt(LocalDateTime.now())
                    .build());
        }
    }

    private ParsedResumeSnapshot buildResumeSnapshot(ParsedResume parsedResume) {
        UUID id = parsedResume.getId();
        List<String> skills = skillRepository.findAllByParsedResumeId(id).stream()
                .map(CandidateSkill::getSkillName).toList();
        List<String> experienceSummaries = experienceRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id).stream()
                .map(e -> String.join(" - ", nullToEmpty(e.getDesignation()), nullToEmpty(e.getCompanyName()),
                        (e.isCurrent() ? "Present" : nullToEmpty(e.getEndDate()))))
                .toList();
        List<String> educationSummaries = educationRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id).stream()
                .map(ed -> String.join(" - ", nullToEmpty(ed.getDegree()), nullToEmpty(ed.getFieldOfStudy()), nullToEmpty(ed.getInstitutionName())))
                .toList();
        List<String> certificationNames = certificationRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id).stream()
                .map(CandidateCertification::getCertificationName).toList();
        List<String> projectSummaries = projectRepository.findAllByParsedResumeIdOrderByDisplayOrderAsc(id).stream()
                .map(p -> String.join(" - ", nullToEmpty(p.getProjectName()), nullToEmpty(p.getTechnologies())))
                .toList();

        return new ParsedResumeSnapshot(
                parsedResume.getFullName(),
                parsedResume.getProfessionalSummary(),
                skills,
                parsedResume.getTotalExperienceYears(),
                experienceSummaries,
                educationSummaries,
                certificationNames,
                projectSummaries);
    }

    private JobRequirementsSnapshot buildJobSnapshot(JobDetailDto jobDetail) {
        List<JobSkillDto> skills = jobDetail.getRequirements() != null && jobDetail.getRequirements().getSkills() != null
                ? jobDetail.getRequirements().getSkills() : List.of();
        List<String> required = skills.stream()
                .filter(s -> "REQUIRED".equalsIgnoreCase(s.getSkillType()))
                .map(JobSkillDto::getSkillName).toList();
        List<String> preferred = skills.stream()
                .filter(s -> "PREFERRED".equalsIgnoreCase(s.getSkillType()))
                .map(JobSkillDto::getSkillName).toList();

        return new JobRequirementsSnapshot(
                jobDetail.getTitle(),
                jobDetail.getRequirements() != null ? jobDetail.getRequirements().getMinExperienceYears() : null,
                jobDetail.getRequirements() != null ? jobDetail.getRequirements().getMaxExperienceYears() : null,
                required,
                preferred,
                jobDetail.getRequirements() != null ? jobDetail.getRequirements().getEducation() : null,
                jobDetail.getRequirements() != null ? jobDetail.getRequirements().getCertifications() : null);
    }

    @Transactional
    protected void persistMatchData(ResumeMatch resumeMatch, MatchAnalysisData data) {
        UUID id = resumeMatch.getId();
        // Same "full replace, no partial edit" precedent as the parsing
        // pipeline's persistParsedData -- a recalculate fully supersedes
        // the previous analysis's child rows.
        skillMatchRepository.deleteAllByResumeMatchId(id);
        missingSkillRepository.deleteAllByResumeMatchId(id);
        resumeMatchNoteRepository.deleteAllByResumeMatchId(id);
        aiRecommendationRepository.deleteAllByResumeMatchId(id);

        resumeMatch.setOverallMatchScore(data.overallMatchScore());
        resumeMatch.setTechnicalSkillScore(data.technicalSkillScore());
        resumeMatch.setProgrammingLanguageScore(data.programmingLanguageScore());
        resumeMatch.setFrameworkScore(data.frameworkScore());
        resumeMatch.setDatabaseScore(data.databaseScore());
        resumeMatch.setCloudScore(data.cloudScore());
        resumeMatch.setDevopsScore(data.devopsScore());
        resumeMatch.setExperienceMatchLabel(toEnum(ExperienceMatchLabel.class, data.experienceMatchLabel(), ExperienceMatchLabel.ADEQUATE));
        resumeMatch.setProjectMatchScore(data.projectMatchScore());
        resumeMatch.setEducationMatchLabel(toEnum(EducationMatchLabel.class, data.educationMatchLabel(), EducationMatchLabel.PARTIAL));
        resumeMatch.setCertificationMatchLabel(toEnum(CertificationMatchLabel.class, data.certificationMatchLabel(), CertificationMatchLabel.NONE));
        resumeMatch.setCommunicationPrediction(data.communicationPrediction());
        resumeMatch.setProblemSolvingPrediction(data.problemSolvingPrediction());
        resumeMatch.setLearningAbilityPrediction(data.learningAbilityPrediction());

        if (data.matchedSkills() != null) {
            List<SkillMatch> matched = data.matchedSkills().stream()
                    .map(s -> SkillMatch.builder().resumeMatchId(id).skillName(s.name())
                            .skillCategory(toSkillCategory(s.category())).build())
                    .toList();
            skillMatchRepository.saveAll(matched);
        }
        if (data.missingSkills() != null) {
            List<MissingSkill> missing = data.missingSkills().stream()
                    .map(s -> MissingSkill.builder().resumeMatchId(id).skillName(s.name())
                            .skillCategory(toSkillCategory(s.category())).required(s.required()).build())
                    .toList();
            missingSkillRepository.saveAll(missing);
        }
        if (data.strengths() != null) {
            saveNotes(id, NoteType.STRENGTH, data.strengths());
        }
        if (data.weaknesses() != null) {
            saveNotes(id, NoteType.WEAKNESS, data.weaknesses());
        }

        aiRecommendationRepository.save(AiRecommendation.builder()
                .resumeMatchId(id)
                .hiringRecommendation(toEnum(HiringRecommendation.class, data.hiringRecommendation(), HiringRecommendation.HOLD))
                .overallAiSummary(data.overallAiSummary())
                .recommendedLearningTopics(data.recommendedLearningTopics())
                .build());
    }

    private void saveNotes(UUID resumeMatchId, NoteType noteType, List<String> descriptions) {
        List<ResumeMatchNote> notes = new java.util.ArrayList<>();
        int order = 0;
        for (String description : descriptions) {
            notes.add(ResumeMatchNote.builder()
                    .resumeMatchId(resumeMatchId).noteType(noteType).description(description).displayOrder(order++).build());
        }
        resumeMatchNoteRepository.saveAll(notes);
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

    private <E extends Enum<E>> E toEnum(Class<E> enumType, String raw, E fallback) {
        if (raw == null) {
            return fallback;
        }
        try {
            return Enum.valueOf(enumType, raw.trim().toUpperCase().replace(' ', '_').replace('-', '_'));
        } catch (IllegalArgumentException e) {
            return fallback;
        }
    }

    private String nullToEmpty(String value) {
        return value != null ? value : "";
    }
}
