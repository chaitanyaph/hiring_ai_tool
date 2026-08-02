package com.cadence.candidateservice.kafka.consumer;

import com.cadence.candidateservice.constant.KafkaTopics;
import com.cadence.candidateservice.entity.Candidate;
import com.cadence.candidateservice.kafka.event.UserRegisteredEvent;
import com.cadence.candidateservice.repository.CandidateRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Pre-seeds the candidate row with the fullName/email given at registration --
 * without this, the profile page shows a blank Full Name until the candidate
 * re-types it into the profile wizard's Basic Info step, since that step is
 * otherwise the only place a Candidate entity is ever created.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AuthEventConsumer {

    private final CandidateRepository candidateRepository;

    @KafkaListener(topics = KafkaTopics.USER_REGISTERED, groupId = "candidate-service-group")
    @Transactional
    public void onUserRegistered(UserRegisteredEvent event) {
        try {
            if (!"CANDIDATE".equals(event.getUserType())) {
                return;
            }
            if (candidateRepository.existsById(event.getUserId())) {
                return;
            }
            Candidate candidate = Candidate.builder()
                    .id(event.getUserId())
                    .fullName(event.getFullName())
                    .email(event.getEmail())
                    .build();
            candidateRepository.save(candidate);
        } catch (Exception e) {
            log.error("Failed to pre-seed candidate profile for user {}: {}", event.getUserId(), e.getMessage(), e);
        }
    }
}
