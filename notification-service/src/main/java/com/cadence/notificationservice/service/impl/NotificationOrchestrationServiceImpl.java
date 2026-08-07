package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.constants.*;
import com.cadence.notificationservice.email.TemplateRenderer;
import com.cadence.notificationservice.entity.EmailQueue;
import com.cadence.notificationservice.entity.Notification;
import com.cadence.notificationservice.entity.NotificationLog;
import com.cadence.notificationservice.entity.NotificationTemplate;
import com.cadence.notificationservice.feign.ApplicationServiceClient;
import com.cadence.notificationservice.feign.CandidateServiceClient;
import com.cadence.notificationservice.feign.CompanyServiceClient;
import com.cadence.notificationservice.feign.InterviewManagementServiceClient;
import com.cadence.notificationservice.feign.dto.ApplicationSummaryDto;
import com.cadence.notificationservice.feign.dto.CandidateDto;
import com.cadence.notificationservice.feign.dto.InterviewDetailDto;
import com.cadence.notificationservice.kafka.event.*;
import com.cadence.notificationservice.repository.EmailQueueRepository;
import com.cadence.notificationservice.repository.NotificationLogRepository;
import com.cadence.notificationservice.repository.NotificationRepository;
import com.cadence.notificationservice.repository.NotificationTemplateRepository;
import com.cadence.notificationservice.service.NotificationOrchestrationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Core event-to-notification router. Every Feign enrichment call is
 * wrapped in a safe try/catch that degrades to fields already present
 * on the Kafka payload -- a Feign failure (in particular the known
 * 401-prone interview-management-service call, see
 * InterviewDetailDto's javadoc) never blocks a send.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationOrchestrationServiceImpl implements NotificationOrchestrationService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy");
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("h:mm a");
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("MMM d, yyyy 'at' h:mm a");

    private final NotificationTemplateRepository templateRepository;
    private final EmailQueueRepository emailQueueRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationLogRepository notificationLogRepository;
    private final TemplateRenderer templateRenderer;

    private final CandidateServiceClient candidateServiceClient;
    private final CompanyServiceClient companyServiceClient;
    private final ApplicationServiceClient applicationServiceClient;
    private final InterviewManagementServiceClient interviewManagementServiceClient;

    @Override
    @Transactional
    public void handleCompanyCreated(CompanyCreatedEvent event) {
        // No individual recipient userId is carried on this event --
        // logged only, flagged rather than guessing an admin user.
        log(LogLevel.INFO, "company-service", "CompanyCreated", "Company created: " + event.getCompanyName(), event.getCompanyId());
    }

    @Override
    @Transactional
    public void handleTeamInvitationCreated(TeamInvitationCreatedEvent event) {
        String companyName = safeCompanyName(event.getCompanyId());
        Map<String, String> vars = new HashMap<>();
        vars.put("recipient_name", event.getFirstName());
        vars.put("company_name", companyName);
        vars.put("role", event.getRole());
        vars.put("invite_link", event.getInviteToken());
        queueEmail(TemplateCategory.RECRUITER_INVITATION, event.getEmail(), event.getFirstName(), vars,
                "TEAM_INVITATION", event.getInvitationId(), TriggerEvent.TEAM_INVITATION_CREATED);
    }

    @Override
    @Transactional
    public void handleJobPublished(JobPublishedEvent event) {
        log(LogLevel.INFO, "job-service", "JobPublished", "Job published: " + event.getTitle(), event.getJobId());
    }

    @Override
    @Transactional
    public void handleJobClosed(JobClosedEvent event) {
        log(LogLevel.INFO, "job-service", "JobClosed", "Job closed", event.getJobId());
    }

    @Override
    @Transactional
    public void handleUserRegistered(UserRegisteredEvent event) {
        boolean isCandidate = "CANDIDATE".equalsIgnoreCase(event.getUserType());
        Map<String, String> vars = new HashMap<>();
        vars.put("recipient_name", event.getFullName());
        vars.put("candidate_name", event.getFullName());
        vars.put("company_name", event.getCompanyId() != null ? safeCompanyName(event.getCompanyId()) : "");

        queueEmail(isCandidate ? TemplateCategory.CANDIDATE_REGISTRATION : TemplateCategory.WELCOME_EMAIL,
                event.getEmail(), event.getFullName(), vars, "USER", event.getUserId(), TriggerEvent.USER_REGISTERED);

        if (event.getVerificationLink() != null && !event.getVerificationLink().isBlank()) {
            Map<String, String> verifyVars = new HashMap<>(vars);
            verifyVars.put("verification_link", event.getVerificationLink());
            queueEmail(TemplateCategory.EMAIL_VERIFICATION, event.getEmail(), event.getFullName(), verifyVars,
                    "USER", event.getUserId(), TriggerEvent.USER_REGISTERED);
        }

        createNotification(event.getUserId(), event.getUserType(), event.getCompanyId(), NotificationCategory.ACCOUNT,
                "Welcome to Cadence", "Your account has been created.", ColorTone.SUCCESS, "USER", event.getUserId());
    }

    @Override
    @Transactional
    public void handlePasswordResetRequested(PasswordResetRequestedEvent event) {
        Map<String, String> vars = Map.of("reset_link", event.getResetLink());
        queueEmail(TemplateCategory.PASSWORD_RESET, event.getEmail(), null, vars,
                "USER", event.getUserId(), TriggerEvent.PASSWORD_RESET_REQUESTED);
    }

    @Override
    @Transactional
    public void handleApplicationCreated(ApplicationCreatedEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        String companyName = safeCompanyName(event.getCompanyId());

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "ApplicationCreated", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle, "company_name", companyName);
        queueEmail(TemplateCategory.APPLICATION_RECEIVED, candidateEmail, candidateName, vars,
                "APPLICATION", event.getApplicationId(), TriggerEvent.APPLICATION_SUBMITTED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, event.getCompanyId(), NotificationCategory.APPLICATION,
                "Application received", "We've received your application for " + jobTitle + ".", ColorTone.SUCCESS,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleResumeUploaded(ResumeUploadedEvent event) {
        Optional<CandidateDto> candidate = safeCandidate(event.getCandidateId());
        if (candidate.isEmpty()) {
            log(LogLevel.WARN, "notification-service", "ResumeUploaded", "Could not resolve candidate for resume " + event.getResumeId(), event.getCandidateId());
            return;
        }
        Map<String, String> vars = Map.of("candidate_name", nvl(candidate.get().getFullName()));
        queueEmail(TemplateCategory.RESUME_UPLOADED, candidate.get().getEmail(), candidate.get().getFullName(), vars,
                "RESUME", event.getResumeId(), TriggerEvent.RESUME_UPLOADED);
    }

    @Override
    @Transactional
    public void handleResumeAnalyzed(ResumeAnalyzedEvent event) {
        // No recruiter recipient id is carried on this event -- logged only.
        log(LogLevel.INFO, "resume-parser-service", "ResumeAnalyzed",
                "Resume analyzed for application " + event.getApplicationId() + " (score " + event.getOverallMatchScore() + ")", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleCandidateShortlisted(CandidateShortlistedEvent event) {
        // event.getDecision() is the String form of ai-interview-service's ShortlistDecision enum,
        // whose actual value is "REJECTED" (not "REJECT") -- comparing against "REJECT" here never
        // matched, so every non-shortlisted decision silently fell through to the "shortlisted" email.
        boolean rejected = !"SHORTLISTED".equalsIgnoreCase(event.getDecision());
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "CandidateShortlisted", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        String companyName = app.map(ApplicationSummaryDto::getCompanyId).map(this::safeCompanyName).orElse("");
        Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle, "company_name", companyName);
        TemplateCategory category = rejected ? TemplateCategory.RESUME_REJECTED : TemplateCategory.RESUME_SHORTLISTED;
        queueEmail(category, candidateEmail, candidateName, vars, "APPLICATION", event.getApplicationId(), TriggerEvent.CANDIDATE_SHORTLISTED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.APPLICATION,
                rejected ? "Application update" : "You've been shortlisted!",
                rejected ? "Update on your application for " + jobTitle + "." : "You've been shortlisted for " + jobTitle + ".",
                rejected ? ColorTone.WARNING : ColorTone.SUCCESS, "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleAiInterviewInvited(AiInterviewInvitedEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        String companyName = app.map(ApplicationSummaryDto::getCompanyId).map(this::safeCompanyName).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "AiInterviewInvited", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("candidate_name", nvl(candidateName));
        vars.put("job_title", jobTitle);
        vars.put("company_name", companyName);
        vars.put("interview_link", nvl(event.getInterviewLink()));
        vars.put("valid_from", event.getValidFrom() != null ? event.getValidFrom().format(DATE_TIME_FMT) : "");
        vars.put("valid_until", event.getValidUntil() != null ? event.getValidUntil().format(DATE_TIME_FMT) : "");
        queueEmail(TemplateCategory.AI_INTERVIEW_INVITATION, candidateEmail, candidateName, vars,
                "APPLICATION", event.getApplicationId(), TriggerEvent.AI_INTERVIEW_INVITED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.INTERVIEW,
                "AI interview invitation", "You've been invited to an AI interview for " + jobTitle + ".", ColorTone.INFO,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleAiInterviewCompleted(AiInterviewCompletedEvent event) {
        // No recruiter recipient id is carried on this event -- logged only.
        log(LogLevel.INFO, "ai-interview-service", "AIInterviewCompleted",
                "Candidate finished AI interview for application " + event.getApplicationId(), event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleCodingAssessmentInvited(CodingAssessmentInvitedEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        String companyName = app.map(ApplicationSummaryDto::getCompanyId).map(this::safeCompanyName).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "CodingAssessmentInvited", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("candidate_name", nvl(candidateName));
        vars.put("job_title", jobTitle);
        vars.put("company_name", companyName);
        vars.put("assessment_name", nvl(event.getAssessmentName()));
        vars.put("duration_minutes", event.getDurationMinutes() != null ? String.valueOf(event.getDurationMinutes()) : "");
        vars.put("passing_score_percent", event.getPassingScorePercent() != null ? String.valueOf(event.getPassingScorePercent()) : "");
        vars.put("assessment_link", nvl(event.getAssessmentLink()));
        vars.put("expiry_date", event.getExpiresAt() != null ? event.getExpiresAt().format(DATE_TIME_FMT) : "");
        queueEmail(TemplateCategory.CODING_ASSESSMENT_INVITATION, candidateEmail, candidateName, vars,
                "APPLICATION", event.getApplicationId(), TriggerEvent.CODING_ASSESSMENT_INVITED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.APPLICATION,
                "Coding assessment invitation", "You've been invited to a coding assessment for " + jobTitle + ".", ColorTone.INFO,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleCodingAssessmentCompleted(CodingAssessmentCompletedEvent event) {
        boolean passed = Boolean.TRUE.equals(event.getPassed());
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        String companyName = app.map(ApplicationSummaryDto::getCompanyId).map(this::safeCompanyName).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "CodingAssessmentCompleted", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle, "company_name", companyName,
                "score", event.getScore() != null ? String.valueOf(event.getScore()) : "");
        TemplateCategory category = passed ? TemplateCategory.CODING_ASSESSMENT_PASSED : TemplateCategory.CODING_ASSESSMENT_REJECTED;
        queueEmail(category, candidateEmail, candidateName, vars, "APPLICATION", event.getApplicationId(), TriggerEvent.CODING_ASSESSMENT_COMPLETED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.APPLICATION,
                passed ? "Coding assessment passed!" : "Coding assessment update",
                passed ? "You passed the coding assessment for " + jobTitle + "." : "Update on your coding assessment for " + jobTitle + ".",
                passed ? ColorTone.SUCCESS : ColorTone.WARNING, "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleOfferSent(OfferSentEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = event.getCandidateEmail() != null ? event.getCandidateEmail()
                : app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        String companyName = safeCompanyName(event.getCompanyId());

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "OfferSent", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle, "company_name", companyName);
        queueEmail(TemplateCategory.OFFER_LETTER, candidateEmail, candidateName, vars,
                "APPLICATION", event.getApplicationId(), TriggerEvent.OFFER_SENT);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, event.getCompanyId(), NotificationCategory.OFFER,
                "Offer letter sent", "Your offer letter for " + jobTitle + " is ready to review.", ColorTone.SUCCESS,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleAiInterviewEvaluated(InterviewEvaluatedEvent event) {
        String recommendation = event.getHiringRecommendation();
        if (recommendation == null || "HOLD".equalsIgnoreCase(recommendation)) {
            // Recruiter review pending -- no automatic pass/fail verdict to email yet.
            return;
        }
        boolean passed = "PROCEED".equalsIgnoreCase(recommendation);

        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        String companyName = app.map(ApplicationSummaryDto::getCompanyId).map(this::safeCompanyName).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "AiInterviewEvaluated", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle, "company_name", companyName,
                "overall_score", event.getOverallScore() != null ? String.valueOf(event.getOverallScore()) : "");
        TemplateCategory category = passed ? TemplateCategory.AI_INTERVIEW_PASSED : TemplateCategory.AI_INTERVIEW_REJECTED;
        queueEmail(category, candidateEmail, candidateName, vars, "APPLICATION", event.getApplicationId(), TriggerEvent.AI_INTERVIEW_EVALUATED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.INTERVIEW,
                passed ? "AI interview passed!" : "AI interview update",
                passed ? "You passed the AI interview for " + jobTitle + "." : "Update on your AI interview for " + jobTitle + ".",
                passed ? ColorTone.SUCCESS : ColorTone.WARNING, "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleAiInterviewExpired(AiInterviewExpiredEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        String companyName = app.map(ApplicationSummaryDto::getCompanyId).map(this::safeCompanyName).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "AiInterviewExpired", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle, "company_name", companyName);
        queueEmail(TemplateCategory.AI_INTERVIEW_EXPIRED, candidateEmail, candidateName, vars,
                "APPLICATION", event.getApplicationId(), TriggerEvent.AI_INTERVIEW_EXPIRED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.INTERVIEW,
                "AI interview expired", "Your AI interview invitation for " + jobTitle + " has expired.", ColorTone.WARNING,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleAiInterviewReminder(AiInterviewReminderEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "AiInterviewReminder", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("candidate_name", nvl(candidateName));
        vars.put("job_title", jobTitle);
        vars.put("interview_link", nvl(event.getInterviewLink()));
        vars.put("expiry_date", event.getExpiresAt() != null ? event.getExpiresAt().format(DATE_TIME_FMT) : "");
        queueEmail(TemplateCategory.AI_INTERVIEW_REMINDER, candidateEmail, candidateName, vars,
                "APPLICATION", event.getApplicationId(), TriggerEvent.AI_INTERVIEW_REMINDER_DUE);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.INTERVIEW,
                "AI interview reminder", "Your AI interview for " + jobTitle + " is expiring soon.", ColorTone.INFO,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleCodingAssessmentReminder(CodingAssessmentReminderEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "CodingAssessmentReminder", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = new HashMap<>();
        vars.put("candidate_name", nvl(candidateName));
        vars.put("job_title", jobTitle);
        vars.put("assessment_link", nvl(event.getAssessmentLink()));
        vars.put("expiry_date", event.getExpiresAt() != null ? event.getExpiresAt().format(DATE_TIME_FMT) : "");
        queueEmail(TemplateCategory.CODING_ASSESSMENT_REMINDER, candidateEmail, candidateName, vars,
                "APPLICATION", event.getApplicationId(), TriggerEvent.CODING_ASSESSMENT_REMINDER_DUE);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.APPLICATION,
                "Coding assessment reminder", "Your coding assessment for " + jobTitle + " is still pending.", ColorTone.INFO,
                "APPLICATION", event.getApplicationId());
    }

    /**
     * Resume/AI-interview/coding-assessment rejections already send their own tailored
     * email directly off their own source event (handleCandidateShortlisted,
     * handleAiInterviewEvaluated, handleCodingAssessmentCompleted) -- skip those fromStatus
     * values here so a single rejection doesn't produce two different emails. Everything
     * else that lands on REJECTED (background verification failure, or a manual recruiter
     * rejection from any stage) has no other email today, so this is the only place it fires.
     */
    private static final java.util.Set<String> REJECTION_HANDLED_ELSEWHERE = java.util.Set.of(
            "AI_MATCHED", "MANUAL_REVIEW", "AI_INTERVIEW_PENDING", "AI_INTERVIEW_COMPLETED",
            "CODING_ASSESSMENT_PENDING", "CODING_ASSESSMENT_COMPLETED");

    @Override
    @Transactional
    public void handleApplicationStatusChanged(ApplicationStatusChangedEvent event) {
        if ("REJECTED".equals(event.getToStatus()) && !REJECTION_HANDLED_ELSEWHERE.contains(event.getFromStatus())) {
            sendFinalRejection(event);
        } else if ("TECHNICAL_INTERVIEW".equals(event.getToStatus())) {
            notifyRecruiterOfQualifiedCandidate(event);
        }
    }

    private void sendFinalRejection(ApplicationStatusChangedEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        String companyName = safeCompanyName(event.getCompanyId());

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "ApplicationStatusChanged", "Could not resolve candidate email for application " + event.getApplicationId(), event.getApplicationId());
            return;
        }

        Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle, "company_name", companyName);
        queueEmail(TemplateCategory.FINAL_REJECTION, candidateEmail, candidateName, vars,
                "APPLICATION", event.getApplicationId(), TriggerEvent.APPLICATION_REJECTED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, event.getCompanyId(), NotificationCategory.APPLICATION,
                "Application update", "Thank you for your interest in " + jobTitle + ". We've decided to move forward with other candidates.",
                ColorTone.WARNING, "APPLICATION", event.getApplicationId());
    }

    private void notifyRecruiterOfQualifiedCandidate(ApplicationStatusChangedEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        UUID recruiterId = app.map(ApplicationSummaryDto::getAssignedRecruiterId).orElse(null);
        if (recruiterId == null) {
            return;
        }
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElse("A candidate");
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");
        createNotification(recruiterId, "RECRUITER", event.getCompanyId(), NotificationCategory.APPLICATION,
                "Candidate ready for technical interview",
                candidateName + " has qualified for the technical interview stage for " + jobTitle + ".",
                ColorTone.SUCCESS, "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleInterviewScheduled(InterviewScheduledEvent event) {
        Optional<InterviewDetailDto> detail = safeInterviewDetail(event.getInterviewId());
        Optional<ApplicationSummaryDto> app = detail.isEmpty() ? safeApplicationSummary(event.getJobId(), event.getApplicationId()) : Optional.empty();

        String candidateName = detail.map(InterviewDetailDto::getCandidateName)
                .or(() -> app.map(ApplicationSummaryDto::getCandidateNameSnapshot))
                .orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = detail.map(d -> (String) null) // interview detail never carries a raw email
                .or(() -> app.map(ApplicationSummaryDto::getCandidateEmailSnapshot))
                .orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = detail.map(InterviewDetailDto::getJobTitle).or(() -> app.map(ApplicationSummaryDto::getJobTitleSnapshot)).orElse("");
        String companyName = detail.map(InterviewDetailDto::getCompanyName).orElse("");

        if (candidateEmail == null) {
            log(LogLevel.WARN, "notification-service", "InterviewScheduled", "Could not resolve candidate email for interview " + event.getInterviewId(), event.getInterviewId());
            return;
        }

        String roundType = event.getRoundType() == null ? "" : event.getRoundType().toUpperCase();
        TemplateCategory category = "HR".equals(roundType) ? TemplateCategory.HR_INTERVIEW_INVITATION : TemplateCategory.TECHNICAL_INTERVIEW_INVITATION;
        // MANAGER/ARCHITECT/CUSTOM share the Technical template (no dedicated template exists
        // for those round types) but must not claim to be a "technical" round in the copy --
        // round_type substitutes the honest label instead of a hardcoded word.
        String roundTypeLabel = switch (roundType) {
            case "TECHNICAL" -> "technical";
            case "MANAGER" -> "manager";
            case "ARCHITECT" -> "architect";
            case "CUSTOM" -> "additional";
            default -> "technical";
        };

        Map<String, String> vars = Map.of(
                "candidate_name", nvl(candidateName), "job_title", jobTitle, "company_name", companyName,
                "round_type", roundTypeLabel,
                "interview_date", event.getScheduledDate() != null ? event.getScheduledDate().format(DATE_FMT) : "",
                "interview_time", event.getScheduledTime() != null ? event.getScheduledTime().format(TIME_FMT) : "");
        queueEmail(category, candidateEmail, candidateName, vars, "INTERVIEW", event.getInterviewId(), TriggerEvent.INTERVIEW_SCHEDULED);

        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.INTERVIEW,
                "Interview scheduled", "Your interview for " + jobTitle + " is scheduled.", ColorTone.INFO,
                "INTERVIEW", event.getInterviewId());
    }

    @Override
    @Transactional
    public void handleInterviewRescheduled(InterviewRescheduledEvent event) {
        Optional<EmailQueue> priorEmail = emailQueueRepository.findFirstByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc("INTERVIEW", event.getInterviewId());
        if (priorEmail.isEmpty()) {
            log(LogLevel.WARN, "notification-service", "InterviewRescheduled",
                    "No prior scheduled-interview email found to recover recipient contact for interview " + event.getInterviewId(), event.getInterviewId());
            return;
        }
        EmailQueue prior = priorEmail.get();
        Map<String, String> vars = Map.of(
                "candidate_name", nvl(prior.getRecipientName()),
                "job_title", "",
                "interview_date", event.getNewScheduledDate() != null ? event.getNewScheduledDate().format(DATE_FMT) : "",
                "interview_time", event.getNewScheduledTime() != null ? event.getNewScheduledTime().format(TIME_FMT) : "",
                "reschedule_reason", event.getReason() != null ? " Reason: " + event.getReason() : "");
        queueEmail(TemplateCategory.INTERVIEW_RESCHEDULED, prior.getRecipientEmail(), prior.getRecipientName(), vars,
                "INTERVIEW", event.getInterviewId(), TriggerEvent.INTERVIEW_RESCHEDULED);
    }

    @Override
    @Transactional
    public void handleInterviewCancelled(InterviewCancelledEvent event) {
        Optional<EmailQueue> priorEmail = emailQueueRepository.findFirstByRelatedEntityTypeAndRelatedEntityIdOrderByCreatedAtDesc("INTERVIEW", event.getInterviewId());
        if (priorEmail.isEmpty()) {
            log(LogLevel.WARN, "notification-service", "InterviewCancelled",
                    "No prior scheduled-interview email found to recover recipient contact for interview " + event.getInterviewId(), event.getInterviewId());
            return;
        }
        EmailQueue prior = priorEmail.get();
        Map<String, String> vars = Map.of(
                "candidate_name", nvl(prior.getRecipientName()), "job_title", "",
                "cancel_reason", event.getReason() != null ? " Reason: " + event.getReason() : "");
        queueEmail(TemplateCategory.INTERVIEW_CANCELLED, prior.getRecipientEmail(), prior.getRecipientName(), vars,
                "INTERVIEW", event.getInterviewId(), TriggerEvent.INTERVIEW_CANCELLED);
    }

    @Override
    @Transactional
    public void handleCandidateMovedToHr(CandidateMovedToHrEvent event) {
        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.APPLICATION,
                "Moved to HR round", "You've been moved to the HR round.", ColorTone.SUCCESS,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleCandidateSelected(CandidateSelectedEvent event) {
        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.APPLICATION,
                "You've been selected!", "Congratulations, you've been selected.", ColorTone.SUCCESS,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleCandidateRejected(CandidateRejectedEvent event) {
        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, null, NotificationCategory.APPLICATION,
                "Application update", "Thank you for your time. We've decided to move forward with other candidates.", ColorTone.WARNING,
                "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleOfferAccepted(OfferAcceptedEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");

        if (candidateEmail != null) {
            Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle);
            queueEmail(TemplateCategory.OFFER_ACCEPTED, candidateEmail, candidateName, vars,
                    "APPLICATION", event.getApplicationId(), TriggerEvent.OFFER_ACCEPTED);
        }
        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, event.getCompanyId(), NotificationCategory.OFFER,
                "Offer accepted", "Welcome aboard for " + jobTitle + "!", ColorTone.SUCCESS, "APPLICATION", event.getApplicationId());
    }

    @Override
    @Transactional
    public void handleOfferRejected(OfferRejectedEvent event) {
        Optional<ApplicationSummaryDto> app = safeApplicationSummary(event.getJobId(), event.getApplicationId());
        String candidateName = app.map(ApplicationSummaryDto::getCandidateNameSnapshot).orElseGet(() -> safeCandidateName(event.getCandidateId()));
        String candidateEmail = app.map(ApplicationSummaryDto::getCandidateEmailSnapshot).orElseGet(() -> safeCandidateEmail(event.getCandidateId()));
        String jobTitle = app.map(ApplicationSummaryDto::getJobTitleSnapshot).orElse("");

        if (candidateEmail != null) {
            Map<String, String> vars = Map.of("candidate_name", nvl(candidateName), "job_title", jobTitle);
            queueEmail(TemplateCategory.OFFER_REJECTED, candidateEmail, candidateName, vars,
                    "APPLICATION", event.getApplicationId(), TriggerEvent.OFFER_REJECTED);
        }
        createNotification(event.getCandidateId(), PlatformRole.CANDIDATE, event.getCompanyId(), NotificationCategory.OFFER,
                "Offer response received", "We've received your decision regarding the offer for " + jobTitle + ".", ColorTone.INFO,
                "APPLICATION", event.getApplicationId());
    }

    // ---- shared helpers ----

    private void queueEmail(TemplateCategory category, String recipientEmail, String recipientName, Map<String, String> vars,
                             String relatedEntityType, UUID relatedEntityId, TriggerEvent triggerEvent) {
        Optional<NotificationTemplate> templateOpt = templateRepository.findByCategory(category);
        if (templateOpt.isEmpty() || !templateOpt.get().isActive()) {
            log(LogLevel.WARN, "notification-service", "TemplateMissing", "No active template for category " + category, relatedEntityId);
            return;
        }
        NotificationTemplate template = templateOpt.get();
        String subject = templateRenderer.render(template.getSubject(), vars);
        String bodyHtml = templateRenderer.render(template.getBodyHtml(), vars);

        EmailQueue queued = EmailQueue.builder()
                .templateId(template.getId())
                .recipientEmail(recipientEmail)
                .recipientName(recipientName)
                .subject(subject)
                .bodyHtml(bodyHtml)
                .status(EmailStatus.PENDING)
                .attempts(0)
                .maxAttempts(5)
                .relatedEntityType(relatedEntityType)
                .relatedEntityId(relatedEntityId)
                .triggerEvent(triggerEvent)
                .build();
        emailQueueRepository.save(queued);
        log(LogLevel.INFO, "email-dispatcher", "EmailQueued", "Queued " + category + " email to " + recipientEmail, relatedEntityId);
    }

    private void createNotification(UUID recipientId, String recipientRole, UUID companyId, NotificationCategory category,
                                     String title, String message, ColorTone colorTone, String entityType, UUID entityId) {
        if (recipientId == null) {
            return;
        }
        notificationRepository.save(Notification.builder()
                .recipientId(recipientId)
                .recipientRole(recipientRole)
                .companyId(companyId)
                .category(category)
                .title(title)
                .message(message)
                .colorTone(colorTone)
                .entityType(entityType)
                .entityId(entityId)
                .status(NotificationStatus.UNREAD)
                .build());
    }

    private void log(LogLevel level, String source, String eventType, String message, UUID relatedEntityId) {
        notificationLogRepository.save(NotificationLog.builder()
                .level(level).source(source).eventType(eventType).message(message).relatedEntityId(relatedEntityId).build());
    }


    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private Optional<CandidateDto> safeCandidate(UUID candidateId) {
        if (candidateId == null) return Optional.empty();
        try {
            return Optional.ofNullable(candidateServiceClient.getCandidateSummary(candidateId).getData());
        } catch (Exception e) {
            log.warn("Candidate enrichment failed for {}: {}", candidateId, e.getMessage());
            return Optional.empty();
        }
    }

    private String safeCandidateName(UUID candidateId) {
        return safeCandidate(candidateId).map(CandidateDto::getFullName).orElse(null);
    }

    private String safeCandidateEmail(UUID candidateId) {
        return safeCandidate(candidateId).map(CandidateDto::getEmail).orElse(null);
    }

    private String safeCompanyName(UUID companyId) {
        if (companyId == null) return "";
        try {
            var data = companyServiceClient.getCompany(companyId).getData();
            return data != null ? data.getCompanyName() : "";
        } catch (Exception e) {
            log.warn("Company enrichment failed for {}: {}", companyId, e.getMessage());
            return "";
        }
    }

    private Optional<ApplicationSummaryDto> safeApplicationSummary(UUID jobId, UUID applicationId) {
        if (jobId == null || applicationId == null) return Optional.empty();
        try {
            List<ApplicationSummaryDto> apps = applicationServiceClient.getApplicationsByJob(jobId).getData();
            if (apps == null) return Optional.empty();
            return apps.stream().filter(a -> applicationId.equals(a.getId())).findFirst();
        } catch (Exception e) {
            log.warn("Application enrichment failed for job {}: {}", jobId, e.getMessage());
            return Optional.empty();
        }
    }

    private Optional<InterviewDetailDto> safeInterviewDetail(UUID interviewId) {
        if (interviewId == null) return Optional.empty();
        try {
            return Optional.ofNullable(interviewManagementServiceClient.getInterviewDetail(interviewId).getData());
        } catch (Exception e) {
            log.debug("Interview-management enrichment unavailable for {} (expected -- auth-protected endpoint): {}", interviewId, e.getMessage());
            return Optional.empty();
        }
    }
}
