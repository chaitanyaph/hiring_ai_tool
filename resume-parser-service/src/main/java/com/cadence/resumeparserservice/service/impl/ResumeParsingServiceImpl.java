package com.cadence.resumeparserservice.service.impl;

import com.cadence.resumeparserservice.constants.LogLevel;
import com.cadence.resumeparserservice.constants.ParsingStatus;
import com.cadence.resumeparserservice.entity.ParsedResume;
import com.cadence.resumeparserservice.entity.ParserLog;
import com.cadence.resumeparserservice.exception.ErrorCode;
import com.cadence.resumeparserservice.exception.ParsingConflictException;
import com.cadence.resumeparserservice.exception.ResourceNotFoundException;
import com.cadence.resumeparserservice.repository.ParsedResumeRepository;
import com.cadence.resumeparserservice.repository.ParserLogRepository;
import com.cadence.resumeparserservice.service.ResumeParsingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * The synchronous "front door": lock acquisition, idempotency checks,
 * and state transitions all happen here on the calling thread (so a
 * bad retry request gets an immediate 404/409), then the actual
 * pipeline work is handed off to ResumeParsingPipelineRunner -- a
 * separate bean, so its @Async annotation actually takes effect
 * (Spring can't proxy a method calling another method on the *same*
 * bean instance).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeParsingServiceImpl implements ResumeParsingService {

    private final ParsedResumeRepository parsedResumeRepository;
    private final ParserLogRepository parserLogRepository;
    private final StringRedisTemplate redisTemplate;
    private final ResumeParsingPipelineRunner pipelineRunner;

    @Value("${resume-parser.idempotency.lock-ttl-seconds:300}")
    private long lockTtlSeconds;

    // Deliberately NOT @Transactional: this method hands off to an @Async
    // pipeline runner that re-reads the ParsedResume row by id on its own
    // thread. Wrapping this method in a transaction meant that read could
    // race the enclosing transaction's commit -- the async thread's
    // findById() sometimes ran before this method's own INSERT/UPDATE was
    // actually visible, came back empty, and Optional#ifPresent() silently
    // did nothing: no exception, no log, the row just sat at QUEUED forever.
    // Each repository .save() below still commits atomically on its own
    // (Spring Data's repository proxy is transactional per-call); they just
    // don't need to share ONE transaction with each other.
    @Override
    public void processResume(UUID resumeId, UUID candidateId, String checksum) {
        if (parsedResumeRepository.existsByResumeIdAndChecksumAndStatus(resumeId, checksum, ParsingStatus.PARSED)) {
            log.info("Resume {} already parsed with checksum {} -- skipping (duplicate parsing prevention)", resumeId, checksum);
            return;
        }

        String lockKey = lockKeyFor(resumeId, checksum);
        boolean acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTtlSeconds)));
        if (!acquired) {
            log.info("Resume {} is already being processed -- skipping (idempotency lock held)", resumeId);
            return;
        }

        ParsedResume parsedResume = parsedResumeRepository.findByResumeId(resumeId)
                .map(existing -> {
                    existing.setChecksum(checksum);
                    existing.setStatus(ParsingStatus.QUEUED);
                    existing.setFailureReason(null);
                    return existing;
                })
                .orElseGet(() -> ParsedResume.builder()
                        .resumeId(resumeId)
                        .candidateId(candidateId)
                        .checksum(checksum)
                        .status(ParsingStatus.QUEUED)
                        .build());
        parsedResume = parsedResumeRepository.save(parsedResume);
        writeLog(parsedResume, LogLevel.INFO, "Resume queued for parsing");

        pipelineRunner.run(parsedResume.getId(), lockKey);
    }

    // Same reasoning as processResume(): not @Transactional, so the row is
    // actually committed before the @Async pipeline runner tries to read it.
    @Override
    public void retryParsing(UUID resumeId) {
        ParsedResume parsedResume = parsedResumeRepository.findByResumeId(resumeId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.RESUME_NOT_FOUND,
                        "No parsing record found for resume " + resumeId));

        if (parsedResume.getStatus() != ParsingStatus.FAILED) {
            throw new ParsingConflictException(ErrorCode.PARSING_NOT_FAILED, "Only a failed parse can be retried");
        }

        String lockKey = lockKeyFor(resumeId, parsedResume.getChecksum());
        boolean acquired = Boolean.TRUE.equals(
                redisTemplate.opsForValue().setIfAbsent(lockKey, "1", Duration.ofSeconds(lockTtlSeconds)));
        if (!acquired) {
            throw new ParsingConflictException(ErrorCode.PARSING_ALREADY_IN_PROGRESS, "This resume is already being processed");
        }

        parsedResume.setAttemptCount(parsedResume.getAttemptCount() + 1);
        parsedResume.setStatus(ParsingStatus.QUEUED);
        parsedResume.setFailureReason(null);
        parsedResume = parsedResumeRepository.save(parsedResume);
        writeLog(parsedResume, LogLevel.INFO, "Retry requested (attempt " + parsedResume.getAttemptCount() + ")");

        pipelineRunner.run(parsedResume.getId(), lockKey);
    }

    @Override
    @Transactional
    public void handleCandidateDeleted(UUID candidateId) {
        List<ParsedResume> parsedResumes = parsedResumeRepository.findAllByCandidateId(candidateId);
        if (parsedResumes.isEmpty()) {
            return;
        }
        // Fully derived, regenerable data with no independent retention
        // requirement once its source candidate is gone (unlike the
        // resume *file* itself, which Resume Service deliberately keeps
        // for audit/recovery) -- so this is a real hard delete, not an
        // archive. Cascades to every child table via ON DELETE CASCADE.
        parsedResumeRepository.deleteAll(parsedResumes);
        log.info("Deleted {} parsed resume record(s) for candidate {} following CandidateDeleted", parsedResumes.size(), candidateId);
    }

    private String lockKeyFor(UUID resumeId, String checksum) {
        return "resume-parser:lock:" + resumeId + ":" + checksum;
    }

    private void writeLog(ParsedResume parsedResume, LogLevel level, String message) {
        parserLogRepository.save(ParserLog.builder()
                .parsedResumeId(parsedResume.getId())
                .resumeId(parsedResume.getResumeId())
                .logLevel(level)
                .message(message)
                .build());
    }
}
