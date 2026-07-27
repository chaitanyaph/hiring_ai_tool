package com.cadence.candidateservice.service.impl;

import com.cadence.candidateservice.dto.response.SavedJobResponse;
import com.cadence.candidateservice.entity.SavedJob;
import com.cadence.candidateservice.exception.ErrorCode;
import com.cadence.candidateservice.exception.ResourceNotFoundException;
import com.cadence.candidateservice.kafka.event.JobSavedEvent;
import com.cadence.candidateservice.kafka.event.JobUnsavedEvent;
import com.cadence.candidateservice.kafka.producer.CandidateEventProducer;
import com.cadence.candidateservice.mapper.SavedJobMapper;
import com.cadence.candidateservice.repository.SavedJobRepository;
import com.cadence.candidateservice.security.CurrentUser;
import com.cadence.candidateservice.service.SavedJobService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SavedJobServiceImpl implements SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final SavedJobMapper savedJobMapper;
    private final CandidateEventProducer eventProducer;

    @Override
    @Transactional
    public SavedJobResponse saveJob(CurrentUser candidate, UUID jobId) {
        SavedJob existing = savedJobRepository.findByCandidateIdAndJobId(candidate.getUserId(), jobId).orElse(null);
        if (existing != null) {
            return savedJobMapper.toResponse(existing);
        }

        SavedJob savedJob = savedJobRepository.save(SavedJob.builder()
                .candidateId(candidate.getUserId())
                .jobId(jobId)
                .build());

        eventProducer.publishJobSaved(JobSavedEvent.builder()
                .candidateId(candidate.getUserId()).jobId(jobId).occurredAt(LocalDateTime.now()).build());

        return savedJobMapper.toResponse(savedJob);
    }

    @Override
    @Transactional
    public void unsaveJob(CurrentUser candidate, UUID jobId) {
        if (!savedJobRepository.existsByCandidateIdAndJobId(candidate.getUserId(), jobId)) {
            throw new ResourceNotFoundException(ErrorCode.SAVED_JOB_NOT_FOUND, "This job is not in your saved list");
        }
        savedJobRepository.deleteByCandidateIdAndJobId(candidate.getUserId(), jobId);

        eventProducer.publishJobUnsaved(JobUnsavedEvent.builder()
                .candidateId(candidate.getUserId()).jobId(jobId).occurredAt(LocalDateTime.now()).build());
    }

    @Override
    @Transactional(readOnly = true)
    public List<SavedJobResponse> listSavedJobs(CurrentUser candidate) {
        return savedJobMapper.toResponseList(savedJobRepository.findAllByCandidateIdOrderBySavedAtDesc(candidate.getUserId()));
    }
}
