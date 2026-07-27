package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.constants.CandidateAssessmentStatus;
import com.cadence.codingassessmentservice.dto.request.CreateAssessmentRequest;
import com.cadence.codingassessmentservice.dto.request.UpdateAssessmentRequest;
import com.cadence.codingassessmentservice.dto.response.AssessmentResponse;
import com.cadence.codingassessmentservice.entity.Assessment;
import com.cadence.codingassessmentservice.entity.AssessmentQuestion;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.exception.AssessmentConflictException;
import com.cadence.codingassessmentservice.exception.ErrorCode;
import com.cadence.codingassessmentservice.exception.ResourceNotFoundException;
import com.cadence.codingassessmentservice.feign.JobServiceClient;
import com.cadence.codingassessmentservice.feign.dto.JobDetailDto;
import com.cadence.codingassessmentservice.kafka.event.AssessmentCreatedEvent;
import com.cadence.codingassessmentservice.kafka.producer.CodingAssessmentEventProducer;
import com.cadence.codingassessmentservice.mapper.AssessmentMapper;
import com.cadence.codingassessmentservice.repository.AssessmentQuestionRepository;
import com.cadence.codingassessmentservice.repository.AssessmentRepository;
import com.cadence.codingassessmentservice.repository.CandidateAssessmentRepository;
import com.cadence.codingassessmentservice.service.AssessmentEligibilityService;
import com.cadence.codingassessmentservice.service.AssessmentService;
import com.cadence.codingassessmentservice.service.CandidateAssessmentService;
import com.cadence.codingassessmentservice.service.EligibleCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentServiceImpl implements AssessmentService {

    private final AssessmentRepository assessmentRepository;
    private final AssessmentQuestionRepository assessmentQuestionRepository;
    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final JobServiceClient jobServiceClient;
    private final AssessmentEligibilityService assessmentEligibilityService;
    private final CandidateAssessmentService candidateAssessmentService;
    private final CodingAssessmentEventProducer eventProducer;
    private final AssessmentMapper assessmentMapper;

    @Override
    @Transactional
    public AssessmentResponse createAssessment(UUID companyId, UUID recruiterId, CreateAssessmentRequest request) {
        Assessment assessment = Assessment.builder()
                .companyId(companyId)
                .jobId(request.getJobId())
                .createdByRecruiterId(recruiterId)
                .name(request.getName())
                .type(request.getType())
                .status(AssessmentStatus.DRAFT)
                .difficulty(request.getDifficulty())
                .durationMinutes(request.getDurationMinutes())
                .questionCount(request.getQuestionCount())
                .passingScorePercent(request.getPassingScorePercent())
                .totalMarks(request.getTotalMarks())
                .compilerVersion(request.getCompilerVersion())
                .allowedLanguages(assessmentMapper.joinLanguages(request.getAllowedLanguages()))
                .negativeMarking(request.isNegativeMarking())
                .antiCheatMonitoring(request.isAntiCheatMonitoring())
                .plagiarismDetection(request.isPlagiarismDetection())
                .startDate(request.getStartDate())
                .expiryDate(request.getExpiryDate())
                .candidateInstructions(request.getCandidateInstructions())
                .build();
        assessment = assessmentRepository.save(assessment);

        if (request.getQuestionIds() != null && !request.getQuestionIds().isEmpty()) {
            linkQuestions(assessment.getId(), request.getQuestionIds());
        }

        if (request.isPublishNow()) {
            publishAssessment(companyId, assessment.getId());
        }

        return toEnrichedResponse(assessmentRepository.findById(assessment.getId()).orElseThrow());
    }

    @Override
    @Transactional
    public AssessmentResponse updateAssessment(UUID companyId, UUID assessmentId, UpdateAssessmentRequest request) {
        Assessment assessment = findByIdAndCompanyOrThrow(companyId, assessmentId);
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_DRAFT, "Only a draft assessment can be edited");
        }
        assessment.setName(request.getName());
        assessment.setType(request.getType());
        assessment.setAllowedLanguages(assessmentMapper.joinLanguages(request.getAllowedLanguages()));
        assessment.setDifficulty(request.getDifficulty());
        assessment.setDurationMinutes(request.getDurationMinutes());
        assessment.setQuestionCount(request.getQuestionCount());
        assessment.setPassingScorePercent(request.getPassingScorePercent());
        assessment.setTotalMarks(request.getTotalMarks());
        assessment.setCompilerVersion(request.getCompilerVersion());
        assessment.setNegativeMarking(request.isNegativeMarking());
        assessment.setAntiCheatMonitoring(request.isAntiCheatMonitoring());
        assessment.setPlagiarismDetection(request.isPlagiarismDetection());
        assessment.setStartDate(request.getStartDate());
        assessment.setExpiryDate(request.getExpiryDate());
        assessment.setCandidateInstructions(request.getCandidateInstructions());
        assessmentRepository.save(assessment);
        return toEnrichedResponse(assessment);
    }

    @Override
    @Transactional
    public void deleteAssessment(UUID companyId, UUID assessmentId) {
        Assessment assessment = findByIdAndCompanyOrThrow(companyId, assessmentId);
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_DRAFT, "Only a draft assessment can be deleted");
        }
        assessmentRepository.delete(assessment);
    }

    @Override
    @Transactional
    public void publishAssessment(UUID companyId, UUID assessmentId) {
        Assessment assessment = findByIdAndCompanyOrThrow(companyId, assessmentId);
        if (assessment.getStatus() != AssessmentStatus.DRAFT) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_ALREADY_PUBLISHED, "Only a draft assessment can be published");
        }
        assessment.setStatus(AssessmentStatus.PUBLISHED);
        assessmentRepository.save(assessment);

        List<EligibleCandidate> eligible = assessmentEligibilityService.findUninvitedEligibleCandidates(assessment.getJobId(), assessmentId);
        for (EligibleCandidate candidate : eligible) {
            candidateAssessmentService.inviteCandidate(assessmentId, candidate.applicationId(), assessment.getJobId(), candidate.candidateId());
        }
        log.info("Published assessment {} -- invited {} eligible candidate(s)", assessmentId, eligible.size());

        eventProducer.publishAssessmentCreated(AssessmentCreatedEvent.builder()
                .assessmentId(assessmentId).jobId(assessment.getJobId()).companyId(companyId)
                .occurredAt(LocalDateTime.now()).build());
    }

    @Override
    @Transactional
    public AssessmentResponse cloneAssessment(UUID companyId, UUID assessmentId) {
        Assessment source = findByIdAndCompanyOrThrow(companyId, assessmentId);
        Assessment clone = Assessment.builder()
                .companyId(source.getCompanyId())
                .jobId(source.getJobId())
                .createdByRecruiterId(source.getCreatedByRecruiterId())
                .name(source.getName() + " (Copy)")
                .type(source.getType())
                .status(AssessmentStatus.DRAFT)
                .difficulty(source.getDifficulty())
                .durationMinutes(source.getDurationMinutes())
                .questionCount(source.getQuestionCount())
                .passingScorePercent(source.getPassingScorePercent())
                .totalMarks(source.getTotalMarks())
                .compilerVersion(source.getCompilerVersion())
                .allowedLanguages(source.getAllowedLanguages())
                .negativeMarking(source.isNegativeMarking())
                .antiCheatMonitoring(source.isAntiCheatMonitoring())
                .plagiarismDetection(source.isPlagiarismDetection())
                .startDate(source.getStartDate())
                .expiryDate(source.getExpiryDate())
                .candidateInstructions(source.getCandidateInstructions())
                .build();
        clone = assessmentRepository.save(clone);

        List<UUID> questionIds = assessmentQuestionRepository.findAllByAssessmentIdOrderByDisplayOrderAsc(assessmentId).stream()
                .map(AssessmentQuestion::getQuestionId).toList();
        if (!questionIds.isEmpty()) {
            linkQuestions(clone.getId(), questionIds);
        }

        return toEnrichedResponse(clone);
    }

    @Override
    @Transactional
    public void archiveAssessment(UUID companyId, UUID assessmentId) {
        Assessment assessment = findByIdAndCompanyOrThrow(companyId, assessmentId);
        assessment.setStatus(AssessmentStatus.ARCHIVED);
        assessmentRepository.save(assessment);
    }

    @Override
    @Transactional
    public void closeAssessment(UUID companyId, UUID assessmentId) {
        Assessment assessment = findByIdAndCompanyOrThrow(companyId, assessmentId);
        assessment.setStatus(AssessmentStatus.CLOSED);
        assessmentRepository.save(assessment);
    }

    @Override
    @Transactional
    public void assignQuestions(UUID companyId, UUID assessmentId, List<UUID> questionIds) {
        findByIdAndCompanyOrThrow(companyId, assessmentId);
        assessmentQuestionRepository.deleteAllByAssessmentId(assessmentId);
        linkQuestions(assessmentId, questionIds);
    }

    @Override
    @Transactional
    public void sendReminders(UUID companyId, UUID assessmentId) {
        findByIdAndCompanyOrThrow(companyId, assessmentId);
        List<CandidateAssessment> notStarted = candidateAssessmentRepository
                .search(assessmentId, CandidateAssessmentStatus.NOT_STARTED, Pageable.unpaged())
                .getContent();
        for (CandidateAssessment ca : notStarted) {
            candidateAssessmentService.sendReminder(ca.getId());
        }
        log.info("Sent reminders for assessment {} to {} not-started candidate(s)", assessmentId, notStarted.size());
    }

    @Override
    @Transactional
    public void remindCandidate(UUID companyId, UUID assessmentId, UUID applicationId) {
        findByIdAndCompanyOrThrow(companyId, assessmentId);
        CandidateAssessment ca = candidateAssessmentRepository.findByAssessmentIdAndApplicationId(assessmentId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No invitation found for this candidate"));
        candidateAssessmentService.sendReminder(ca.getId());
    }

    @Override
    @Transactional
    public void moveToNextStage(UUID companyId, UUID assessmentId, UUID applicationId) {
        findByIdAndCompanyOrThrow(companyId, assessmentId);
        CandidateAssessment ca = candidateAssessmentRepository.findByAssessmentIdAndApplicationId(assessmentId, applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.CANDIDATE_ASSESSMENT_NOT_FOUND, "No submission found for this candidate"));
        if (ca.getStatus() != CandidateAssessmentStatus.COMPLETED) {
            throw new AssessmentConflictException(ErrorCode.ASSESSMENT_NOT_IN_PROGRESS, "Only a completed submission can be moved to the next stage");
        }
        eventProducer.publishCodingAssessmentCompleted(com.cadence.codingassessmentservice.kafka.event.CodingAssessmentCompletedEvent.builder()
                .applicationId(applicationId).score(ca.getTotalScore()).build());
    }

    private void linkQuestions(UUID assessmentId, List<UUID> questionIds) {
        List<AssessmentQuestion> links = new java.util.ArrayList<>();
        int order = 0;
        for (UUID questionId : questionIds) {
            links.add(AssessmentQuestion.builder().assessmentId(assessmentId).questionId(questionId).displayOrder(order++).build());
        }
        assessmentQuestionRepository.saveAll(links);
    }

    private AssessmentResponse toEnrichedResponse(Assessment assessment) {
        AssessmentResponse response = assessmentMapper.toResponse(assessment);
        JobDetailDto job = jobServiceClient.getJobDetail(assessment.getJobId()).getData();
        if (job != null) {
            response.setJobTitle(job.getTitle());
        }
        return response;
    }

    private Assessment findByIdAndCompanyOrThrow(UUID companyId, UUID assessmentId) {
        Assessment assessment = assessmentRepository.findById(assessmentId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.ASSESSMENT_NOT_FOUND, "Assessment not found: " + assessmentId));
        if (!assessment.getCompanyId().equals(companyId)) {
            throw new ResourceNotFoundException(ErrorCode.ASSESSMENT_NOT_FOUND, "Assessment not found: " + assessmentId);
        }
        return assessment;
    }
}
