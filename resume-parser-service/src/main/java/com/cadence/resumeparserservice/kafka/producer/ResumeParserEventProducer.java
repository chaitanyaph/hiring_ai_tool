package com.cadence.resumeparserservice.kafka.producer;

import com.cadence.resumeparserservice.constants.KafkaTopics;
import com.cadence.resumeparserservice.kafka.event.ResumeAnalysisFailedEvent;
import com.cadence.resumeparserservice.kafka.event.ResumeAnalyzedEvent;
import com.cadence.resumeparserservice.kafka.event.ResumeParsedEvent;
import com.cadence.resumeparserservice.kafka.event.ResumeParsingFailedEvent;
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
 * future, so a Kafka outage would otherwise stall the parsing
 * pipeline. The parsed_resume row is always the source of truth --
 * Kafka is best-effort fan-out on top of it.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeParserEventProducer {

    private final KafkaTemplate<String, Object> kafkaTemplate;

    @Async
    public void publishResumeParsed(ResumeParsedEvent event) {
        publish(KafkaTopics.RESUME_PARSED, event.getResumeId().toString(), event);
    }

    @Async
    public void publishResumeParsingFailed(ResumeParsingFailedEvent event) {
        publish(KafkaTopics.RESUME_PARSING_FAILED, event.getResumeId().toString(), event);
    }

    @Async
    public void publishResumeAnalyzed(ResumeAnalyzedEvent event) {
        publish(KafkaTopics.RESUME_ANALYZED, event.getApplicationId().toString(), event);
    }

    @Async
    public void publishResumeAnalysisFailed(ResumeAnalysisFailedEvent event) {
        publish(KafkaTopics.RESUME_ANALYSIS_FAILED, event.getApplicationId().toString(), event);
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
