package com.cadence.analyticsservice.kafka.consumer;

import com.cadence.analyticsservice.constants.KafkaTopics;
import com.cadence.analyticsservice.kafka.event.CompanyCreatedEvent;
import com.cadence.analyticsservice.service.MetricIngestionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/** A malformed event never crashes the consumer thread or blocks the partition -- same defensive posture as every sibling service. */
@Slf4j
@Component
@RequiredArgsConstructor
public class CompanyEventConsumer {

    private final MetricIngestionService metricIngestionService;

    @KafkaListener(topics = KafkaTopics.COMPANY_CREATED, groupId = "analytics-service-group")
    public void onCompanyCreated(CompanyCreatedEvent event) {
        try {
            metricIngestionService.onCompanyCreated(event);
        } catch (Exception e) {
            log.error("Failed to process CompanyCreated for company {}: {}", event.getCompanyId(), e.getMessage(), e);
        }
    }
}
