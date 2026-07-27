package com.cadence.applicationservice.service;

import com.cadence.applicationservice.dto.request.ScoreUpdateRequest;
import com.cadence.applicationservice.dto.response.ApplicationResponse;
import com.cadence.applicationservice.entity.Application;
import com.cadence.applicationservice.exception.ResourceNotFoundException;
import com.cadence.applicationservice.mapper.ApplicationMapper;
import com.cadence.applicationservice.repository.ApplicationRepository;
import com.cadence.applicationservice.repository.ApplicationScoreRepository;
import com.cadence.applicationservice.service.impl.InternalScoreServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InternalScoreServiceImplTest {

    @Mock private ApplicationRepository applicationRepository;
    @Mock private ApplicationScoreRepository scoreRepository;
    @Mock private ApplicationMapper applicationMapper;

    @InjectMocks
    private InternalScoreServiceImpl internalScoreService;

    @Test
    void updateResumeScore_shouldSetScoreAndRecomputeOverall() {
        UUID applicationId = UUID.randomUUID();
        Application app = Application.builder().id(applicationId).aiInterviewScore(90).build();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        internalScoreService.updateResumeScore(applicationId, ScoreUpdateRequest.builder().score(70).source("resume-matching-service").build());

        assertThat(app.getResumeMatchScore()).isEqualTo(70);
        assertThat(app.getOverallScore()).isEqualTo(80); // average of 70 and 90
        verify(scoreRepository).save(any());
    }

    @Test
    void updateResumeScore_shouldThrow_whenApplicationNotFound() {
        UUID applicationId = UUID.randomUUID();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> internalScoreService.updateResumeScore(applicationId, ScoreUpdateRequest.builder().score(70).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateOverallScore_shouldOverrideDirectly() {
        UUID applicationId = UUID.randomUUID();
        Application app = Application.builder().id(applicationId).resumeMatchScore(50).aiInterviewScore(60).build();
        when(applicationRepository.findById(applicationId)).thenReturn(Optional.of(app));
        when(applicationRepository.save(any(Application.class))).thenAnswer(inv -> inv.getArgument(0));
        when(applicationMapper.toResponse(any(Application.class))).thenReturn(ApplicationResponse.builder().build());

        internalScoreService.updateOverallScore(applicationId, ScoreUpdateRequest.builder().score(95).source("weighted-model").build());

        assertThat(app.getOverallScore()).isEqualTo(95);
    }
}
