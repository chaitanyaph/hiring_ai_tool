package com.cadence.aiinterviewservice.service.impl;

import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import com.cadence.aiinterviewservice.entity.CandidateShortlist;
import com.cadence.aiinterviewservice.feign.ResumeParserServiceClient;
import com.cadence.aiinterviewservice.feign.dto.FeignApiResponse;
import com.cadence.aiinterviewservice.feign.dto.MatchedSkillDto;
import com.cadence.aiinterviewservice.feign.dto.MissingSkillDto;
import com.cadence.aiinterviewservice.feign.dto.ResumeMatchDto;
import com.cadence.aiinterviewservice.kafka.event.ResumeAnalyzedEvent;
import com.cadence.aiinterviewservice.kafka.producer.AiInterviewEventProducer;
import com.cadence.aiinterviewservice.repository.CandidateShortlistRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ShortlistingServiceImplTest {

    @Mock private CandidateShortlistRepository candidateShortlistRepository;
    @Mock private ResumeParserServiceClient resumeParserServiceClient;
    @Mock private AiInterviewEventProducer eventProducer;

    @InjectMocks
    private ShortlistingServiceImpl shortlistingService;

    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(shortlistingService, "autoShortlistMinScore", 70);
        ReflectionTestUtils.setField(shortlistingService, "autoRejectMaxScore", 59);
        applicationId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        lenient().when(candidateShortlistRepository.save(any(CandidateShortlist.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void handleResumeAnalyzed_shouldShortlist_whenScoreAtOrAboveThreshold() {
        when(resumeParserServiceClient.getResumeMatch(applicationId)).thenReturn(new FeignApiResponse<>(true, "OK", match(75)));
        when(candidateShortlistRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        shortlistingService.handleResumeAnalyzed(event(75));

        verify(candidateShortlistRepository).save(argThat(s -> s.getDecision() == ShortlistDecision.SHORTLISTED
                && s.getOverallMatchScore() == 75));
        verify(eventProducer).publishCandidateShortlisted(argThat(e -> e.getDecision() == ShortlistDecision.SHORTLISTED));
    }

    @Test
    void handleResumeAnalyzed_shouldReject_whenScoreAtOrBelowRejectThreshold() {
        when(resumeParserServiceClient.getResumeMatch(applicationId)).thenReturn(new FeignApiResponse<>(true, "OK", match(40)));
        when(candidateShortlistRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        shortlistingService.handleResumeAnalyzed(event(40));

        verify(candidateShortlistRepository).save(argThat(s -> s.getDecision() == ShortlistDecision.REJECTED));
    }

    @Test
    void handleResumeAnalyzed_shouldParkAsManualReview_whenScoreInGrayZone() {
        when(resumeParserServiceClient.getResumeMatch(applicationId)).thenReturn(new FeignApiResponse<>(true, "OK", match(65)));
        when(candidateShortlistRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        shortlistingService.handleResumeAnalyzed(event(65));

        verify(candidateShortlistRepository).save(argThat(s -> s.getDecision() == ShortlistDecision.MANUAL_REVIEW));
    }

    @Test
    void handleResumeAnalyzed_shouldSkip_whenNoMatchFoundUpstream() {
        when(resumeParserServiceClient.getResumeMatch(applicationId)).thenReturn(new FeignApiResponse<>(true, "OK", null));

        shortlistingService.handleResumeAnalyzed(event(80));

        verify(candidateShortlistRepository, never()).save(any());
        verifyNoInteractions(eventProducer);
    }

    @Test
    void handleResumeAnalyzed_shouldUpdateExistingRowInPlace_onReAnalysis() {
        CandidateShortlist existing = CandidateShortlist.builder()
                .applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .decision(ShortlistDecision.MANUAL_REVIEW).overallMatchScore(65).build();
        when(resumeParserServiceClient.getResumeMatch(applicationId)).thenReturn(new FeignApiResponse<>(true, "OK", match(80)));
        when(candidateShortlistRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(existing));

        shortlistingService.handleResumeAnalyzed(event(80));

        assertThat(existing.getDecision()).isEqualTo(ShortlistDecision.SHORTLISTED);
        assertThat(existing.getOverallMatchScore()).isEqualTo(80);
        verify(candidateShortlistRepository).save(existing);
    }

    @Test
    void shortlistOne_shouldThrow_whenNoShortlistRecordExists() {
        when(candidateShortlistRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> shortlistingService.shortlistOne(applicationId))
                .isInstanceOf(com.cadence.aiinterviewservice.exception.ResourceNotFoundException.class);
    }

    @Test
    void bulkShortlist_shouldUpdateEveryMatchingRowAndPublishOnePerRow() {
        CandidateShortlist s1 = CandidateShortlist.builder().applicationId(UUID.randomUUID()).jobId(jobId).candidateId(candidateId)
                .decision(ShortlistDecision.MANUAL_REVIEW).overallMatchScore(65).build();
        CandidateShortlist s2 = CandidateShortlist.builder().applicationId(UUID.randomUUID()).jobId(jobId).candidateId(candidateId)
                .decision(ShortlistDecision.MANUAL_REVIEW).overallMatchScore(68).build();
        when(candidateShortlistRepository.findAllByApplicationIdIn(any())).thenReturn(List.of(s1, s2));

        shortlistingService.bulkShortlist(List.of(s1.getApplicationId(), s2.getApplicationId()));

        assertThat(s1.getDecision()).isEqualTo(ShortlistDecision.SHORTLISTED);
        assertThat(s2.getDecision()).isEqualTo(ShortlistDecision.SHORTLISTED);
        verify(candidateShortlistRepository).saveAll(List.of(s1, s2));
        verify(eventProducer, times(2)).publishCandidateShortlisted(any());
    }

    @Test
    void assignRecruiter_shouldSetRecruiterOnEveryMatchingRow() {
        UUID recruiterId = UUID.randomUUID();
        CandidateShortlist s1 = CandidateShortlist.builder().applicationId(UUID.randomUUID()).jobId(jobId).candidateId(candidateId)
                .decision(ShortlistDecision.MANUAL_REVIEW).build();
        when(candidateShortlistRepository.findAllByApplicationIdIn(any())).thenReturn(List.of(s1));

        shortlistingService.assignRecruiter(List.of(s1.getApplicationId()), recruiterId);

        assertThat(s1.getAssignedRecruiterId()).isEqualTo(recruiterId);
        verify(candidateShortlistRepository).saveAll(List.of(s1));
    }

    private ResumeMatchDto match(int score) {
        ResumeMatchDto dto = new ResumeMatchDto();
        dto.setId(UUID.randomUUID());
        dto.setApplicationId(applicationId);
        dto.setJobId(jobId);
        dto.setCandidateId(candidateId);
        dto.setFullName("Jane Doe");
        dto.setOverallMatchScore(score);
        dto.setMatchedSkills(List.of(new MatchedSkillDto("Java", "PROGRAMMING_LANGUAGE")));
        dto.setMissingSkills(List.of(new MissingSkillDto("Kubernetes", "DEVOPS", true)));
        return dto;
    }

    private ResumeAnalyzedEvent event(int score) {
        return ResumeAnalyzedEvent.builder()
                .applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .resumeMatchId(UUID.randomUUID()).overallMatchScore(score).occurredAt(LocalDateTime.now()).build();
    }
}
