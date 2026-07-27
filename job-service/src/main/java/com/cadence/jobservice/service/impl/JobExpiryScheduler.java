package com.cadence.jobservice.service.impl;

import com.cadence.jobservice.constant.JobStatus;
import com.cadence.jobservice.entity.Job;
import com.cadence.jobservice.entity.JobStatusHistory;
import com.cadence.jobservice.repository.JobRepository;
import com.cadence.jobservice.repository.JobStatusHistoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * "Application Deadline cannot be a past date" is enforced at
 * write-time, but a job that was published with a future deadline still
 * needs to transition to EXPIRED once that date passes -- nothing in
 * the UI triggers this, so it has to be a scheduled sweep.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JobExpiryScheduler {

    private final JobRepository jobRepository;
    private final JobStatusHistoryRepository jobStatusHistoryRepository;

    @Scheduled(cron = "0 0 * * * *")
    @Transactional
    public void expireOverdueJobs() {
        List<Job> overdue = jobRepository.findAllByStatusInAndApplicationDeadlineBefore(
                List.of(JobStatus.PUBLISHED, JobStatus.PAUSED), LocalDate.now());

        for (Job job : overdue) {
            JobStatus from = job.getStatus();
            job.setStatus(JobStatus.EXPIRED);
            jobRepository.save(job);
            jobStatusHistoryRepository.save(JobStatusHistory.builder()
                    .jobId(job.getId()).fromStatus(from).toStatus(JobStatus.EXPIRED)
                    .reason("Application deadline passed").build());
        }

        if (!overdue.isEmpty()) {
            log.info("Expired {} job(s) past their application deadline", overdue.size());
        }
    }
}
