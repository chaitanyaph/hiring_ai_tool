package com.cadence.candidateservice.service.impl;

import com.cadence.candidateservice.client.CompanyServiceClient;
import com.cadence.candidateservice.client.JobServiceClient;
import com.cadence.candidateservice.client.dto.JobDto;
import com.cadence.candidateservice.constant.ApplicationStatus;
import com.cadence.candidateservice.dto.request.ApplyToJobRequest;
import com.cadence.candidateservice.dto.request.ChangeApplicationStageRequest;
import com.cadence.candidateservice.dto.response.ApplicationResponse;
import com.cadence.candidateservice.entity.Application;
import com.cadence.candidateservice.entity.ApplicationStatusHistory;
import com.cadence.candidateservice.exception.*;
import com.cadence.candidateservice.kafka.event.ApplicationStageChangedEvent;
import com.cadence.candidateservice.kafka.event.ApplicationSubmittedEvent;
import com.cadence.candidateservice.kafka.event.ApplicationWithdrawnEvent;
import com.cadence.candidateservice.kafka.producer.CandidateEventProducer;
import com.cadence.candidateservice.mapper.ApplicationMapper;
import com.cadence.candidateservice.repository.ApplicationRepository;
import com.cadence.candidateservice.repository.ApplicationStatusHistoryRepository;
import com.cadence.candidateservice.security.CurrentUser;
import com.cadence.candidateservice.service.ApplicationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class ApplicationServiceImpl implements ApplicationService {

    private static final Set<ApplicationStatus> ACTIVE_STATUSES = EnumSet.of(
            ApplicationStatus.APPLIED, ApplicationStatus.RESUME_SCREENING, ApplicationStatus.AI_RESUME_MATCH,
            ApplicationStatus.AI_INTERVIEW, ApplicationStatus.CODING_ASSESSMENT,
            ApplicationStatus.TECHNICAL_INTERVIEW, ApplicationStatus.HR_INTERVIEW);
    private static final Set<ApplicationStatus> REJECTED_STATUSES = EnumSet.of(
            ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN);

    private final ApplicationRepository applicationRepository;
    private final ApplicationStatusHistoryRepository historyRepository;
    private final JobServiceClient jobServiceClient;
    private final CompanyServiceClient companyServiceClient;
    private final ApplicationMapper applicationMapper;
    private final CandidateEventProducer eventProducer;

    @Override
    @Transactional
    public ApplicationResponse apply(CurrentUser candidate, ApplyToJobRequest request) {
        if (applicationRepository.existsByCandidateIdAndJobId(candidate.getUserId(), request.getJobId())) {
            throw new DuplicateApplicationException();
        }

        JobDto job = fetchJobOrThrow(request.getJobId());
        if (!"PUBLISHED".equalsIgnoreCase(job.getStatus())) {
            throw new CandidateValidationException(ErrorCode.JOB_NOT_PUBLISHED,
                    "This job is not currently accepting applications");
        }

        Application application = Application.builder()
                .candidateId(candidate.getUserId())
                .jobId(job.getId())
                .companyId(job.getCompanyId())
                .jobTitleSnapshot(job.getTitle())
                .locationSnapshot(job.getLocation())
                .employmentTypeSnapshot(job.getEmploymentType())
                .companyNameSnapshot(resolveCompanyNameBestEffort(job.getCompanyId()))
                .status(ApplicationStatus.APPLIED)
                .createdBy(candidate.getUserId())
                .updatedBy(candidate.getUserId())
                .build();
        application = applicationRepository.save(application);

        recordHistory(application.getId(), null, ApplicationStatus.APPLIED, candidate.getUserId(), "Application submitted");

        eventProducer.publishApplicationSubmitted(ApplicationSubmittedEvent.builder()
                .applicationId(application.getId()).candidateId(candidate.getUserId())
                .jobId(job.getId()).companyId(job.getCompanyId()).occurredAt(LocalDateTime.now()).build());

        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional(readOnly = true)
    public List<ApplicationResponse> listMyApplications(CurrentUser candidate, String filter) {
        List<Application> applications = switch (filter == null ? "all" : filter.toLowerCase()) {
            case "active" -> applicationRepository.findAllByCandidateIdAndStatusInOrderByAppliedAtDesc(candidate.getUserId(), ACTIVE_STATUSES);
            case "offer" -> applicationRepository.findAllByCandidateIdAndStatusInOrderByAppliedAtDesc(candidate.getUserId(), Set.of(ApplicationStatus.OFFER));
            case "rejected" -> applicationRepository.findAllByCandidateIdAndStatusInOrderByAppliedAtDesc(candidate.getUserId(), REJECTED_STATUSES);
            default -> applicationRepository.findAllByCandidateIdOrderByAppliedAtDesc(candidate.getUserId());
        };
        return applicationMapper.toResponseList(applications);
    }

    @Override
    @Transactional(readOnly = true)
    public ApplicationResponse getApplicationDetail(CurrentUser candidate, UUID applicationId) {
        Application application = findOwnedOrThrow(applicationId, candidate.getUserId());
        ApplicationResponse response = applicationMapper.toResponse(application);
        response.setHistory(applicationMapper.toHistoryResponseList(
                historyRepository.findAllByApplicationIdOrderByChangedAtAsc(application.getId())));
        return response;
    }

    @Override
    @Transactional
    public ApplicationResponse withdraw(CurrentUser candidate, UUID applicationId) {
        Application application = findOwnedOrThrow(applicationId, candidate.getUserId());
        if (!application.getStatus().isWithdrawable()) {
            throw new CandidateValidationException(ErrorCode.APPLICATION_NOT_WITHDRAWABLE,
                    "This application can no longer be withdrawn");
        }

        ApplicationStatus previous = application.getStatus();
        application.setStatus(ApplicationStatus.WITHDRAWN);
        application.setWithdrawnAt(LocalDateTime.now());
        application.setUpdatedBy(candidate.getUserId());
        application = applicationRepository.save(application);

        recordHistory(application.getId(), previous, ApplicationStatus.WITHDRAWN, candidate.getUserId(), "Withdrawn by candidate");
        eventProducer.publishApplicationWithdrawn(ApplicationWithdrawnEvent.builder()
                .applicationId(application.getId()).candidateId(candidate.getUserId())
                .jobId(application.getJobId()).occurredAt(LocalDateTime.now()).build());

        return applicationMapper.toResponse(application);
    }

    @Override
    @Transactional
    public ApplicationResponse changeStage(CurrentUser recruiter, UUID applicationId, ChangeApplicationStageRequest request) {
        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));

        if (recruiter.getCompanyId() == null || !recruiter.getCompanyId().equals(application.getCompanyId())) {
            // Same 404 a non-existent application would return -- never leak that an
            // application belonging to another company exists.
            throw new ResourceNotFoundException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found");
        }

        ApplicationStatus previous = application.getStatus();
        if (!previous.canTransitionTo(request.getToStatus())) {
            throw new InvalidStatusTransitionException(previous, request.getToStatus());
        }

        application.setStatus(request.getToStatus());
        if (request.getMatchScore() != null) {
            application.setMatchScore(request.getMatchScore());
        }
        if (request.getToStatus() == ApplicationStatus.WITHDRAWN) {
            application.setWithdrawnAt(LocalDateTime.now());
        }
        application.setUpdatedBy(recruiter.getUserId());
        application = applicationRepository.save(application);

        recordHistory(application.getId(), previous, request.getToStatus(), recruiter.getUserId(), request.getNote());
        eventProducer.publishApplicationStageChanged(ApplicationStageChangedEvent.builder()
                .applicationId(application.getId()).candidateId(application.getCandidateId())
                .jobId(application.getJobId()).fromStatus(previous).toStatus(request.getToStatus())
                .occurredAt(LocalDateTime.now()).build());

        return applicationMapper.toResponse(application);
    }

    // ------------------------------------------------------------------

    private Application findOwnedOrThrow(UUID applicationId, UUID candidateId) {
        return applicationRepository.findByIdAndCandidateId(applicationId, candidateId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
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

    private String resolveCompanyNameBestEffort(UUID companyId) {
        try {
            var response = companyServiceClient.getCompany(companyId);
            return response != null && response.getData() != null ? response.getData().getCompanyName() : null;
        } catch (Exception e) {
            log.warn("Could not resolve company name for companyId {}: {}", companyId, e.getMessage());
            return null;
        }
    }

    private void recordHistory(UUID applicationId, ApplicationStatus from, ApplicationStatus to, UUID changedBy, String note) {
        historyRepository.save(ApplicationStatusHistory.builder()
                .applicationId(applicationId)
                .fromStatus(from)
                .toStatus(to)
                .changedBy(changedBy)
                .note(note)
                .build());
    }
}
