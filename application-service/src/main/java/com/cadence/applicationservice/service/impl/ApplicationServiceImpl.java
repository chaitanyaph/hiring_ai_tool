package com.cadence.applicationservice.service.impl;

import com.cadence.applicationservice.client.CandidateServiceClient;
import com.cadence.applicationservice.client.JobServiceClient;
import com.cadence.applicationservice.client.ResumeServiceClient;
import com.cadence.applicationservice.client.dto.CandidateDto;
import com.cadence.applicationservice.client.dto.JobDto;
import com.cadence.applicationservice.client.dto.ResumeDto;
import com.cadence.applicationservice.config.RedisConfig;
import com.cadence.applicationservice.constant.ApplicationStage;
import com.cadence.applicationservice.constant.ApplicationStatus;
import com.cadence.applicationservice.constant.HiringRecommendation;
import com.cadence.applicationservice.constant.InterviewType;
import com.cadence.applicationservice.constant.PlatformRole;
import com.cadence.applicationservice.constant.ScoreType;
import com.cadence.applicationservice.constant.ShortlistDecision;
import com.cadence.applicationservice.dto.request.*;
import com.cadence.applicationservice.dto.response.*;
import com.cadence.applicationservice.entity.*;
import com.cadence.applicationservice.exception.*;
import com.cadence.applicationservice.kafka.event.*;
import com.cadence.applicationservice.kafka.producer.ApplicationEventProducer;
import com.cadence.applicationservice.mapper.ApplicationMapper;
import com.cadence.applicationservice.repository.*;
import com.cadence.applicationservice.security.CurrentUser;
import com.cadence.applicationservice.service.ApplicationLifecycleEventService;
import com.cadence.applicationservice.service.ApplicationService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Implements both the human-driven half of the lifecycle
 * (ApplicationService: apply/withdraw/assign/status-change/notes) and
 * the system-driven half (ApplicationLifecycleEventService: reacting
 * to Kafka events from other services) in one class, since both
 * ultimately mutate the same Application aggregate through the same
 * transitionStatus() guard and share the same history-recording
 * helpers -- splitting them into two classes would only duplicate that
 * shared logic or force an awkward third collaborator.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService, ApplicationLifecycleEventService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository statusHistoryRepository;
    private final ApplicationStageHistoryRepository stageHistoryRepository;
    private final ApplicationScoreRepository scoreRepository;
    private final ApplicationNoteRepository noteRepository;
    private final ApplicationEventRepository eventRepository;
    private final JobServiceClient jobServiceClient;
    private final CandidateServiceClient candidateServiceClient;
    private final ResumeServiceClient resumeServiceClient;
    private final ApplicationMapper applicationMapper;
    private final ApplicationEventProducer eventProducer;
    private final CacheManager cacheManager;
    private final ObjectMapper objectMapper;

    @Value("${app.application.min-profile-completion-percent:60}")
    private int minProfileCompletionPercent;

    // ============================================================
    // Candidate-facing
    // ============================================================

    @Override
    @Transactional
    public ApplicationResponse apply(CurrentUser candidate, ApplyRequest request) {
        if (applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), request.getJobId())) {
            throw new DuplicateApplicationException();
        }

        JobDto job = fetchJobOrThrow(request.getJobId());
        if (!"PUBLISHED".equalsIgnoreCase(job.getStatus())) {
            throw new ApplicationValidationException(ErrorCode.JOB_NOT_PUBLISHED, "This job is not currently accepting applications");
        }
        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new ApplicationValidationException(ErrorCode.APPLICATION_DEADLINE_PASSED, "The application deadline for this job has passed");
        }

        CandidateDto candidateProfile = fetchCandidateOrThrow(candidate.getUserId());
        int completion = candidateProfile.getProfileCompletionPercent() == null ? 0 : candidateProfile.getProfileCompletionPercent();
        if (completion < minProfileCompletionPercent) {
            throw new ApplicationValidationException(ErrorCode.CANDIDATE_PROFILE_INCOMPLETE,
                    "Complete at least " + minProfileCompletionPercent + "% of your profile before applying (currently " + completion + "%)");
        }

        validateResumeOwnership(request.getResumeId(), candidate.getUserId());

        Application application = Application.builder()
                .companyId(job.getCompanyId())
                .jobId(job.getId())
                .candidateId(candidate.getUserId())
                .candidateNameSnapshot(candidateProfile.getFullName())
                .candidateEmailSnapshot(candidateProfile.getEmail())
                .jobTitleSnapshot(job.getTitle())
                .resumeId(request.getResumeId())
                .currentStatus(ApplicationStatus.APPLIED)
                .currentStage(ApplicationStage.APPLICATION)
                .createdBy(candidate.getUserId())
                .updatedBy(candidate.getUserId())
                .build();
        application = applicationRepository.save(application);
        recordStatusHistory(application.getId(), null, ApplicationStatus.APPLIED, candidate.getUserId(), "Application submitted");

        // Resume parsing starts automatically the moment ApplicationCreated is published below.
        transitionStatus(application, ApplicationStatus.RESUME_PARSING, candidate.getUserId(), "Resume parsing started automatically");
        application = applicationRepository.save(application);

        eventProducer.publishApplicationCreated(ApplicationCreatedEvent.builder()
                .applicationId(application.getId()).companyId(application.getCompanyId())
                .jobId(application.getJobId()).candidateId(application.getCandidateId())
                .resumeId(application.getResumeId()).occurredAt(LocalDateTime.now()).build());

        return assembleResponse(application, false);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getForCandidate(CurrentUser candidate, UUID applicationId) {
        return assembleResponse(findForCandidateOrThrow(applicationId, candidate.getUserId()), false);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listMyApplications(CurrentUser candidate) {
        return applicationMapper.toResponseList(applicationRepository.findAll(
                ApplicationSpecifications.candidateId(candidate.getUserId())));
    }

    @Override
    @Transactional
    public ApplicationResponse withdraw(CurrentUser candidate, UUID applicationId) {
        Application application = findForCandidateOrThrow(applicationId, candidate.getUserId());
        transitionStatus(application, ApplicationStatus.WITHDRAWN, candidate.getUserId(), "Withdrawn by candidate");
        application.setUpdatedBy(candidate.getUserId());
        application = applicationRepository.save(application);

        eventProducer.publishApplicationWithdrawn(ApplicationWithdrawnEvent.builder()
                .applicationId(application.getId()).companyId(application.getCompanyId())
                .jobId(application.getJobId()).candidateId(candidate.getUserId())
                .occurredAt(LocalDateTime.now()).build());
        evictApplicationCache(application.getId());

        return assembleResponse(application, false);
    }

    @Override
    @Transactional
    public ApplicationResponse acceptOffer(CurrentUser candidate, UUID applicationId) {
        Application application = findForCandidateOrThrow(applicationId, candidate.getUserId());
        transitionStatus(application, ApplicationStatus.OFFER_ACCEPTED, candidate.getUserId(), "Offer accepted by candidate");
        transitionStatus(application, ApplicationStatus.HIRED, candidate.getUserId(), "Automatically marked HIRED upon offer acceptance");
        application.setUpdatedBy(candidate.getUserId());
        application = applicationRepository.save(application);

        eventProducer.publishOfferAccepted(OfferAcceptedEvent.builder()
                .applicationId(application.getId()).companyId(application.getCompanyId())
                .jobId(application.getJobId()).candidateId(candidate.getUserId())
                .occurredAt(LocalDateTime.now()).build());
        evictApplicationCache(application.getId());

        return assembleResponse(application, false);
    }

    @Override
    @Transactional
    public ApplicationResponse rejectOffer(CurrentUser candidate, UUID applicationId) {
        Application application = findForCandidateOrThrow(applicationId, candidate.getUserId());
        transitionStatus(application, ApplicationStatus.OFFER_DECLINED, candidate.getUserId(), "Offer declined by candidate");
        application.setUpdatedBy(candidate.getUserId());
        application = applicationRepository.save(application);

        eventProducer.publishOfferRejected(OfferRejectedEvent.builder()
                .applicationId(application.getId()).companyId(application.getCompanyId())
                .jobId(application.getJobId()).candidateId(candidate.getUserId())
                .occurredAt(LocalDateTime.now()).build());
        evictApplicationCache(application.getId());

        return assembleResponse(application, false);
    }

    // ============================================================
    // Recruiter-facing
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getForRecruiter(CurrentUser recruiter, UUID applicationId) {
        return assembleResponse(findForCompanyOrThrow(applicationId, recruiter.getCompanyId()), true);
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> searchCompanyApplications(CurrentUser recruiter, ApplicationSearchCriteria criteria, Pageable pageable) {
        Specification<Application> spec = Specification.where(ApplicationSpecifications.companyId(recruiter.getCompanyId()))
                .and(ApplicationSpecifications.jobId(criteria.getJobId()))
                .and(ApplicationSpecifications.recruiterId(criteria.getRecruiterId()))
                .and(ApplicationSpecifications.hiringManagerId(criteria.getHiringManagerId()))
                .and(ApplicationSpecifications.status(criteria.getStatus()))
                .and(ApplicationSpecifications.stage(criteria.getStage()))
                .and(ApplicationSpecifications.priority(criteria.getPriority()))
                .and(ApplicationSpecifications.candidateNameContains(criteria.getCandidateName()))
                .and(ApplicationSpecifications.candidateEmailContains(criteria.getCandidateEmail()))
                .and(ApplicationSpecifications.jobTitleContains(criteria.getJobTitle()))
                .and(ApplicationSpecifications.minOverallScore(criteria.getMinOverallScore()))
                .and(ApplicationSpecifications.appliedBetween(criteria.getAppliedFrom(), criteria.getAppliedTo()));

        Page<Application> page = applicationRepository.findAll(spec, pageable);
        return PagedResponse.from(page.map(app -> applicationMapper.toResponse(app)));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<ApplicationResponse> getJobApplications(CurrentUser recruiter, UUID jobId, Pageable pageable) {
        Specification<Application> spec = Specification.where(ApplicationSpecifications.companyId(recruiter.getCompanyId()))
                .and(ApplicationSpecifications.jobId(jobId));
        Page<Application> page = applicationRepository.findAll(spec, pageable);
        return PagedResponse.from(page.map(app -> applicationMapper.toResponse(app)));
    }

    @Override
    @Transactional
    public ApplicationResponse changeStatus(CurrentUser recruiter, UUID applicationId, StatusChangeRequest request) {
        requireWriteAccess(recruiter);
        Application application = findForCompanyOrThrow(applicationId, recruiter.getCompanyId());

        transitionStatus(application, request.getToStatus(), recruiter.getUserId(), request.getReason());
        application.setUpdatedBy(recruiter.getUserId());
        application = applicationRepository.save(application);
        evictApplicationCache(application.getId());

        return assembleResponse(application, true);
    }

    @Override
    @Transactional
    public ApplicationResponse assignRecruiter(CurrentUser recruiter, UUID applicationId, AssignRecruiterRequest request) {
        requireWriteAccess(recruiter);
        Application application = findForCompanyOrThrow(applicationId, recruiter.getCompanyId());
        application.setAssignedRecruiterId(request.getRecruiterId());
        application.setUpdatedBy(recruiter.getUserId());
        application = applicationRepository.save(application);

        eventProducer.publishRecruiterAssigned(RecruiterAssignedEvent.builder()
                .applicationId(application.getId()).companyId(application.getCompanyId())
                .recruiterId(request.getRecruiterId()).occurredAt(LocalDateTime.now()).build());
        evictApplicationCache(application.getId());

        return assembleResponse(application, true);
    }

    @Override
    @Transactional
    public ApplicationResponse assignHiringManager(CurrentUser recruiter, UUID applicationId, AssignHiringManagerRequest request) {
        requireWriteAccess(recruiter);
        Application application = findForCompanyOrThrow(applicationId, recruiter.getCompanyId());
        application.setAssignedHiringManagerId(request.getHiringManagerId());
        application.setUpdatedBy(recruiter.getUserId());
        application = applicationRepository.save(application);

        eventProducer.publishHiringManagerAssigned(HiringManagerAssignedEvent.builder()
                .applicationId(application.getId()).companyId(application.getCompanyId())
                .hiringManagerId(request.getHiringManagerId()).occurredAt(LocalDateTime.now()).build());
        evictApplicationCache(application.getId());

        return assembleResponse(application, true);
    }

    @Override
    @Transactional
    public NoteResponse addNote(CurrentUser recruiter, UUID applicationId, AddNoteRequest request) {
        // Any recruiting-side role can leave a note, including a view-only Hiring Manager -- notes are
        // lower-risk than status/assignment changes, which is why this doesn't call requireWriteAccess.
        Application application = findForCompanyOrThrow(applicationId, recruiter.getCompanyId());
        ApplicationNote note = noteRepository.save(ApplicationNote.builder()
                .applicationId(application.getId())
                .authorId(recruiter.getUserId())
                .note(request.getNote())
                .build());
        return applicationMapper.toResponse(note);
    }

    // ============================================================
    // Shared: timeline / history
    // ============================================================

    @Override
    @Transactional(readOnly = true)
    public List<TimelineEntryResponse> getTimeline(CurrentUser caller, UUID applicationId) {
        Application application = findAccessibleOrThrow(caller, applicationId);

        List<TimelineEntryResponse> timeline = new ArrayList<>();
        statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAsc(application.getId()).forEach(h ->
                timeline.add(TimelineEntryResponse.builder().type("STATUS")
                        .from(h.getFromStatus() == null ? null : h.getFromStatus().name())
                        .to(h.getToStatus().name()).changedBy(h.getChangedBy())
                        .changedAt(h.getChangedAt()).reason(h.getReason()).build()));
        stageHistoryRepository.findAllByApplicationIdOrderByChangedAtAsc(application.getId()).forEach(h ->
                timeline.add(TimelineEntryResponse.builder().type("STAGE")
                        .from(h.getFromStage() == null ? null : h.getFromStage().name())
                        .to(h.getToStage().name()).changedBy(h.getChangedBy())
                        .changedAt(h.getChangedAt()).reason(h.getReason()).build()));

        timeline.sort(Comparator.comparing(TimelineEntryResponse::getChangedAt));
        return timeline;
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationHistoryResponse getHistory(CurrentUser caller, UUID applicationId) {
        Application application = findAccessibleOrThrow(caller, applicationId);
        return ApplicationHistoryResponse.builder()
                .statusHistory(applicationMapper.toStatusHistoryResponseList(
                        statusHistoryRepository.findAllByApplicationIdOrderByChangedAtAsc(application.getId())))
                .stageHistory(applicationMapper.toStageHistoryResponseList(
                        stageHistoryRepository.findAllByApplicationIdOrderByChangedAtAsc(application.getId())))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isResumeInUse(UUID resumeId) {
        List<ApplicationStatus> terminalStatuses = Arrays.stream(ApplicationStatus.values())
                .filter(ApplicationStatus::isTerminal).toList();
        return applicationRepository.existsByResumeIdAndCurrentStatusNotIn(resumeId, terminalStatuses);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean hasApplicationFromCandidateToCompany(UUID candidateId, UUID companyId) {
        return applicationRepository.existsByCandidateIdAndCompanyId(candidateId, companyId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> getApplicationsByJobInternal(UUID jobId) {
        Specification<Application> spec = Specification.where(ApplicationSpecifications.jobId(jobId));
        return applicationRepository.findAll(spec).stream().map(applicationMapper::toResponse).toList();
    }

    // ============================================================
    // ApplicationLifecycleEventService -- system-driven (Kafka)
    // ============================================================

    @Override
    @Transactional
    public void handleResumeParsed(UUID applicationId, UUID resumeId) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("ResumeParsed event for unknown application {}", applicationId);
            return;
        }
        if (application.getCurrentStatus() != ApplicationStatus.RESUME_PARSING) {
            log.warn("Ignoring ResumeParsed for application {} in unexpected status {}", applicationId, application.getCurrentStatus());
            return;
        }
        application.setResumeId(resumeId);
        transitionStatus(application, ApplicationStatus.RESUME_PARSED, null, "Resume parsed");
        transitionStatus(application, ApplicationStatus.AI_MATCHING, null, "Resume matching started automatically");
        applicationRepository.save(application);
        recordConsumedEvent(applicationId, "ResumeParsed", Map.of("resumeId", String.valueOf(resumeId)));
    }

    @Override
    @Transactional
    public void handleResumeMatched(UUID applicationId, Integer matchScore) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("ResumeMatched event for unknown application {}", applicationId);
            return;
        }
        // Performs the full RESUME_PARSING -> RESUME_PARSED -> AI_MATCHING -> AI_MATCHED
        // chain here rather than relying on a separate ResumeParsed event to advance
        // through the middle two steps first: resume-parser-service's actual
        // ResumeParsedEvent has no applicationId at all (parsing is scoped to a resume,
        // not a specific application), so a listener keyed on applicationId can never
        // resolve it. This event -- the one resume-parser-service actually finishes
        // match analysis with -- is the only one that carries both applicationId and a
        // score, and by construction can't fire until parsing has already completed.
        if (application.getCurrentStatus() == ApplicationStatus.RESUME_PARSING) {
            transitionStatus(application, ApplicationStatus.RESUME_PARSED, null, "Resume parsed");
            transitionStatus(application, ApplicationStatus.AI_MATCHING, null, "Resume matching started automatically");
        } else if (application.getCurrentStatus() != ApplicationStatus.AI_MATCHING) {
            log.warn("Ignoring ResumeMatched for application {} in unexpected status {}", applicationId, application.getCurrentStatus());
            return;
        }
        application.setResumeMatchScore(matchScore);
        application.setOverallScore(ScoreCalculator.recomputeOverall(application));
        transitionStatus(application, ApplicationStatus.AI_MATCHED, null, "Resume match score received: " + matchScore);
        applicationRepository.save(application);
        recordScore(applicationId, ScoreType.RESUME_MATCH, matchScore, "resume-matching-service");
        recordConsumedEvent(applicationId, "ResumeMatched", Map.of("matchScore", String.valueOf(matchScore)));
    }

    @Override
    @Transactional
    public void handleCandidateShortlisted(UUID applicationId, ShortlistDecision decision, Integer overallMatchScore) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("CandidateShortlisted event for unknown application {}", applicationId);
            return;
        }
        // AI_MATCHED: the initial automatic decision. MANUAL_REVIEW: a recruiter
        // resolving an earlier hold -- ai-interview-service republishes this same
        // event (with SHORTLISTED or REJECTED) when a recruiter acts on the
        // manual-review queue, so this must accept both starting states.
        if (application.getCurrentStatus() != ApplicationStatus.AI_MATCHED
                && application.getCurrentStatus() != ApplicationStatus.MANUAL_REVIEW) {
            log.warn("Ignoring CandidateShortlisted for application {} in unexpected status {}", applicationId, application.getCurrentStatus());
            return;
        }
        String scoreSuffix = overallMatchScore != null ? " (score " + overallMatchScore + ")" : "";
        switch (decision) {
            case SHORTLISTED -> {
                transitionStatus(application, ApplicationStatus.SHORTLISTED, null, "Shortlisted by AI Shortlisting Service" + scoreSuffix);
                transitionStatus(application, ApplicationStatus.AI_INTERVIEW_PENDING, null, "AI interview invitation triggered automatically");
            }
            case REJECTED -> transitionStatus(application, ApplicationStatus.REJECTED, null, "AI screening did not recommend this candidate to proceed" + scoreSuffix);
            case MANUAL_REVIEW -> transitionStatus(application, ApplicationStatus.MANUAL_REVIEW, null, "AI screening flagged this application for manual recruiter review" + scoreSuffix);
        }
        applicationRepository.save(application);
        recordConsumedEvent(applicationId, "CandidateShortlisted", Map.of("decision", decision.name()));
    }

    @Override
    @Transactional
    public void handleInterviewCompleted(UUID applicationId, InterviewType interviewType, Integer score, String feedback, HiringRecommendation recommendation) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("InterviewCompleted event for unknown application {}", applicationId);
            return;
        }

        ApplicationStatus current = application.getCurrentStatus();
        boolean handled = switch (interviewType) {
            case AI -> {
                if (current != ApplicationStatus.AI_INTERVIEW_PENDING) yield false;
                application.setAiInterviewScore(score);
                application.setOverallScore(ScoreCalculator.recomputeOverall(application));
                transitionStatus(application, ApplicationStatus.AI_INTERVIEW_COMPLETED, null, feedback);
                // PROCEED auto-advances (no recruiter action required); REJECT auto-rejects;
                // HOLD (or an unrecognized/missing recommendation) deliberately stops here --
                // the recruiter reviews the evaluation report and moves the application on
                // manually via the existing generic PUT /{id}/status endpoint.
                if (recommendation == HiringRecommendation.REJECT) {
                    transitionStatus(application, ApplicationStatus.REJECTED, null, "AI interview evaluation recommended rejection");
                } else if (recommendation == HiringRecommendation.PROCEED) {
                    transitionStatus(application, ApplicationStatus.CODING_ASSESSMENT_PENDING, null, "Coding assessment triggered automatically");
                }
                if (score != null) recordScore(applicationId, ScoreType.AI_INTERVIEW, score, "ai-interview-service");
                yield true;
            }
            case TECHNICAL -> {
                if (current != ApplicationStatus.TECHNICAL_INTERVIEW) yield false;
                transitionStatus(application, ApplicationStatus.MANAGER_INTERVIEW, null, feedback);
                yield true;
            }
            case MANAGER -> {
                if (current != ApplicationStatus.MANAGER_INTERVIEW) yield false;
                transitionStatus(application, ApplicationStatus.HR_INTERVIEW, null, feedback);
                yield true;
            }
            case HR -> {
                if (current != ApplicationStatus.HR_INTERVIEW) yield false;
                transitionStatus(application, ApplicationStatus.BACKGROUND_VERIFICATION, null, feedback);
                yield true;
            }
        };

        if (!handled) {
            log.warn("Ignoring InterviewCompleted ({}) for application {} in unexpected status {}", interviewType, applicationId, current);
            return;
        }
        applicationRepository.save(application);
        recordConsumedEvent(applicationId, "InterviewCompleted:" + interviewType, Map.of("score", String.valueOf(score)));
    }

    @Override
    @Transactional
    public void handleCodingAssessmentCompleted(UUID applicationId, Integer score, Boolean passed) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("CodingAssessmentCompleted event for unknown application {}", applicationId);
            return;
        }
        if (application.getCurrentStatus() != ApplicationStatus.CODING_ASSESSMENT_PENDING) {
            log.warn("Ignoring CodingAssessmentCompleted for application {} in unexpected status {}", applicationId, application.getCurrentStatus());
            return;
        }
        application.setCodingScore(score);
        application.setOverallScore(ScoreCalculator.recomputeOverall(application));
        transitionStatus(application, ApplicationStatus.CODING_ASSESSMENT_COMPLETED, null, "Coding assessment completed, score: " + score);
        // Fail closed: a missing/malformed `passed` field must never silently auto-advance a candidate.
        boolean didPass = Boolean.TRUE.equals(passed);
        if (didPass) {
            transitionStatus(application, ApplicationStatus.TECHNICAL_INTERVIEW, null, "Technical interview scheduled automatically");
        } else {
            transitionStatus(application, ApplicationStatus.REJECTED, null, "Coding assessment score below the passing threshold");
        }
        applicationRepository.save(application);
        recordScore(applicationId, ScoreType.CODING, score, "coding-assessment-service");
        recordConsumedEvent(applicationId, "CodingAssessmentCompleted", Map.of("score", String.valueOf(score), "passed", String.valueOf(didPass)));
    }

    @Override
    @Transactional
    public void handleBackgroundVerificationCompleted(UUID applicationId, boolean passed, String remarks) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("BackgroundVerificationCompleted event for unknown application {}", applicationId);
            return;
        }
        if (application.getCurrentStatus() != ApplicationStatus.BACKGROUND_VERIFICATION) {
            log.warn("Ignoring BackgroundVerificationCompleted for application {} in unexpected status {}", applicationId, application.getCurrentStatus());
            return;
        }
        if (passed) {
            application.setRemarks(remarks);
            applicationRepository.save(application);
        } else {
            transitionStatus(application, ApplicationStatus.REJECTED, null, "Background verification failed: " + remarks);
            applicationRepository.save(application);
        }
        recordConsumedEvent(applicationId, "BackgroundVerificationCompleted", Map.of("passed", String.valueOf(passed)));
    }

    @Override
    @Transactional
    public void handleOfferReleased(UUID applicationId, UUID offerId) {
        Application application = applicationRepository.findById(applicationId).orElse(null);
        if (application == null) {
            log.warn("OfferReleased event for unknown application {}", applicationId);
            return;
        }
        if (application.getCurrentStatus() != ApplicationStatus.BACKGROUND_VERIFICATION) {
            log.warn("Ignoring OfferReleased for application {} in unexpected status {}", applicationId, application.getCurrentStatus());
            return;
        }
        transitionStatus(application, ApplicationStatus.OFFER_RELEASED, null, "Offer released" + (offerId != null ? " (offerId=" + offerId + ")" : ""));
        applicationRepository.save(application);
        recordConsumedEvent(applicationId, "OfferReleased", Map.of("offerId", String.valueOf(offerId)));
    }

    // ============================================================
    // Shared private helpers
    // ============================================================

    private void transitionStatus(Application application, ApplicationStatus target, UUID changedBy, String reason) {
        ApplicationStatus previous = application.getCurrentStatus();
        if (!previous.canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(previous, target);
        }
        application.setCurrentStatus(target);
        application.setLastStatusChangedAt(LocalDateTime.now());
        recordStatusHistory(application.getId(), previous, target, changedBy, reason);

        ApplicationStage targetStage = target.getDefaultStage();
        if (targetStage != null && targetStage != application.getCurrentStage()) {
            ApplicationStage previousStage = application.getCurrentStage();
            application.setCurrentStage(targetStage);
            stageHistoryRepository.save(ApplicationStageHistory.builder()
                    .applicationId(application.getId()).fromStage(previousStage).toStage(targetStage)
                    .changedBy(changedBy).reason(reason).build());
        }

        // Published from the one shared choke point every status change (manual recruiter
        // action or automatic Kafka-event-driven lifecycle transition) already flows through,
        // so notification-service can react to every transition -- not just the ones made via
        // the recruiter-facing changeStatus() endpoint, which used to publish this alone.
        eventProducer.publishApplicationStatusChanged(ApplicationStatusChangedEvent.builder()
                .applicationId(application.getId()).companyId(application.getCompanyId()).jobId(application.getJobId())
                .candidateId(application.getCandidateId())
                .fromStatus(previous).toStatus(target).occurredAt(LocalDateTime.now()).build());
    }

    private void recordStatusHistory(UUID applicationId, ApplicationStatus from, ApplicationStatus to, UUID changedBy, String reason) {
        statusHistoryRepository.save(ApplicationStatusHistory.builder()
                .applicationId(applicationId).fromStatus(from).toStatus(to).changedBy(changedBy).reason(reason).build());
    }

    private void recordScore(UUID applicationId, ScoreType type, Integer value, String source) {
        scoreRepository.save(ApplicationScore.builder()
                .applicationId(applicationId).scoreType(type).scoreValue(value).source(source).build());
    }

    private void recordConsumedEvent(UUID applicationId, String eventType, Object payload) {
        try {
            String json = objectMapper.writeValueAsString(payload);
            eventRepository.save(ApplicationEvent.builder()
                    .applicationId(applicationId).eventType(eventType).direction("CONSUMED").payload(json).build());
        } catch (Exception e) {
            log.warn("Could not serialize event payload for audit trail (event={}, applicationId={}): {}", eventType, applicationId, e.getMessage());
        }
    }

    private void requireWriteAccess(CurrentUser recruiter) {
        boolean fullWriteRole = recruiter.getRole() != null && PlatformRole.FULL_WRITE_ROLES.contains(recruiter.getRole());
        boolean hiringManagerWithEditGrant = PlatformRole.HIRING_MANAGER.equals(recruiter.getRole())
                && recruiter.hasPermission(PlatformRole.APPLICATION_EDIT_PERMISSION);
        if (!fullWriteRole && !hiringManagerWithEditGrant) {
            throw new AccessDeniedException("Hiring Manager is view-only unless granted the " + PlatformRole.APPLICATION_EDIT_PERMISSION + " permission");
        }
    }

    private Application findForCandidateOrThrow(UUID id, UUID candidateId) {
        return applicationRepository.findByIdAndCandidateId(id, candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
    }

    private Application findForCompanyOrThrow(UUID id, UUID companyId) {
        return applicationRepository.findByIdAndCompanyId(id, companyId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
    }

    /** Accessible to the owning candidate OR a recruiter from the same company -- same 404 either way. */
    private Application findAccessibleOrThrow(CurrentUser caller, UUID applicationId) {
        if (PlatformRole.CANDIDATE.equals(caller.getRole())) {
            return findForCandidateOrThrow(applicationId, caller.getUserId());
        }
        return findForCompanyOrThrow(applicationId, caller.getCompanyId());
    }

    private JobDto fetchJobOrThrow(UUID jobId) {
        try {
            var response = jobServiceClient.getJob(jobId);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException(ErrorCode.JOB_NOT_FOUND, "Job not found");
            }
            return response.getData();
        } catch (feign.FeignException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.JOB_NOT_FOUND, "Job not found");
        }
    }

    private void validateResumeOwnership(UUID resumeId, UUID candidateId) {
        ResumeDto resume;
        try {
            var response = resumeServiceClient.getResume(resumeId);
            if (response == null || response.getData() == null) {
                throw new ApplicationValidationException(ErrorCode.RESUME_NOT_FOUND, "Selected resume not found");
            }
            resume = response.getData();
        } catch (feign.FeignException.NotFound e) {
            throw new ApplicationValidationException(ErrorCode.RESUME_NOT_FOUND, "Selected resume not found");
        }
        if (!candidateId.equals(resume.getCandidateId())) {
            // Same 404-shaped error a truly-missing resume would give -- never reveal that a resumeId
            // belonging to a different candidate exists.
            throw new ApplicationValidationException(ErrorCode.RESUME_NOT_FOUND, "Selected resume not found");
        }
        if (!"ACTIVE".equalsIgnoreCase(resume.getStatus())) {
            throw new ApplicationValidationException(ErrorCode.RESUME_NOT_FOUND, "Selected resume is no longer available -- choose another");
        }
    }

    private CandidateDto fetchCandidateOrThrow(UUID candidateId) {
        try {
            var response = candidateServiceClient.getCandidateSummary(candidateId);
            if (response == null || response.getData() == null) {
                throw new ResourceNotFoundException(ErrorCode.CANDIDATE_NOT_FOUND, "Candidate profile not found");
            }
            return response.getData();
        } catch (feign.FeignException.NotFound e) {
            throw new ResourceNotFoundException(ErrorCode.CANDIDATE_NOT_FOUND, "Candidate profile not found");
        }
    }

    private ApplicationResponse assembleResponse(Application application, boolean includeNotes) {
        ApplicationResponse response = applicationMapper.toResponse(application);
        if (includeNotes) {
            response.setNotes(applicationMapper.toNoteResponseList(
                    noteRepository.findAllByApplicationIdOrderByCreatedAtDesc(application.getId())));
        }
        return response;
    }

    private void evictApplicationCache(UUID applicationId) {
        var cache = cacheManager.getCache(RedisConfig.APPLICATION_DETAIL_CACHE);
        if (cache != null) cache.evict(applicationId);
    }
}
