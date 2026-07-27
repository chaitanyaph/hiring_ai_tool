package com.cadence.applicationservice.service.impl;

import com.cadence.applicationservice.constant.ScoreType;
import com.cadence.applicationservice.dto.request.ScoreUpdateRequest;
import com.cadence.applicationservice.dto.response.ApplicationResponse;
import com.cadence.applicationservice.entity.Application;
import com.cadence.applicationservice.entity.ApplicationScore;
import com.cadence.applicationservice.exception.ErrorCode;
import com.cadence.applicationservice.exception.ResourceNotFoundException;
import com.cadence.applicationservice.mapper.ApplicationMapper;
import com.cadence.applicationservice.repository.ApplicationRepository;
import com.cadence.applicationservice.repository.ApplicationScoreRepository;
import com.cadence.applicationservice.service.InternalScoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InternalScoreServiceImpl implements InternalScoreService {

    private final ApplicationRepository applicationRepository;
    private final ApplicationScoreRepository scoreRepository;
    private final ApplicationMapper applicationMapper;

    @Override
    @Transactional
    public ApplicationResponse updateResumeScore(UUID applicationId, ScoreUpdateRequest request) {
        Application application = findOrThrow(applicationId);
        application.setResumeMatchScore(request.getScore());
        return recomputeAndSave(application, ScoreType.RESUME_MATCH, request);
    }

    @Override
    @Transactional
    public ApplicationResponse updateInterviewScore(UUID applicationId, ScoreUpdateRequest request) {
        Application application = findOrThrow(applicationId);
        application.setAiInterviewScore(request.getScore());
        return recomputeAndSave(application, ScoreType.AI_INTERVIEW, request);
    }

    @Override
    @Transactional
    public ApplicationResponse updateCodingScore(UUID applicationId, ScoreUpdateRequest request) {
        Application application = findOrThrow(applicationId);
        application.setCodingScore(request.getScore());
        return recomputeAndSave(application, ScoreType.CODING, request);
    }

    @Override
    @Transactional
    public ApplicationResponse updateOverallScore(UUID applicationId, ScoreUpdateRequest request) {
        Application application = findOrThrow(applicationId);
        // Overall is normally the derived average (see ScoreCalculator) -- an explicit call here lets a
        // future service that computes its own weighted score override that default outright.
        application.setOverallScore(request.getScore());
        application = applicationRepository.save(application);
        scoreRepository.save(ApplicationScore.builder()
                .applicationId(applicationId).scoreType(ScoreType.OVERALL)
                .scoreValue(request.getScore()).source(request.getSource()).build());
        return applicationMapper.toResponse(application);
    }

    private ApplicationResponse recomputeAndSave(Application application, ScoreType type, ScoreUpdateRequest request) {
        application.setOverallScore(ScoreCalculator.recomputeOverall(application));
        application = applicationRepository.save(application);
        scoreRepository.save(ApplicationScore.builder()
                .applicationId(application.getId()).scoreType(type)
                .scoreValue(request.getScore()).source(request.getSource()).build());
        return applicationMapper.toResponse(application);
    }

    private Application findOrThrow(UUID applicationId) {
        return applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.APPLICATION_NOT_FOUND, "Application not found"));
    }
}
