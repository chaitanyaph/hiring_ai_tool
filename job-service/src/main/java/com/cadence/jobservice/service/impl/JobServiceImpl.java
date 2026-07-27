package com.cadence.jobservice.service.impl;

import com.cadence.jobservice.client.CompanyServiceClient;
import com.cadence.jobservice.client.dto.DepartmentDto;
import com.cadence.jobservice.constant.*;
import com.cadence.jobservice.dto.request.*;
import com.cadence.jobservice.dto.response.*;
import com.cadence.jobservice.entity.*;
import com.cadence.jobservice.exception.*;
import com.cadence.jobservice.kafka.event.*;
import com.cadence.jobservice.kafka.producer.JobEventProducer;
import com.cadence.jobservice.mapper.JobMapper;
import com.cadence.jobservice.mapper.PipelineStageMapper;
import com.cadence.jobservice.mapper.SkillMapper;
import com.cadence.jobservice.repository.*;
import com.cadence.jobservice.security.CurrentUser;
import com.cadence.jobservice.service.JobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobServiceImpl implements JobService {

    private final JobRepository jobRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final JobRequirementsRepository jobRequirementsRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobBenefitRepository jobBenefitRepository;
    private final JobPipelineStageRepository jobPipelineStageRepository;
    private final JobAssignmentRepository jobAssignmentRepository;
    private final JobStatusHistoryRepository jobStatusHistoryRepository;
    private final JobAuditRepository jobAuditRepository;

    private final JobMapper jobMapper;
    private final SkillMapper skillMapper;
    private final PipelineStageMapper pipelineStageMapper;

    private final JobEventProducer eventProducer;
    private final CompanyServiceClient companyServiceClient;

    // ------------------------------------------------------------------
    // Wizard Step 1 -- Basic info
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public JobDetailResponse createDraft(JobBasicInfoRequest request, CurrentUser currentUser) {
        Job job = Job.builder()
                .companyId(currentUser.getCompanyId())
                .departmentId(request.getDepartmentId())
                .jobCode(generateUniqueJobCode(currentUser.getCompanyId()))
                .title(request.getTitle().trim())
                .location(request.getLocation())
                .workType(request.getWorkType())
                .employmentType(request.getEmploymentType())
                .numberOfOpenings(request.getNumberOfOpenings())
                .applicationDeadline(request.getApplicationDeadline())
                .status(JobStatus.DRAFT)
                .createdBy(currentUser.getUserId())
                .updatedBy(currentUser.getUserId())
                .build();
        job = jobRepository.save(job);

        saveDescription(job.getId(), request.getDescriptionHtml());
        seedDefaultPipeline(job.getId());
        recordStatusHistory(job.getId(), null, JobStatus.DRAFT, currentUser.getUserId(), "Job created");
        recordAudit(job.getId(), "JOB_CREATED", currentUser.getUserId(), null);

        eventProducer.publishJobCreated(JobCreatedEvent.builder()
                .jobId(job.getId()).companyId(job.getCompanyId())
                .title(job.getTitle()).jobCode(job.getJobCode()).occurredAt(LocalDateTime.now()).build());

        return assembleDetail(job);
    }

    @Override
    @Transactional
    public JobDetailResponse updateBasicInfo(UUID jobId, JobBasicInfoRequest request, CurrentUser currentUser) {
        Job job = findEditableJobOrThrow(jobId, currentUser);

        job.setTitle(request.getTitle().trim());
        job.setDepartmentId(request.getDepartmentId());
        job.setLocation(request.getLocation());
        job.setWorkType(request.getWorkType());
        job.setEmploymentType(request.getEmploymentType());
        job.setNumberOfOpenings(request.getNumberOfOpenings());
        job.setApplicationDeadline(request.getApplicationDeadline());
        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);

        saveDescription(jobId, request.getDescriptionHtml());
        recordAudit(jobId, "BASIC_INFO_UPDATED", currentUser.getUserId(), null);
        publishUpdated(job);

        return assembleDetail(job);
    }

    // ------------------------------------------------------------------
    // Wizard Step 2 -- Requirements
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public JobDetailResponse updateRequirements(UUID jobId, JobRequirementsRequest request, CurrentUser currentUser) {
        Job job = findEditableJobOrThrow(jobId, currentUser);
        validateExperienceRange(request.getMinExperienceYears(), request.getMaxExperienceYears());
        validateSalaryRange(request.getMinSalary(), request.getMaxSalary());

        JobRequirements requirements = jobRequirementsRepository.findByJobId(jobId)
                .orElseGet(() -> JobRequirements.builder().jobId(jobId).build());
        requirements.setMinExperienceYears(request.getMinExperienceYears());
        requirements.setMaxExperienceYears(request.getMaxExperienceYears());
        requirements.setEducation(request.getEducation());
        requirements.setCertifications(request.getCertifications());
        requirements.setLanguages(request.getLanguages());
        requirements.setMinSalary(request.getMinSalary());
        requirements.setMaxSalary(request.getMaxSalary());
        if (request.getSalaryCurrency() != null && !request.getSalaryCurrency().isBlank()) {
            requirements.setSalaryCurrency(request.getSalaryCurrency());
        }
        requirements.setNoticePeriodDays(request.getNoticePeriodDays());
        requirements.setResponsibilities(request.getResponsibilities());
        jobRequirementsRepository.save(requirements);

        jobSkillRepository.deleteAllByJobId(jobId);
        if (request.getSkills() != null) {
            List<JobSkill> skills = request.getSkills().stream()
                    .map(s -> JobSkill.builder().jobId(jobId).skillName(s.getSkillName()).skillType(s.getSkillType()).build())
                    .toList();
            jobSkillRepository.saveAll(skills);
        }

        jobBenefitRepository.deleteAllByJobId(jobId);
        if (request.getBenefits() != null) {
            List<JobBenefit> benefits = request.getBenefits().stream()
                    .map(b -> JobBenefit.builder().jobId(jobId).benefitText(b).build())
                    .toList();
            jobBenefitRepository.saveAll(benefits);
        }

        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);
        recordAudit(jobId, "REQUIREMENTS_UPDATED", currentUser.getUserId(), null);
        publishUpdated(job);

        return assembleDetail(job);
    }

    // ------------------------------------------------------------------
    // Wizard Step 3 -- Hiring stages
    // ------------------------------------------------------------------

    @Override
    @Transactional
    public JobDetailResponse updatePipelineStages(UUID jobId, UpdatePipelineStagesRequest request, CurrentUser currentUser) {
        Job job = findEditableJobOrThrow(jobId, currentUser);

        List<JobPipelineStage> existing = jobPipelineStageRepository.findAllByJobIdOrderByStageOrderAsc(jobId);
        Map<UUID, JobPipelineStage> existingById = existing.stream()
                .collect(Collectors.toMap(JobPipelineStage::getId, s -> s));
        Set<UUID> keptIds = new HashSet<>();

        for (PipelineStageRequest stageRequest : request.getStages()) {
            if (stageRequest.getId() != null && existingById.containsKey(stageRequest.getId())) {
                JobPipelineStage stage = existingById.get(stageRequest.getId());
                stage.setStageName(stageRequest.getStageName().trim());
                stage.setStageOrder(stageRequest.getStageOrder());
                stage.setEnabled(stageRequest.isEnabled());
                jobPipelineStageRepository.save(stage);
                keptIds.add(stage.getId());
            } else {
                JobPipelineStage created = jobPipelineStageRepository.save(JobPipelineStage.builder()
                        .jobId(jobId)
                        .stageName(stageRequest.getStageName().trim())
                        .stageOrder(stageRequest.getStageOrder())
                        .enabled(stageRequest.isEnabled())
                        .systemDefault(false)
                        .build());
                keptIds.add(created.getId());
            }
        }

        existing.stream()
                .filter(s -> !keptIds.contains(s.getId()))
                .forEach(jobPipelineStageRepository::delete);

        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);
        recordAudit(jobId, "PIPELINE_STAGES_UPDATED", currentUser.getUserId(), null);
        publishUpdated(job);

        return assembleDetail(job);
    }

    // ------------------------------------------------------------------
    // Step 4 -- Review / read
    // ------------------------------------------------------------------

    @Override
    @Cacheable(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional(readOnly = true)
    public JobDetailResponse getJobDetail(UUID jobId, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        return assembleDetail(job);
    }

    @Override
    @Transactional(readOnly = true)
    public JobDetailResponse getJobDetailInternal(UUID jobId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));
        return assembleDetail(job);
    }

    // ------------------------------------------------------------------
    // Lifecycle transitions
    // ------------------------------------------------------------------

    @Override
    @CacheEvict(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional
    public JobDetailResponse publishJob(UUID jobId, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);
        transitionStatus(job, JobStatus.PUBLISHED);
        validateReadyToPublish(job);

        job.setStatus(JobStatus.PUBLISHED);
        job.setPublishedAt(LocalDateTime.now());
        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);

        recordStatusHistory(jobId, JobStatus.DRAFT, JobStatus.PUBLISHED, currentUser.getUserId(), "Published");
        recordAudit(jobId, "JOB_PUBLISHED", currentUser.getUserId(), null);

        eventProducer.publishJobPublished(JobPublishedEvent.builder()
                .jobId(job.getId()).companyId(job.getCompanyId()).departmentId(job.getDepartmentId())
                .title(job.getTitle()).jobCode(job.getJobCode()).occurredAt(LocalDateTime.now()).build());

        return assembleDetail(job);
    }

    @Override
    @CacheEvict(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional
    public JobDetailResponse pauseJob(UUID jobId, StatusChangeRequest request, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);
        JobStatus from = job.getStatus();
        transitionStatus(job, JobStatus.PAUSED);

        job.setStatus(JobStatus.PAUSED);
        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);

        recordStatusHistory(jobId, from, JobStatus.PAUSED, currentUser.getUserId(), request.getReason());
        recordAudit(jobId, "JOB_PAUSED", currentUser.getUserId(), request.getReason());
        publishUpdated(job);
        return assembleDetail(job);
    }

    @Override
    @CacheEvict(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional
    public JobDetailResponse resumeJob(UUID jobId, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);
        JobStatus from = job.getStatus();
        transitionStatus(job, JobStatus.PUBLISHED);

        job.setStatus(JobStatus.PUBLISHED);
        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);

        recordStatusHistory(jobId, from, JobStatus.PUBLISHED, currentUser.getUserId(), "Resumed");
        recordAudit(jobId, "JOB_RESUMED", currentUser.getUserId(), null);
        publishUpdated(job);
        return assembleDetail(job);
    }

    @Override
    @CacheEvict(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional
    public JobDetailResponse closeJob(UUID jobId, StatusChangeRequest request, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);
        JobStatus from = job.getStatus();
        transitionStatus(job, JobStatus.CLOSED);

        job.setStatus(JobStatus.CLOSED);
        job.setClosedAt(LocalDateTime.now());
        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);

        recordStatusHistory(jobId, from, JobStatus.CLOSED, currentUser.getUserId(), request.getReason());
        recordAudit(jobId, "JOB_CLOSED", currentUser.getUserId(), request.getReason());

        eventProducer.publishJobClosed(JobClosedEvent.builder()
                .jobId(job.getId()).companyId(job.getCompanyId()).occurredAt(LocalDateTime.now()).build());

        return assembleDetail(job);
    }

    @Override
    @CacheEvict(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional
    public JobDetailResponse archiveJob(UUID jobId, StatusChangeRequest request, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);
        JobStatus from = job.getStatus();
        transitionStatus(job, JobStatus.ARCHIVED);

        job.setStatus(JobStatus.ARCHIVED);
        job.setArchivedAt(LocalDateTime.now());
        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);

        recordStatusHistory(jobId, from, JobStatus.ARCHIVED, currentUser.getUserId(), request.getReason());
        recordAudit(jobId, "JOB_ARCHIVED", currentUser.getUserId(), request.getReason());

        eventProducer.publishJobArchived(JobArchivedEvent.builder()
                .jobId(job.getId()).companyId(job.getCompanyId()).occurredAt(LocalDateTime.now()).build());

        return assembleDetail(job);
    }

    @Override
    @CacheEvict(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional
    public JobDetailResponse restoreJob(UUID jobId, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);
        JobStatus from = job.getStatus();
        transitionStatus(job, JobStatus.DRAFT);

        job.setStatus(JobStatus.DRAFT);
        job.setArchivedAt(null);
        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);

        recordStatusHistory(jobId, from, JobStatus.DRAFT, currentUser.getUserId(), "Restored");
        recordAudit(jobId, "JOB_RESTORED", currentUser.getUserId(), null);

        eventProducer.publishJobRestored(JobRestoredEvent.builder()
                .jobId(job.getId()).companyId(job.getCompanyId()).occurredAt(LocalDateTime.now()).build());

        return assembleDetail(job);
    }

    @Override
    @CacheEvict(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional
    public void deleteJob(UUID jobId, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);
        if (job.getStatus() != JobStatus.DRAFT) {
            throw new JobValidationException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Only draft jobs can be deleted -- archive a published/closed job instead");
        }

        job.setUpdatedBy(currentUser.getUserId());
        job.markDeleted();
        jobRepository.save(job);
        recordAudit(jobId, "JOB_DELETED", currentUser.getUserId(), null);

        eventProducer.publishJobDeleted(JobDeletedEvent.builder()
                .jobId(job.getId()).companyId(job.getCompanyId()).occurredAt(LocalDateTime.now()).build());
    }

    @Override
    @Transactional
    public JobDetailResponse duplicateJob(UUID jobId, CurrentUser currentUser) {
        Job source = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);

        Job copy = Job.builder()
                .companyId(source.getCompanyId())
                .departmentId(source.getDepartmentId())
                .jobCode(generateUniqueJobCode(source.getCompanyId()))
                .title(source.getTitle() + " (Copy)")
                .location(source.getLocation())
                .workType(source.getWorkType())
                .employmentType(source.getEmploymentType())
                .numberOfOpenings(source.getNumberOfOpenings())
                .status(JobStatus.DRAFT)
                .createdBy(currentUser.getUserId())
                .updatedBy(currentUser.getUserId())
                .build();
        copy = jobRepository.save(copy);
        Job finalCopy = copy;

        jobDescriptionRepository.findByJobId(jobId).ifPresent(desc -> saveDescription(finalCopy.getId(), desc.getDescriptionHtml()));

        jobRequirementsRepository.findByJobId(jobId).ifPresent(reqs -> {
            JobRequirements copyReqs = JobRequirements.builder()
                    .jobId(finalCopy.getId())
                    .minExperienceYears(reqs.getMinExperienceYears())
                    .maxExperienceYears(reqs.getMaxExperienceYears())
                    .education(reqs.getEducation())
                    .certifications(reqs.getCertifications())
                    .languages(reqs.getLanguages())
                    .minSalary(reqs.getMinSalary())
                    .maxSalary(reqs.getMaxSalary())
                    .salaryCurrency(reqs.getSalaryCurrency())
                    .noticePeriodDays(reqs.getNoticePeriodDays())
                    .responsibilities(reqs.getResponsibilities())
                    .build();
            jobRequirementsRepository.save(copyReqs);
        });

        jobSkillRepository.findAllByJobId(jobId).forEach(skill ->
                jobSkillRepository.save(JobSkill.builder().jobId(finalCopy.getId()).skillName(skill.getSkillName()).skillType(skill.getSkillType()).build()));

        jobBenefitRepository.findAllByJobId(jobId).forEach(benefit ->
                jobBenefitRepository.save(JobBenefit.builder().jobId(finalCopy.getId()).benefitText(benefit.getBenefitText()).build()));

        jobPipelineStageRepository.findAllByJobIdOrderByStageOrderAsc(jobId).forEach(stage ->
                jobPipelineStageRepository.save(JobPipelineStage.builder()
                        .jobId(finalCopy.getId()).stageName(stage.getStageName())
                        .stageOrder(stage.getStageOrder()).enabled(stage.isEnabled())
                        .systemDefault(stage.isSystemDefault()).build()));

        recordStatusHistory(finalCopy.getId(), null, JobStatus.DRAFT, currentUser.getUserId(), "Duplicated from " + source.getJobCode());
        recordAudit(finalCopy.getId(), "JOB_DUPLICATED", currentUser.getUserId(), "source=" + source.getId());

        eventProducer.publishJobCreated(JobCreatedEvent.builder()
                .jobId(finalCopy.getId()).companyId(finalCopy.getCompanyId())
                .title(finalCopy.getTitle()).jobCode(finalCopy.getJobCode()).occurredAt(LocalDateTime.now()).build());

        return assembleDetail(copy);
    }

    // ------------------------------------------------------------------
    // Assignment
    // ------------------------------------------------------------------

    @Override
    @CacheEvict(value = com.cadence.jobservice.config.RedisConfig.JOB_DETAIL_CACHE, key = "#jobId")
    @Transactional
    public JobDetailResponse assignJob(UUID jobId, AssignJobRequest request, CurrentUser currentUser) {
        Job job = findEditableJobOrThrow(jobId, currentUser);

        if (request.getRecruiterId() != null) {
            jobAssignmentRepository.deleteByJobIdAndAssignmentRole(jobId, JobAssignmentRole.RECRUITER);
            jobAssignmentRepository.save(JobAssignment.builder()
                    .jobId(jobId).userId(request.getRecruiterId()).assignmentRole(JobAssignmentRole.RECRUITER).build());
            job.setRecruiterId(request.getRecruiterId());
        }
        if (request.getHiringManagerId() != null) {
            jobAssignmentRepository.deleteByJobIdAndAssignmentRole(jobId, JobAssignmentRole.HIRING_MANAGER);
            jobAssignmentRepository.save(JobAssignment.builder()
                    .jobId(jobId).userId(request.getHiringManagerId()).assignmentRole(JobAssignmentRole.HIRING_MANAGER).build());
            job.setHiringManagerId(request.getHiringManagerId());
        }

        job.setUpdatedBy(currentUser.getUserId());
        job = jobRepository.save(job);
        recordAudit(jobId, "JOB_ASSIGNED", currentUser.getUserId(), null);
        publishUpdated(job);

        return assembleDetail(job);
    }

    // ------------------------------------------------------------------
    // Listing / dashboard
    // ------------------------------------------------------------------

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<JobSummaryResponse> searchJobs(JobSearchCriteria criteria, Pageable pageable, CurrentUser currentUser) {
        Specification<Job> spec = Specification.where(JobSpecifications.companyId(currentUser.getCompanyId()))
                .and(JobSpecifications.titleContains(criteria.getTitle()))
                .and(JobSpecifications.departmentId(criteria.getDepartmentId()))
                .and(JobSpecifications.locationContains(criteria.getLocation()))
                .and(JobSpecifications.status(criteria.getStatus()))
                .and(JobSpecifications.employmentType(criteria.getEmploymentType()))
                .and(JobSpecifications.recruiterId(criteria.getRecruiterId()))
                .and(JobSpecifications.hiringManagerId(criteria.getHiringManagerId()))
                .and(JobSpecifications.createdBetween(criteria.getCreatedFrom(), criteria.getCreatedTo()));

        Page<Job> page = jobRepository.findAll(spec, pageable);
        Map<UUID, String> departmentNameCache = new HashMap<>();

        Page<JobSummaryResponse> mapped = page.map(job -> {
            JobSummaryResponse summary = jobMapper.toSummary(job);
            summary.setDepartmentName(resolveDepartmentName(job.getDepartmentId(), departmentNameCache));
            summary.setApplicantsCount(0L);
            return summary;
        });

        return PagedResponse.from(mapped);
    }

    @Override
    @Cacheable(value = com.cadence.jobservice.config.RedisConfig.JOB_COUNTS_CACHE, key = "#currentUser.companyId")
    @Transactional(readOnly = true)
    public JobCountsResponse getCounts(CurrentUser currentUser) {
        UUID companyId = currentUser.getCompanyId();
        return JobCountsResponse.builder()
                .total(jobRepository.countByCompanyId(companyId))
                .published(jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.PUBLISHED))
                .draft(jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.DRAFT))
                .archived(jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.ARCHIVED))
                .paused(jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.PAUSED))
                .closed(jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.CLOSED))
                .expired(jobRepository.countByCompanyIdAndStatus(companyId, JobStatus.EXPIRED))
                .distinctDepartments(jobRepository.countDistinctDepartments(companyId))
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardResponse getDashboard(CurrentUser currentUser) {
        UUID companyId = currentUser.getCompanyId();
        JobCountsResponse counts = getCounts(currentUser);

        List<JobSummaryResponse> recentlyCreated = jobRepository.findTop5ByCompanyIdOrderByCreatedAtDesc(companyId).stream()
                .map(jobMapper::toSummary).toList();

        LocalDate today = LocalDate.now();
        List<JobSummaryResponse> closingSoon = jobRepository.findAllByCompanyIdAndStatusInAndApplicationDeadlineBetween(
                        companyId, List.of(JobStatus.PUBLISHED, JobStatus.PAUSED), today, today.plusDays(7)).stream()
                .map(jobMapper::toSummary).toList();

        return DashboardResponse.builder()
                .totalJobs(counts.getTotal())
                .publishedJobs(counts.getPublished())
                .draftJobs(counts.getDraft())
                .archivedJobs(counts.getArchived())
                .recentlyCreated(recentlyCreated)
                .closingSoon(closingSoon)
                .applicationsCount(0)
                .build();
    }

    // ------------------------------------------------------------------
    // Internal helpers
    // ------------------------------------------------------------------

    private JobDetailResponse assembleDetail(Job job) {
        JobDetailResponse response = jobMapper.toDetail(job);
        response.setDescriptionHtml(jobDescriptionRepository.findByJobId(job.getId())
                .map(JobDescription::getDescriptionHtml).orElse(null));

        JobRequirements requirements = jobRequirementsRepository.findByJobId(job.getId()).orElse(null);
        List<SkillResponse> skills = jobSkillRepository.findAllByJobId(job.getId()).stream()
                .map(skillMapper::toResponse).toList();
        List<String> benefits = jobBenefitRepository.findAllByJobId(job.getId()).stream()
                .map(JobBenefit::getBenefitText).toList();

        response.setRequirements(JobRequirementsResponse.builder()
                .minExperienceYears(requirements != null ? requirements.getMinExperienceYears() : null)
                .maxExperienceYears(requirements != null ? requirements.getMaxExperienceYears() : null)
                .skills(skills)
                .education(requirements != null ? requirements.getEducation() : null)
                .certifications(requirements != null ? requirements.getCertifications() : null)
                .languages(requirements != null ? requirements.getLanguages() : null)
                .minSalary(requirements != null ? requirements.getMinSalary() : null)
                .maxSalary(requirements != null ? requirements.getMaxSalary() : null)
                .salaryCurrency(requirements != null ? requirements.getSalaryCurrency() : null)
                .noticePeriodDays(requirements != null ? requirements.getNoticePeriodDays() : null)
                .responsibilities(requirements != null ? requirements.getResponsibilities() : null)
                .benefits(benefits)
                .build());

        response.setPipelineStages(jobPipelineStageRepository.findAllByJobIdOrderByStageOrderAsc(job.getId()).stream()
                .map(pipelineStageMapper::toResponse).toList());

        return response;
    }

    private void saveDescription(UUID jobId, String descriptionHtml) {
        JobDescription description = jobDescriptionRepository.findByJobId(jobId)
                .orElseGet(() -> JobDescription.builder().jobId(jobId).build());
        description.setDescriptionHtml(descriptionHtml);
        jobDescriptionRepository.save(description);
    }

    private void seedDefaultPipeline(UUID jobId) {
        int order = 1;
        for (String stageName : PipelineStageDefaults.DEFAULT_STAGES) {
            jobPipelineStageRepository.save(JobPipelineStage.builder()
                    .jobId(jobId).stageName(stageName).stageOrder(order++).enabled(true).systemDefault(true).build());
        }
    }

    private String generateUniqueJobCode(UUID companyId) {
        String code;
        do {
            code = "JOB-" + LocalDate.now().getYear() + "-" + String.format("%05d", new Random().nextInt(100000));
        } while (jobRepository.existsByCompanyIdAndJobCode(companyId, code));
        return code;
    }

    private void recordStatusHistory(UUID jobId, JobStatus from, JobStatus to, UUID changedBy, String reason) {
        jobStatusHistoryRepository.save(JobStatusHistory.builder()
                .jobId(jobId).fromStatus(from).toStatus(to).changedBy(changedBy).reason(reason).build());
    }

    private void recordAudit(UUID jobId, String action, UUID performedBy, String details) {
        jobAuditRepository.save(JobAudit.builder()
                .jobId(jobId).action(action).performedBy(performedBy).details(details).build());
    }

    private void publishUpdated(Job job) {
        eventProducer.publishJobUpdated(JobUpdatedEvent.builder()
                .jobId(job.getId()).companyId(job.getCompanyId()).title(job.getTitle()).occurredAt(LocalDateTime.now()).build());
    }

    private void transitionStatus(Job job, JobStatus target) {
        if (!job.getStatus().canTransitionTo(target)) {
            throw new InvalidStatusTransitionException(job.getStatus(), target);
        }
    }

    private void validateReadyToPublish(Job job) {
        List<String> missing = new ArrayList<>();
        if (job.getDepartmentId() == null) missing.add("department");
        if (job.getNumberOfOpenings() == null || job.getNumberOfOpenings() <= 0) missing.add("number of openings");
        if (job.getRecruiterId() == null) missing.add("recruiter assignment");
        if (job.getApplicationDeadline() != null && job.getApplicationDeadline().isBefore(LocalDate.now())) {
            throw new JobValidationException(ErrorCode.PAST_APPLICATION_DEADLINE, "Application deadline cannot be in the past");
        }
        boolean hasEnabledStage = jobPipelineStageRepository.findAllByJobIdOrderByStageOrderAsc(job.getId()).stream()
                .anyMatch(JobPipelineStage::isEnabled);
        if (!hasEnabledStage) missing.add("at least one enabled hiring stage");

        if (!missing.isEmpty()) {
            throw new JobValidationException(ErrorCode.JOB_NOT_READY_TO_PUBLISH,
                    "Cannot publish -- missing: " + String.join(", ", missing));
        }
    }

    private void validateExperienceRange(Integer min, Integer max) {
        if (min != null && max != null && min > max) {
            throw new JobValidationException(ErrorCode.INVALID_EXPERIENCE_RANGE, "Minimum experience cannot exceed maximum experience");
        }
    }

    private void validateSalaryRange(BigDecimal min, BigDecimal max) {
        if (min != null && max != null && min.compareTo(max) > 0) {
            throw new JobValidationException(ErrorCode.INVALID_SALARY_RANGE, "Minimum salary cannot exceed maximum salary");
        }
    }

    private String resolveDepartmentName(UUID departmentId, Map<UUID, String> cache) {
        if (departmentId == null) {
            return null;
        }
        return cache.computeIfAbsent(departmentId, id -> {
            try {
                var response = companyServiceClient.getDepartment(id);
                return response != null && response.getData() != null ? response.getData().getDepartmentName() : null;
            } catch (Exception e) {
                log.warn("Could not resolve department name for {}: {}", id, e.getMessage());
                return null;
            }
        });
    }

    private Job findJobInCompanyOrThrow(UUID jobId, CurrentUser currentUser) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));
        if (!job.getCompanyId().equals(currentUser.getCompanyId())) {
            // Deliberately the same 404 a truly-missing job would return --
            // never reveal that a job exists in a company you don't belong to.
            throw new ResourceNotFoundException(ErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId);
        }
        return job;
    }

    private Job findEditableJobOrThrow(UUID jobId, CurrentUser currentUser) {
        Job job = findJobInCompanyOrThrow(jobId, currentUser);
        requireWriteAccess(currentUser);
        if (job.getStatus() == JobStatus.ARCHIVED || job.getStatus() == JobStatus.CLOSED) {
            throw new JobValidationException(ErrorCode.INVALID_STATUS_TRANSITION,
                    "Cannot edit a job that is " + job.getStatus() + " -- restore it first");
        }
        return job;
    }

    private void requireWriteAccess(CurrentUser currentUser) {
        boolean fullWriteRole = currentUser.getRole() != null && PlatformRole.FULL_WRITE_ROLES.contains(currentUser.getRole());
        boolean hiringManagerWithEditGrant = PlatformRole.HIRING_MANAGER.equals(currentUser.getRole())
                && currentUser.hasPermission(PlatformRole.JOB_EDIT_PERMISSION);
        if (!fullWriteRole && !hiringManagerWithEditGrant) {
            throw new AccessDeniedException("Hiring Manager is view-only unless granted the " + PlatformRole.JOB_EDIT_PERMISSION + " permission");
        }
    }
}
