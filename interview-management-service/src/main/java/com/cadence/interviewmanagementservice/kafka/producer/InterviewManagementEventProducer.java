package com.cadence.interviewmanagementservice.kafka.producer;

import com.cadence.interviewmanagementservice.constants.KafkaTopics;
import com.cadence.interviewmanagementservice.kafka.event.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.SendResult;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;

/**
 * Every publish method is @Async: KafkaTemplate.send() blocks the
 * calling thread while resolving broker metadata *before* returning a
 * future, so a Kafka outage would otherwise stall the scheduling/
 * feedback/decision flows. Each entity row is always the source of
 * truth -- Kafka is best-effort fan-out on top of it.
 *
 * publishApplicationInterviewCompleted is the one bridge onto
 * application-service's own already-wired interview.interview.completed
 * topic -- every other publish*() method is forward-scaffolded (no
 * consumer exists anywhere yet), same posture coding-assessment-
 * service took for its own unconsumed topics.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewManagementEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishInterviewScheduled(InterviewScheduledEvent event) {
        publish(KafkaTopics.INTERVIEW_SCHEDULED, event.getInterviewId().toString(), event);
    }

    @Async
    public void publishInterviewRescheduled(InterviewRescheduledEvent event) {
        publish(KafkaTopics.INTERVIEW_RESCHEDULED, event.getInterviewId().toString(), event);
    }

    @Async
    public void publishInterviewCancelled(InterviewCancelledEvent event) {
        publish(KafkaTopics.INTERVIEW_CANCELLED, event.getInterviewId().toString(), event);
    }

    @Async
    public void publishInterviewCompleted(InterviewCompletedEvent event) {
        publish(KafkaTopics.INTERVIEW_COMPLETED, event.getInterviewId().toString(), event);
    }

    @Async
    public void publishCandidateMovedToHr(CandidateMovedToHrEvent event) {
        publish(KafkaTopics.CANDIDATE_MOVED_TO_HR, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishCandidateRejected(CandidateRejectedEvent event) {
        publish(KafkaTopics.CANDIDATE_REJECTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishCandidateSelected(CandidateSelectedEvent event) {
        publish(KafkaTopics.CANDIDATE_SELECTED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishApplicationInterviewCompleted(ApplicationInterviewCompletedEvent event) {
        publish(KafkaTopics.APPLICATION_INTERVIEW_COMPLETED, event.getApplicationId().toString(), event);
    }

    private void publish(String topic, String key, Object payload) {
        CompletableFuture<SendResult<String, Object>> future = kafkaTemplate.send(topic, key, payload);
        future.whenComplete((result, ex) -> {
            if (ex != null) {
                log.error("Failed to publish event to topic [{}] key [{}]: {}", topic, key, ex.getMessage(), ex);
            } else {
                log.debug("Published event to topic [{}] partition [{}] offset [{}]",
                        topic, result.getRecordMetadata().partition(), result.getRecordMetadata().offset());
            }
        });
    }
}
