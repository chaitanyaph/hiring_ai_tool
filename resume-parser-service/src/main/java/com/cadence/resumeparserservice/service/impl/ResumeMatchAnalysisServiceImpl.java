package com.cadence.resumeparserservice.service.impl;

import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.constants.ResumeMatchStatus;
import com.cadence.resumeparserservice.entity.ParsedResume;
import com.cadence.resumeparserservice.entity.ResumeMatch;
import com.cadence.resumeparserservice.exception.ErrorCode;
import com.cadence.resumeparserservice.exception.ParsingConflictException;
import com.cadence.resumeparserservice.exception.ResourceNotFoundException;
import com.cadence.resumeparserservice.feign.ApplicationServiceClient;
import com.cadence.resumeparserservice.feign.dto.ApplicationSummaryDto;
import com.cadence.resumeparserservice.kafka.event.ApplicationCreatedEvent;
import com.cadence.resumeparserservice.repository.ParsedResumeRepository;
import com.cadence.resumeparserservice.repository.ResumeMatchRepository;
import com.cadence.resumeparserservice.service.ResumeMatchAnalysisService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * The synchronous "front door" for match analysis -- mirrors
 * ResumeParsingServiceImpl's split from its pipeline runner (see that
 * class's Javadoc for why @Async needs a separate bean). This class
 * only ever moves a ResumeMatch row to AWAITING_PARSE/ANALYZING and
 * hands off; the actual compare-and-score work happens in
 * ResumeMatchAnalysisPipelineRunner.
 *
 * recalculate() is also the (documented, un-authorized-to-fix-upstream)
 * escape hatch for the ApplicationCreatedEvent.resumeId-null gap: it
 * re-fetches the application from Application Service, which by then
 * may have a resumeId even though the original event didn't.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeMatchAnalysisServiceImpl implements ResumeMatchAnalysisService {

    private final ResumeMatchRepository resumeMatchRepository;
    private final ParsedResumeRepository parsedResumeRepository;
    private final ApplicationServiceClient applicationServiceClient;
    private final StringRedisTemplate redisTemplate;
    private final ResumeMatchAnalysisPipelineRunner pipelineRunner;

    @Value("${resume-parser.idempotency.lock-ttl-seconds:300}")
    private long lockTtlSeconds;

    @Override
    @Transactional
    public void handleApplicationCreated(ApplicationCreatedEvent event) {
        if (resumeMatchRepository.findByApplicationId(event.getApplicationId()).isPresent()) {
            log.info("ResumeMatch already exists for application {} -- ignoring duplicate ApplicationCreated delivery", event.getApplicationId());
            return;
        }

        ResumeMatch resumeMatch = ResumeMatch.builder()
                .applicationId(event.getApplicationId())
                .jobId(event.getJobId())
                .candidateId(event.getCandidateId())
                .resumeId(event.getResumeId())
                .status(ResumeMatchStatus.AWAITING_RESUME)
                .build();
        resumeMatch = resumeMatchRepository.save(resumeMatch);

        if (event.getResumeId() == null) {
            log.info("Application {} created without a resumeId yet -- parked as AWAITING_RESUME", event.getApplicationId());
            return;
        }
        advanceOrQueue(resumeMatch, event.getResumeId());
    }

    @Override
    @Transactional
    public void recalculate(UUID applicationId) {
        ResumeMatch resumeMatch = resumeMatchRepository.findByApplicationId(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.MATCH_NOT_FOUND, "No resume match found for application " + applicationId));

        if (resumeMatch.getStatus() == ResumeMatchStatus.ANALYZING) {
            throw new ParsingConflictException(ErrorCode.MATCH_ALREADY_IN_PROGRESS, "This application's match analysis is already in progress");
        }

        UUID resumeId = resumeMatch.getResumeId();
        if (resumeId == null) {
            resumeId = fetchCurrentResumeId(resumeMatch.getJobId(), applicationId);
        }
        if (resumeId == null) {
            resumeMatch.setStatus(ResumeMatchStatus.AWAITING_RESUME);
            resumeMatchRepository.save(resumeMatch);
            log.info("Application {} still has no resumeId on recalculate -- remains AWAITING_RESUME", applicationId);
            return;
        }

        resumeMatch.setResumeId(resumeId);
        resumeMatch.setAttemptCount(resumeMatch.getAttemptCount() + 1);
        advanceOrQueue(resumeMatch, resumeId);
    }

    @Override
    @Transactional
    public void onResumeParsed(UUID resumeId) {
        List<ResumeMatch> awaiting = resumeMatchRepository.findAllByResumeIdAndStatus(resumeId, ResumeMatchStatus.AWAITING_PARSE);
        for (ResumeMatch resumeMatch : awaiting) {
            advanceOrQueue(resumeMatch, resumeId);
        }
    }

    /** Moves a row to ANALYZING + hands off to the pipeline if the resume is already PARSED, otherwise parks it as AWAITING_PARSE. */
    private void advanceOrQueue(ResumeMatch resumeMatch, UUID resumeId) {
        Optional<ParsedResume> parsedResume = parsedResumeRepository.findByResumeId(resumeId);
        if (parsedResume.isEmpty() || parsedResume.get().getStatus() != ParsingStatus.PARSED) {
            resumeMatch.setStatus(ResumeMatchStatus.AWAITING_PARSE);
            resumeMatchRepository.save(resumeMatch);
            return;
        }

        String lockKey = "resume-parser:lock:match:" + resumeMatch.getApplicationId();
        boolean acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTtlSeconds)));
        if (!acquired) {
            log.info("Match analysis for application {} is already being processed -- skipping (idempotency lock held)", resumeMatch.getApplicationId());
            return;
        }

        resumeMatch.setParsedResumeId(parsedResume.get().getId());
        resumeMatch.setStatus(ResumeMatchStatus.ANALYZING);
        resumeMatch = resumeMatchRepository.save(resumeMatch);

        pipelineRunner.run(resumeMatch.getId(), lockKey);
    }

    private UUID fetchCurrentResumeId(UUID jobId, UUID applicationId) {
        List<ApplicationSummaryDto> applications = applicationServiceClient.getApplicationsByJob(jobId).getData();
        if (applications == null) {
            return null;
        }
        return applications.stream()
                .filter(a -> applicationId.equals(a.getId()))
                .map(ApplicationSummaryDto::getResumeId)
                .findFirst()
                .orElse(null);
    }
}
