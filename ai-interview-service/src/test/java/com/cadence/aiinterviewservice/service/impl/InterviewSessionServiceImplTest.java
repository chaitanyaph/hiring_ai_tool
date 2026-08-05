package com.cadence.aiinterviewservice.service.impl;

import com.cadence.aiinterviewservice.constants.HiringRecommendation;
import com.cadence.aiinterviewservice.constants.InterviewMode;
import com.cadence.aiinterviewservice.constants.InterviewSessionStatus;
import com.cadence.aiinterviewservice.dto.request.AnswerRequest;
import com.cadence.aiinterviewservice.dto.response.InterviewQuestionResponse;
import com.cadence.aiinterviewservice.constants.ShortlistDecision;
import com.cadence.aiinterviewservice.entity.CandidateShortlist;
import com.cadence.aiinterviewservice.entity.InterviewAnswer;
import com.cadence.aiinterviewservice.entity.InterviewQuestion;
import com.cadence.aiinterviewservice.entity.InterviewRecommendation;
import com.cadence.aiinterviewservice.entity.InterviewSession;
import com.cadence.aiinterviewservice.exception.InterviewConflictException;
import com.cadence.aiinterviewservice.exception.ResourceNotFoundException;
import com.cadence.aiinterviewservice.feign.JobServiceClient;
import com.cadence.aiinterviewservice.feign.ResumeParserServiceClient;
import com.cadence.aiinterviewservice.feign.dto.FeignApiResponse;
import com.cadence.aiinterviewservice.feign.dto.JobDetailDto;
import com.cadence.aiinterviewservice.feign.dto.ResumeMatchDto;
import com.cadence.aiinterviewservice.kafka.producer.AiInterviewEventProducer;
import com.cadence.aiinterviewservice.provider.AIInterviewProvider;
import com.cadence.aiinterviewservice.provider.GeneratedQuestion;
import com.cadence.aiinterviewservice.repository.*;
import com.cadence.aiinterviewservice.service.InterviewEvaluationService;
import com.cadence.aiinterviewservice.strategy.AIInterviewProviderFactory;
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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewSessionServiceImplTest {

    @Mock private InterviewSessionRepository interviewSessionRepository;
    @Mock private InterviewQuestionRepository interviewQuestionRepository;
    @Mock private InterviewAnswerRepository interviewAnswerRepository;
    @Mock private InterviewRecommendationRepository interviewRecommendationRepository;
    @Mock private InterviewLogRepository interviewLogRepository;
    @Mock private CandidateShortlistRepository candidateShortlistRepository;
    @Mock private JobServiceClient jobServiceClient;
    @Mock private ResumeParserServiceClient resumeParserServiceClient;
    @Mock private AIInterviewProviderFactory providerFactory;
    @Mock private AiInterviewEventProducer eventProducer;
    @Mock private InterviewEvaluationService interviewEvaluationService;
    @Mock private AIInterviewProvider provider;
    @Mock private com.cadence.aiinterviewservice.provider.TextToSpeechService textToSpeechService;

    @InjectMocks
    private InterviewSessionServiceImpl interviewSessionService;

    private UUID applicationId;
    private UUID jobId;
    private UUID candidateId;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(interviewSessionService, "defaultQuestionCount", 8);
        ReflectionTestUtils.setField(interviewSessionService, "ttlHours", 72);
        ReflectionTestUtils.setField(interviewSessionService, "frontendBaseUrl", "https://hirepilot.duckdns.org");
        applicationId = UUID.randomUUID();
        jobId = UUID.randomUUID();
        candidateId = UUID.randomUUID();
        lenient().when(interviewSessionRepository.save(any(InterviewSession.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(interviewQuestionRepository.save(any(InterviewQuestion.class))).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void inviteCandidate_shouldCreateNotStartedSession_whenNoneExists() {
        CandidateShortlist shortlist = CandidateShortlist.builder()
                .applicationId(applicationId).jobId(jobId).candidateId(candidateId).decision(ShortlistDecision.SHORTLISTED).build();
        when(candidateShortlistRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(shortlist));
        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.empty());

        interviewSessionService.inviteCandidate(applicationId);

        verify(interviewSessionRepository).save(argThat(s -> s.getStatus() == InterviewSessionStatus.NOT_STARTED
                && s.getApplicationId().equals(applicationId) && s.getInvitedAt() != null && s.getExpiresAt() != null));
    }

    @Test
    void startInterview_shouldThrow_whenAlreadyInProgress() {
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.IN_PROGRESS).totalQuestions(8).build();
        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> interviewSessionService.startInterview(applicationId, candidateId, InterviewMode.CHAT))
                .isInstanceOf(InterviewConflictException.class);
        verifyNoInteractions(providerFactory);
    }

    @Test
    void startInterview_shouldMarkExpiredAndThrow_whenPastDeadline() {
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.NOT_STARTED).totalQuestions(8)
                .expiresAt(LocalDateTime.now().minusHours(1)).build();
        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));

        assertThatThrownBy(() -> interviewSessionService.startInterview(applicationId, candidateId, InterviewMode.CHAT))
                .isInstanceOf(InterviewConflictException.class);
        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.EXPIRED);
    }

    @Test
    void startInterview_shouldTransitionAndGenerateFirstQuestion() {
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.NOT_STARTED).totalQuestions(8)
                .expiresAt(LocalDateTime.now().plusHours(1)).build();
        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));
        mockResumeAndJob();
        when(providerFactory.getActiveProvider()).thenReturn(provider);
        when(provider.generateNextQuestion(any())).thenReturn(new GeneratedQuestion("Tell me about yourself."));

        InterviewQuestionResponse response = interviewSessionService.startInterview(applicationId, candidateId, InterviewMode.CHAT);

        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.IN_PROGRESS);
        assertThat(session.getMode()).isEqualTo(InterviewMode.CHAT);
        assertThat(response.getQuestionText()).isEqualTo("Tell me about yourself.");
        assertThat(response.getQuestionOrder()).isEqualTo(1);
        assertThat(response.isInterviewCompleted()).isFalse();
        verify(eventProducer).publishInterviewStarted(any());
    }

    @Test
    void submitAnswer_shouldThrow_whenSessionNotInProgress() {
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.NOT_STARTED).totalQuestions(8).build();
        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));

        AnswerRequest request = AnswerRequest.builder().applicationId(applicationId).questionId(UUID.randomUUID()).answerText("x").build();

        assertThatThrownBy(() -> interviewSessionService.submitAnswer(applicationId, candidateId, request))
                .isInstanceOf(InterviewConflictException.class);
    }

    @Test
    void submitAnswer_shouldThrow_whenQuestionAlreadyAnswered() {
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.IN_PROGRESS).totalQuestions(8).currentQuestionIndex(1).build();
        UUID sessionId = UUID.randomUUID();
        ReflectionTestUtils.setField(session, "id", sessionId);
        InterviewQuestion question = InterviewQuestion.builder().sessionId(sessionId).questionOrder(1).build();
        UUID questionId = UUID.randomUUID();
        ReflectionTestUtils.setField(question, "id", questionId);

        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));
        when(interviewQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(interviewAnswerRepository.findByQuestionId(questionId)).thenReturn(Optional.of(InterviewAnswer.builder().questionId(questionId).build()));

        AnswerRequest request = AnswerRequest.builder().applicationId(applicationId).questionId(questionId).answerText("x").build();

        assertThatThrownBy(() -> interviewSessionService.submitAnswer(applicationId, candidateId, request))
                .isInstanceOf(InterviewConflictException.class);
    }

    @Test
    void submitAnswer_shouldAdvanceAndGenerateNextQuestion_whenMoreQuestionsRemain() {
        UUID sessionId = UUID.randomUUID();
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.IN_PROGRESS).totalQuestions(8).currentQuestionIndex(0).build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        UUID questionId = UUID.randomUUID();
        InterviewQuestion question = InterviewQuestion.builder().sessionId(sessionId).questionOrder(1).build();
        ReflectionTestUtils.setField(question, "id", questionId);

        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));
        when(interviewQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(interviewAnswerRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());
        when(interviewQuestionRepository.findAllBySessionIdOrderByQuestionOrderAsc(sessionId)).thenReturn(List.of(question));
        mockResumeAndJob();
        when(providerFactory.getActiveProvider()).thenReturn(provider);
        when(provider.generateNextQuestion(any())).thenReturn(new GeneratedQuestion("Next question."));

        AnswerRequest request = AnswerRequest.builder().applicationId(applicationId).questionId(questionId).answerText("My answer").build();
        InterviewQuestionResponse response = interviewSessionService.submitAnswer(applicationId, candidateId, request);

        assertThat(session.getCurrentQuestionIndex()).isEqualTo(1);
        assertThat(response.isInterviewCompleted()).isFalse();
        assertThat(response.getQuestionText()).isEqualTo("Next question.");
        verify(interviewAnswerRepository).save(any(InterviewAnswer.class));
        verifyNoInteractions(interviewEvaluationService);
    }

    @Test
    void submitAnswer_shouldCompleteSessionAndTriggerEvaluation_whenLastQuestionAnswered() {
        UUID sessionId = UUID.randomUUID();
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.IN_PROGRESS).totalQuestions(1).currentQuestionIndex(0).build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        UUID questionId = UUID.randomUUID();
        InterviewQuestion question = InterviewQuestion.builder().sessionId(sessionId).questionOrder(1).build();
        ReflectionTestUtils.setField(question, "id", questionId);

        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));
        when(interviewQuestionRepository.findById(questionId)).thenReturn(Optional.of(question));
        when(interviewAnswerRepository.findByQuestionId(questionId)).thenReturn(Optional.empty());

        AnswerRequest request = AnswerRequest.builder().applicationId(applicationId).questionId(questionId).answerText("Final answer").build();
        InterviewQuestionResponse response = interviewSessionService.submitAnswer(applicationId, candidateId, request);

        assertThat(session.getStatus()).isEqualTo(InterviewSessionStatus.COMPLETED);
        assertThat(response.isInterviewCompleted()).isTrue();
        verify(eventProducer).publishInterviewCompleted(any());
        verify(interviewEvaluationService).evaluate(sessionId);
        verifyNoInteractions(providerFactory);
    }

    @Test
    void recordRecruiterDecision_shouldUpdateRecommendationAndPublish() {
        UUID sessionId = UUID.randomUUID();
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.COMPLETED).totalQuestions(8).build();
        ReflectionTestUtils.setField(session, "id", sessionId);
        InterviewRecommendation recommendation = InterviewRecommendation.builder().sessionId(sessionId).hiringRecommendation(HiringRecommendation.HOLD).build();

        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));
        when(interviewRecommendationRepository.findBySessionId(sessionId)).thenReturn(Optional.of(recommendation));

        interviewSessionService.recordRecruiterDecision(applicationId, HiringRecommendation.PROCEED);

        assertThat(recommendation.getHiringRecommendation()).isEqualTo(HiringRecommendation.PROCEED);
        verify(interviewRecommendationRepository).save(recommendation);
        verify(eventProducer).publishCandidateRecommended(argThat(e -> e.getHiringRecommendation() == HiringRecommendation.PROCEED));
    }

    @Test
    void recordRecruiterDecision_shouldThrow_whenNoEvaluationExistsYet() {
        UUID sessionId = UUID.randomUUID();
        InterviewSession session = InterviewSession.builder().applicationId(applicationId).jobId(jobId).candidateId(candidateId)
                .status(InterviewSessionStatus.COMPLETED).totalQuestions(8).build();
        ReflectionTestUtils.setField(session, "id", sessionId);

        when(interviewSessionRepository.findByApplicationId(applicationId)).thenReturn(Optional.of(session));
        when(interviewRecommendationRepository.findBySessionId(sessionId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> interviewSessionService.recordRecruiterDecision(applicationId, HiringRecommendation.PROCEED))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    private void mockResumeAndJob() {
        ResumeMatchDto match = new ResumeMatchDto();
        match.setFullName("Jane Doe");
        when(resumeParserServiceClient.getResumeMatch(applicationId)).thenReturn(new FeignApiResponse<>(true, "OK", match));
        JobDetailDto job = new JobDetailDto();
        job.setTitle("Backend Engineer");
        when(jobServiceClient.getJobDetail(jobId)).thenReturn(new FeignApiResponse<>(true, "OK", job));
    }
}
