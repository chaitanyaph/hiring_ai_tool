package com.cadence.candidateservice.service;

import com.cadence.candidateservice.dto.request.BasicInfoRequest;
import com.cadence.candidateservice.dto.request.UpdateEducationRequest;
import com.cadence.candidateservice.dto.request.UpdateSkillsRequest;
import com.cadence.candidateservice.dto.response.CandidateProfileResponse;
import com.cadence.candidateservice.entity.Candidate;
import com.cadence.candidateservice.entity.CandidateSkill;
import com.cadence.candidateservice.exception.ResourceNotFoundException;
import com.cadence.candidateservice.kafka.producer.CandidateEventProducer;
import com.cadence.candidateservice.mapper.CandidateMapper;
import com.cadence.candidateservice.repository.*;
import com.cadence.candidateservice.security.CurrentUser;
import com.cadence.candidateservice.service.impl.CandidateProfileServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.cache.CacheManager;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CandidateProfileServiceImplTest {

    @Mock private CandidateRepository candidateRepository;
    @Mock private CandidateEducationRepository educationRepository;
    @Mock private CandidateExperienceRepository experienceRepository;
    @Mock private CandidateSkillRepository skillRepository;
    @Mock private CandidateProjectRepository projectRepository;
    @Mock private CandidateCertificationRepository certificationRepository;
    @Mock private CandidateLanguageRepository languageRepository;
    @Mock private CandidateJobPreferenceRepository jobPreferenceRepository;
    @Mock private CandidatePortfolioLinkRepository portfolioLinkRepository;
    @Mock private CandidateMapper candidateMapper;
    @Mock private CandidateEventProducer eventProducer;
    @Mock private CacheManager cacheManager;

    @InjectMocks
    private CandidateProfileServiceImpl candidateProfileService;

    private CurrentUser candidateUser;

    @BeforeEach
    void setUp() {
        candidateUser = CurrentUser.builder()
                .userId(UUID.randomUUID())
                .email("rahul.mehta@email.com")
                .role("CANDIDATE")
                .build();
    }

    @Test
    void updateBasicInfo_shouldCreateCandidate_whenFirstCall() {
        when(candidateRepository.existsById(candidateUser.getUserId())).thenReturn(false);
        when(candidateRepository.findById(candidateUser.getUserId())).thenReturn(Optional.empty());
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));
        when(educationRepository.findAllByCandidateIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(experienceRepository.findAllByCandidateIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(skillRepository.findAllByCandidateId(any())).thenReturn(List.of());
        when(projectRepository.findAllByCandidateIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(certificationRepository.findAllByCandidateIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(languageRepository.findAllByCandidateId(any())).thenReturn(List.of());
        when(jobPreferenceRepository.findByCandidateId(any())).thenReturn(Optional.empty());
        when(portfolioLinkRepository.findByCandidateId(any())).thenReturn(Optional.empty());
        when(candidateMapper.toEducationResponseList(any())).thenReturn(List.of());
        when(candidateMapper.toExperienceResponseList(any())).thenReturn(List.of());
        when(candidateMapper.toProjectResponseList(any())).thenReturn(List.of());
        when(candidateMapper.toCertificationResponseList(any())).thenReturn(List.of());

        BasicInfoRequest request = BasicInfoRequest.builder().fullName("Rahul Mehta").headline("Backend Engineer").build();
        CandidateProfileResponse response = candidateProfileService.updateBasicInfo(candidateUser, request);

        assertThat(response.getFullName()).isEqualTo("Rahul Mehta");
        assertThat(response.getEmail()).isEqualTo("rahul.mehta@email.com");
        assertThat(response.getProfileCompletionPercent()).isEqualTo(10); // 1 of 10 sections

        ArgumentCaptor<Candidate> captor = ArgumentCaptor.forClass(Candidate.class);
        verify(candidateRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(candidateUser.getUserId());
        verify(eventProducer).publishProfileCreated(any());
        verify(eventProducer, never()).publishProfileUpdated(any());
    }

    @Test
    void updateBasicInfo_shouldUpdateExisting_shouldNotRefireCreatedEvent() {
        Candidate existing = Candidate.builder().id(candidateUser.getUserId()).email(candidateUser.getEmail())
                .fullName("Old Name").profileCompletionPercent(10).build();
        when(candidateRepository.existsById(candidateUser.getUserId())).thenReturn(true);
        when(candidateRepository.findById(candidateUser.getUserId())).thenReturn(Optional.of(existing));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildCollections();

        BasicInfoRequest request = BasicInfoRequest.builder().fullName("Rahul Mehta").build();
        CandidateProfileResponse response = candidateProfileService.updateBasicInfo(candidateUser, request);

        assertThat(response.getFullName()).isEqualTo("Rahul Mehta");
        verify(eventProducer, never()).publishProfileCreated(any());
        verify(eventProducer).publishProfileUpdated(any());
    }

    @Test
    void updateEducation_shouldThrow_whenCandidateDoesNotExist() {
        when(candidateRepository.findById(candidateUser.getUserId())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> candidateProfileService.updateEducation(candidateUser, UpdateEducationRequest.builder().items(List.of()).build()))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updateSkills_shouldReplaceAllExistingSkills() {
        Candidate existing = Candidate.builder().id(candidateUser.getUserId()).email(candidateUser.getEmail())
                .fullName("Rahul Mehta").profileCompletionPercent(10).build();
        when(candidateRepository.findById(candidateUser.getUserId())).thenReturn(Optional.of(existing));
        when(candidateRepository.save(any(Candidate.class))).thenAnswer(inv -> inv.getArgument(0));
        stubEmptyChildCollections();
        when(skillRepository.countByCandidateId(any())).thenReturn(2L);
        when(candidateMapper.toEducationResponseList(any())).thenReturn(List.of());
        when(candidateMapper.toExperienceResponseList(any())).thenReturn(List.of());
        when(candidateMapper.toProjectResponseList(any())).thenReturn(List.of());
        when(candidateMapper.toCertificationResponseList(any())).thenReturn(List.of());

        candidateProfileService.updateSkills(candidateUser, UpdateSkillsRequest.builder().skills(List.of("Java", "Kafka")).build());

        verify(skillRepository).deleteAllByCandidateId(candidateUser.getUserId());
        ArgumentCaptor<List<CandidateSkill>> captor = ArgumentCaptor.forClass(List.class);
        verify(skillRepository).saveAll(captor.capture());
        assertThat(captor.getValue()).extracting(CandidateSkill::getSkillName).containsExactly("Java", "Kafka");
    }

    private void stubEmptyChildCollections() {
        when(educationRepository.findAllByCandidateIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(experienceRepository.findAllByCandidateIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(skillRepository.findAllByCandidateId(any())).thenReturn(List.of());
        when(projectRepository.findAllByCandidateIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(certificationRepository.findAllByCandidateIdOrderByDisplayOrderAsc(any())).thenReturn(List.of());
        when(languageRepository.findAllByCandidateId(any())).thenReturn(List.of());
        when(jobPreferenceRepository.findByCandidateId(any())).thenReturn(Optional.empty());
        when(portfolioLinkRepository.findByCandidateId(any())).thenReturn(Optional.empty());
        lenient().when(candidateMapper.toEducationResponseList(any())).thenReturn(List.of());
        lenient().when(candidateMapper.toExperienceResponseList(any())).thenReturn(List.of());
        lenient().when(candidateMapper.toProjectResponseList(any())).thenReturn(List.of());
        lenient().when(candidateMapper.toCertificationResponseList(any())).thenReturn(List.of());
    }
}
