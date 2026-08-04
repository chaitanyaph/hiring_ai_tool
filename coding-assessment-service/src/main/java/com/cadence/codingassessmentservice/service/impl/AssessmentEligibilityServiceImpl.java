package com.cadence.codingassessmentservice.service.impl;

import com.cadence.codingassessmentservice.constants.AssessmentStatus;
import com.cadence.codingassessmentservice.constants.HiringRecommendation;
import com.cadence.codingassessmentservice.entity.Assessment;
import com.cadence.codingassessmentservice.entity.AssessmentEligibility;
import com.cadence.codingassessmentservice.entity.CandidateAssessment;
import com.cadence.codingassessmentservice.kafka.event.CandidateRecommendedEvent;
import com.cadence.codingassessmentservice.repository.AssessmentEligibilityRepository;
import com.cadence.codingassessmentservice.repository.AssessmentRepository;
import com.cadence.codingassessmentservice.repository.CandidateAssessmentRepository;
import com.cadence.codingassessmentservice.service.AssessmentEligibilityService;
import com.cadence.codingassessmentservice.service.CandidateAssessmentService;
import com.cadence.codingassessmentservice.service.EligibleCandidate;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Tracks which applications are eligible to be invited to a coding
 * assessment. Application Service doesn't yet consume CandidateRecommended
 * itself (a documented gap on ai-interview-service's side), so this
 * service tracks eligibility locally from the same event -- this is
 * the pool "publish" invites from.
 *
 * A candidate recommended AFTER an assessment for their job is already
 * published used to be invited only on the next manual publish action
 * (which never comes again once a job's assessment is already live) --
 * handleCandidateRecommended now also invites immediately into any
 * already-published assessment for the job, closing that gap.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AssessmentEligibilityServiceImpl implements AssessmentEligibilityService {

    private final AssessmentEligibilityRepository assessmentEligibilityRepository;
    private final CandidateAssessmentRepository candidateAssessmentRepository;
    private final AssessmentRepository assessmentRepository;
    private final CandidateAssessmentService candidateAssessmentService;

    @Override
    @Transactional
    public void handleCandidateRecommended(CandidateRecommendedEvent event) {
        AssessmentEligibility eligibility = assessmentEligibilityRepository.findByApplicationId(event.getApplicationId())
                .map(existing -> {
                    existing.setHiringRecommendation(event.getHiringRecommendation());
                    existing.setRecommendedAt(event.getOccurredAt());
                    return existing;
                })
                .orElseGet(() -> AssessmentEligibility.builder()
                        .applicationId(event.getApplicationId())
                        .jobId(event.getJobId())
                        .candidateId(event.getCandidateId())
                        .hiringRecommendation(event.getHiringRecommendation())
                        .recommendedAt(event.getOccurredAt())
                        .build());
        assessmentEligibilityRepository.save(eligibility);

        if (event.getHiringRecommendation() != HiringRecommendation.PROCEED) {
            return;
        }
        for (Assessment assessment : assessmentRepository.findAllByJobIdAndStatus(event.getJobId(), AssessmentStatus.PUBLISHED)) {
            if (candidateAssessmentRepository.findByAssessmentIdAndApplicationId(assessment.getId(), event.getApplicationId()).isEmpty()) {
                candidateAssessmentService.inviteCandidate(assessment.getId(), event.getApplicationId(), event.getJobId(), event.getCandidateId());
                log.info("Auto-invited application {} to already-published assessment {} following late CandidateRecommended", event.getApplicationId(), assessment.getId());
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<EligibleCandidate> findUninvitedEligibleCandidates(UUID jobId, UUID assessmentId) {
        Set<UUID> alreadyInvited = candidateAssessmentRepository.findAllByAssessmentId(assessmentId).stream()
                .map(CandidateAssessment::getApplicationId)
                .collect(Collectors.toSet());

        return assessmentEligibilityRepository.findAllByJobId(jobId).stream()
                .filter(e -> e.getHiringRecommendation() == HiringRecommendation.PROCEED)
                .filter(e -> !alreadyInvited.contains(e.getApplicationId()))
                .map(e -> new EligibleCandidate(e.getApplicationId(), e.getCandidateId()))
                .toList();
    }
}
