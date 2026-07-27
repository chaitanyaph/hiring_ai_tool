package com.cadence.jobservice.service.impl;

import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.dto.request.SaveTemplateRequest;
import com.cadence.jobservice.dto.response.JobDetailResponse;
import com.cadence.jobservice.dto.response.JobTemplateResponse;
import com.cadence.jobservice.entity.*;
import com.cadence.jobservice.exception.ErrorCode;
import com.cadence.jobservice.exception.ResourceNotFoundException;
import com.cadence.jobservice.kafka.event.JobCreatedEvent;
import com.cadence.jobservice.kafka.producer.JobEventProducer;
import com.cadence.jobservice.mapper.JobTemplateMapper;
import com.cadence.jobservice.repository.*;
import com.cadence.jobservice.security.CurrentUser;
import com.cadence.jobservice.service.JobService;
import com.cadence.jobservice.service.JobTemplateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class JobTemplateServiceImpl implements JobTemplateService {

    private final JobTemplateRepository jobTemplateRepository;
    private final JobRepository jobRepository;
    private final JobDescriptionRepository jobDescriptionRepository;
    private final JobRequirementsRepository jobRequirementsRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobBenefitRepository jobBenefitRepository;
    private final JobPipelineStageRepository jobPipelineStageRepository;
    private final JobStatusHistoryRepository jobStatusHistoryRepository;
    private final JobAuditRepository jobAuditRepository;

    private final JobTemplateMapper jobTemplateMapper;
    private final JobEventProducer eventProducer;
    private final JobService jobServiceForAssembly;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    @Transactional
    public JobTemplateResponse saveAsTemplate(UUID jobId, SaveTemplateRequest request, CurrentUser currentUser) {
        Job job = jobRepository.findById(jobId)
                .filter(j -> j.getCompanyId().equals(currentUser.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.JOB_NOT_FOUND, "Job not found: " + jobId));

        JobRequirements requirements = jobRequirementsRepository.findByJobId(jobId).orElse(null);
        String descriptionHtml = jobDescriptionRepository.findByJobId(jobId)
                .map(JobDescription::getDescriptionHtml).orElse(null);

        TemplateSnapshot snapshot = TemplateSnapshot.builder()
                .location(job.getLocation())
                .workType(job.getWorkType())
                .employmentType(job.getEmploymentType())
                .numberOfOpenings(job.getNumberOfOpenings())
                .descriptionHtml(descriptionHtml)
                .minExperienceYears(requirements != null ? requirements.getMinExperienceYears() : null)
                .maxExperienceYears(requirements != null ? requirements.getMaxExperienceYears() : null)
                .education(requirements != null ? requirements.getEducation() : null)
                .certifications(requirements != null ? requirements.getCertifications() : null)
                .languages(requirements != null ? requirements.getLanguages() : null)
                .minSalary(requirements != null ? requirements.getMinSalary() : null)
                .maxSalary(requirements != null ? requirements.getMaxSalary() : null)
                .salaryCurrency(requirements != null ? requirements.getSalaryCurrency() : null)
                .noticePeriodDays(requirements != null ? requirements.getNoticePeriodDays() : null)
                .responsibilities(requirements != null ? requirements.getResponsibilities() : null)
                .benefits(jobBenefitRepository.findAllByJobId(jobId).stream().map(JobBenefit::getBenefitText).toList())
                .skills(jobSkillRepository.findAllByJobId(jobId).stream()
                        .map(s -> TemplateSnapshot.SkillSnapshot.builder().skillName(s.getSkillName()).skillType(s.getSkillType()).build())
                        .toList())
                .stages(jobPipelineStageRepository.findAllByJobIdOrderByStageOrderAsc(jobId).stream()
                        .map(s -> TemplateSnapshot.StageSnapshot.builder().stageName(s.getStageName()).stageOrder(s.getStageOrder()).enabled(s.isEnabled()).build())
                        .toList())
                .build();

        String json = writeJson(snapshot);
        JobTemplate template = JobTemplate.builder()
                .companyId(currentUser.getCompanyId())
                .templateName(request.getTemplateName().trim())
                .templateDataJson(json)
                .createdBy(currentUser.getUserId())
                .build();
        template = jobTemplateRepository.save(template);

        return jobTemplateMapper.toResponse(template);
    }

    @Override
    @Transactional(readOnly = true)
    public List<JobTemplateResponse> listTemplates(CurrentUser currentUser) {
        return jobTemplateRepository.findAllByCompanyIdOrderByTemplateNameAsc(currentUser.getCompanyId()).stream()
                .map(jobTemplateMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public JobDetailResponse createDraftFromTemplate(UUID templateId, CurrentUser currentUser) {
        JobTemplate template = jobTemplateRepository.findById(templateId)
                .filter(t -> t.getCompanyId().equals(currentUser.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + templateId));

        TemplateSnapshot snapshot = readJson(template.getTemplateDataJson());

        Job job = Job.builder()
                .companyId(currentUser.getCompanyId())
                .jobCode(generateUniqueJobCode(currentUser.getCompanyId()))
                .title(template.getTemplateName())
                .location(snapshot.getLocation())
                .workType(snapshot.getWorkType())
                .employmentType(snapshot.getEmploymentType())
                .numberOfOpenings(snapshot.getNumberOfOpenings())
                .status(JobStatus.DRAFT)
                .createdBy(currentUser.getUserId())
                .updatedBy(currentUser.getUserId())
                .build();
        job = jobRepository.save(job);
        Job finalJob = job;

        JobDescription description = JobDescription.builder().jobId(finalJob.getId()).descriptionHtml(snapshot.getDescriptionHtml()).build();
        jobDescriptionRepository.save(description);

        JobRequirements requirements = JobRequirements.builder()
                .jobId(finalJob.getId())
                .minExperienceYears(snapshot.getMinExperienceYears())
                .maxExperienceYears(snapshot.getMaxExperienceYears())
                .education(snapshot.getEducation())
                .certifications(snapshot.getCertifications())
                .languages(snapshot.getLanguages())
                .minSalary(snapshot.getMinSalary())
                .maxSalary(snapshot.getMaxSalary())
                .salaryCurrency(snapshot.getSalaryCurrency() != null ? snapshot.getSalaryCurrency() : "INR")
                .noticePeriodDays(snapshot.getNoticePeriodDays())
                .responsibilities(snapshot.getResponsibilities())
                .build();
        jobRequirementsRepository.save(requirements);

        if (snapshot.getSkills() != null) {
            snapshot.getSkills().forEach(s -> jobSkillRepository.save(
                    JobSkill.builder().jobId(finalJob.getId()).skillName(s.getSkillName()).skillType(s.getSkillType()).build()));
        }
        if (snapshot.getBenefits() != null) {
            snapshot.getBenefits().forEach(b -> jobBenefitRepository.save(
                    JobBenefit.builder().jobId(finalJob.getId()).benefitText(b).build()));
        }
        if (snapshot.getStages() != null && !snapshot.getStages().isEmpty()) {
            snapshot.getStages().forEach(s -> jobPipelineStageRepository.save(
                    JobPipelineStage.builder().jobId(finalJob.getId()).stageName(s.getStageName())
                            .stageOrder(s.getStageOrder()).enabled(s.isEnabled()).systemDefault(false).build()));
        } else {
            int order = 1;
            for (String stageName : com.cadence.jobservice.constant.PipelineStageDefaults.DEFAULT_STAGES) {
                jobPipelineStageRepository.save(JobPipelineStage.builder()
                        .jobId(finalJob.getId()).stageName(stageName).stageOrder(order++).enabled(true).systemDefault(true).build());
            }
        }

        jobStatusHistoryRepository.save(JobStatusHistory.builder()
                .jobId(job.getId()).toStatus(JobStatus.DRAFT).changedBy(currentUser.getUserId())
                .reason("Created from template " + template.getTemplateName()).build());
        jobAuditRepository.save(JobAudit.builder()
                .jobId(job.getId()).action("JOB_CREATED_FROM_TEMPLATE").performedBy(currentUser.getUserId())
                .details("templateId=" + templateId).build());

        eventProducer.publishJobCreated(JobCreatedEvent.builder()
                .jobId(finalJob.getId()).companyId(finalJob.getCompanyId())
                .title(finalJob.getTitle()).jobCode(finalJob.getJobCode()).occurredAt(LocalDateTime.now()).build());

        return jobServiceForAssembly.getJobDetail(job.getId(), currentUser);
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID templateId, CurrentUser currentUser) {
        JobTemplate template = jobTemplateRepository.findById(templateId)
                .filter(t -> t.getCompanyId().equals(currentUser.getCompanyId()))
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + templateId));
        jobTemplateRepository.delete(template);
    }

    private String generateUniqueJobCode(UUID companyId) {
        String code;
        do {
            code = "JOB-" + LocalDate.now().getYear() + "-" + String.format("%05d", new java.util.Random().nextInt(100000));
        } while (jobRepository.existsByCompanyIdAndJobCode(companyId, code));
        return code;
    }

    private String writeJson(TemplateSnapshot snapshot) {
        try {
            return objectMapper.writeValueAsString(snapshot);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialize job template", e);
        }
    }

    private TemplateSnapshot readJson(String json) {
        try {
            return objectMapper.readValue(json, TemplateSnapshot.class);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to deserialize job template", e);
        }
    }
}
