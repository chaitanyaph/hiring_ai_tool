package com.cadence.aiinterviewservice.service.impl;

import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import com.cadence.aiinterviewservice.entity.CandidateShortlist;
import com.cadence.aiinterviewservice.exception.ErrorCode;
import com.cadence.aiinterviewservice.exception.ResourceNotFoundException;
import com.cadence.aiinterviewservice.feign.ResumeParserServiceClient;
import com.cadence.aiinterviewservice.feign.dto.MissingSkillDto;
import com.cadence.aiinterviewservice.feign.dto.ResumeMatchDto;
import com.cadence.aiinterviewservice.kafka.event.CandidateShortlistedEvent;
import com.cadence.aiinterviewservice.kafka.event.ResumeAnalyzedEvent;
import com.cadence.aiinterviewservice.kafka.producer.AiInterviewEventProducer;
import com.cadence.aiinterviewservice.repository.CandidateShortlistRepository;
import com.cadence.aiinterviewservice.service.ShortlistingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The AI Shortlisting orchestration service: turns a resume match
 * score into a SHORTLISTED/REJECTED/MANUAL_REVIEW decision, and
 * handles the Manual Review queue's single/bulk recruiter actions.
 * One row per application_id (unique) -- a re-analysis updates the
 * row in place, same "recalculate replaces" precedent as ResumeMatch.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ShortlistingServiceImpl implements ShortlistingService {

    private final CandidateShortlistRepository candidateShortlistRepository;
    private final ResumeParserServiceClient resumeParserServiceClient;
    private final AiInterviewEventProducer eventProducer;

    @Value("${ai-interview.shortlisting.auto-shortlist-min-score}")
    private int autoShortlistMinScore;

    @Override
    @Transactional
    public void handleResumeAnalyzed(ResumeAnalyzedEvent event) {
        ResumeMatchDto match = resumeParserServiceClient.getResumeMatch(event.getApplicationId()).getData();
        if (match == null) {
            log.warn("No resume match found in Resume Parser Service for application {} -- skipping shortlisting", event.getApplicationId());
            return;
        }

        int score = event.getOverallMatchScore() != null ? event.getOverallMatchScore() : 0;
        ShortlistDecision decision = decisionFor(score);
        String reason = reasonFor(decision, match);

        CandidateShortlist shortlist = candidateShortlistRepository.findByApplicationId(event.getApplicationId())
                .map(existing -> {
                    existing.setOverallMatchScore(score);
                    existing.setDecision(decision);
                    existing.setReason(reason);
                    existing.setResumeMatchId(match.getId());
                    existing.setDepartmentId(match.getDepartmentId());
                    existing.setDecidedAt(LocalDateTime.now());
                    return existing;
                })
                .orElseGet(() -> CandidateShortlist.builder()
                        .applicationId(event.getApplicationId())
                        .jobId(event.getJobId())
                        .departmentId(match.getDepartmentId())
                        .candidateId(event.getCandidateId())
                        .resumeMatchId(match.getId())
                        .overallMatchScore(score)
                        .decision(decision)
                        .reason(reason)
                        .decidedAt(LocalDateTime.now())
                        .build());
        candidateShortlistRepository.save(shortlist);

        eventProducer.publishCandidateShortlisted(CandidateShortlistedEvent.builder()
                .applicationId(event.getApplicationId())
                .jobId(event.getJobId())
                .candidateId(event.getCandidateId())
                .decision(decision)
                .overallMatchScore(score)
                .occurredAt(LocalDateTime.now())
                .build());
    }

    @Override
    @Transactional
    public void shortlistOne(UUID applicationId) {
        applyManualDecision(applicationId, ShortlistDecision.SHORTLISTED, "Manually shortlisted by recruiter");
    }

    @Override
    @Transactional
    public void rejectOne(UUID applicationId) {
        applyManualDecision(applicationId, ShortlistDecision.REJECTED, "Manually rejected by recruiter");
    }

    @Override
    @Transactional
    public void bulkShortlist(List<UUID> applicationIds) {
        applyBulkDecision(applicationIds, ShortlistDecision.SHORTLISTED, "Bulk-shortlisted by recruiter");
    }

    @Override
    @Transactional
    public void bulkReject(List<UUID> applicationIds) {
        applyBulkDecision(applicationIds, ShortlistDecision.REJECTED, "Bulk-rejected by recruiter");
    }

    @Override
    @Transactional
    public void assignRecruiter(List<UUID> applicationIds, UUID recruiterId) {
        List<CandidateShortlist> shortlists = candidateShortlistRepository.findAllByApplicationIdIn(applicationIds);
        shortlists.forEach(s -> s.setAssignedRecruiterId(recruiterId));
        candidateShortlistRepository.saveAll(shortlists);
    }

    private void applyManualDecision(UUID applicationId, ShortlistDecision decision, String reason) {
        CandidateShortlist shortlist = candidateShortlistRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.SHORTLIST_NOT_FOUND, "No shortlist record found for application " + applicationId));
        shortlist.setDecision(decision);
        shortlist.setReason(reason);
        shortlist.setDecidedAt(LocalDateTime.now());
        candidateShortlistRepository.save(shortlist);

        eventProducer.publishCandidateShortlisted(CandidateShortlistedEvent.builder()
                .applicationId(shortlist.getApplicationId())
                .jobId(shortlist.getJobId())
                .candidateId(shortlist.getCandidateId())
                .decision(decision)
                .overallMatchScore(shortlist.getOverallMatchScore())
                .occurredAt(LocalDateTime.now())
                .build());
    }

    private void applyBulkDecision(List<UUID> applicationIds, ShortlistDecision decision, String reason) {
        List<CandidateShortlist> shortlists = candidateShortlistRepository.findAllByApplicationIdIn(applicationIds);
        LocalDateTime now = LocalDateTime.now();
        for (CandidateShortlist shortlist : shortlists) {
            shortlist.setDecision(decision);
            shortlist.setReason(reason);
            shortlist.setDecidedAt(now);
        }
        candidateShortlistRepository.saveAll(shortlists);

        for (CandidateShortlist shortlist : shortlists) {
            eventProducer.publishCandidateShortlisted(CandidateShortlistedEvent.builder()
                    .applicationId(shortlist.getApplicationId())
                    .jobId(shortlist.getJobId())
                    .candidateId(shortlist.getCandidateId())
                    .decision(decision)
                    .overallMatchScore(shortlist.getOverallMatchScore())
                    .occurredAt(now)
                    .build());
        }
    }

    /**
     * Strict binary rule: no manual-review middle band. MANUAL_REVIEW remains a
     * valid ShortlistDecision value (still settable by a recruiter through other
     * means) but is no longer produced by automatic scoring.
     */
    private ShortlistDecision decisionFor(int score) {
        return score >= autoShortlistMinScore ? ShortlistDecision.SHORTLISTED : ShortlistDecision.REJECTED;
    }

    private String reasonFor(ShortlistDecision decision, ResumeMatchDto match) {
        return switch (decision) {
            case SHORTLISTED -> {
                Optional<String> topSkills = topSkillNames(match);
                yield topSkills.map(s -> "Strong match on " + s).orElse("Overall match score meets the auto-shortlist threshold");
            }
            case REJECTED -> {
                Optional<String> missingRequired = missingRequiredSkillNames(match);
                yield missingRequired.map(s -> "Missing required skills: " + s).orElse("Overall match score is below the auto-shortlist threshold");
            }
            case MANUAL_REVIEW -> "Borderline match score -- needs recruiter judgment";
        };
    }

    private Optional<String> topSkillNames(ResumeMatchDto match) {
        if (match.getMatchedSkills() == null || match.getMatchedSkills().isEmpty()) {
            return Optional.empty();
        }
        return Optional.of(match.getMatchedSkills().stream().limit(3)
                .map(s -> s.getSkillName()).collect(Collectors.joining(", ")));
    }

    private Optional<String> missingRequiredSkillNames(ResumeMatchDto match) {
        if (match.getMissingSkills() == null || match.getMissingSkills().isEmpty()) {
            return Optional.empty();
        }
        String names = match.getMissingSkills().stream()
                .filter(MissingSkillDto::isRequired)
                .map(MissingSkillDto::getSkillName)
                .collect(Collectors.joining(", "));
        return names.isBlank() ? Optional.empty() : Optional.of(names);
    }
}
