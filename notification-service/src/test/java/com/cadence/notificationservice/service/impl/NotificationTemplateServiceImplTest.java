package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.constants.TemplateCategory;
import com.cadence.notificationservice.constants.TriggerEvent;
import com.cadence.notificationservice.dto.request.CreateTemplateRequest;
import com.cadence.notificationservice.email.TemplateRenderer;
import com.cadence.notificationservice.entity.NotificationTemplate;
import com.cadence.notificationservice.exception.NotificationServiceException;
import com.cadence.notificationservice.exception.ResourceNotFoundException;
import com.cadence.notificationservice.mapper.TemplateMapper;
import com.cadence.notificationservice.mapper.TemplateMapperImpl;
import com.cadence.notificationservice.repository.NotificationTemplateRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class NotificationTemplateServiceImplTest {

    @Mock private NotificationTemplateRepository templateRepository;

    private final TemplateMapper templateMapper = new TemplateMapperImpl();

    private NotificationTemplateServiceImpl templateService;

    @BeforeEach
    void setUp() {
        templateService = new NotificationTemplateServiceImpl(templateRepository, templateMapper, new TemplateRenderer());
        lenient().when(templateRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createTemplate_shouldThrow_whenCategoryAlreadyHasATemplate() {
        when(templateRepository.findByCategory(TemplateCategory.OFFER_LETTER))
                .thenReturn(Optional.of(NotificationTemplate.builder().id(UUID.randomUUID()).category(TemplateCategory.OFFER_LETTER)
                        .triggerEvent(TriggerEvent.NONE).name("Offer Letter").subject("s").bodyHtml("b").active(true).build()));

        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .name("Offer Letter v2").triggerEvent(TriggerEvent.NONE).category(TemplateCategory.OFFER_LETTER)
                .subject("s").bodyHtml("b").build();

        assertThatThrownBy(() -> templateService.createTemplate(request)).isInstanceOf(NotificationServiceException.class);
    }

    @Test
    void createTemplate_shouldPersist_whenCategoryIsNew() {
        when(templateRepository.findByCategory(TemplateCategory.BACKGROUND_VERIFICATION)).thenReturn(Optional.empty());

        CreateTemplateRequest request = CreateTemplateRequest.builder()
                .name("BGV Update").triggerEvent(TriggerEvent.NONE).category(TemplateCategory.BACKGROUND_VERIFICATION)
                .subject("Update").bodyHtml("<p>Hi {{candidate_name}}</p>").build();

        var response = templateService.createTemplate(request);

        assertThat(response.getCategory()).isEqualTo(TemplateCategory.BACKGROUND_VERIFICATION);
        assertThat(response.isActive()).isTrue();
    }

    @Test
    void getTemplate_shouldThrow_whenNotFound() {
        UUID id = UUID.randomUUID();
        when(templateRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> templateService.getTemplate(id)).isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void previewTemplate_shouldSubstituteSampleVariables() {
        UUID id = UUID.randomUUID();
        NotificationTemplate template = NotificationTemplate.builder().id(id).category(TemplateCategory.APPLICATION_RECEIVED)
                .triggerEvent(TriggerEvent.APPLICATION_SUBMITTED).name("Application Received")
                .subject("Hi {{candidate_name}}").bodyHtml("<p>Role: {{job_title}}</p>").active(true).build();
        when(templateRepository.findById(id)).thenReturn(Optional.of(template));

        var preview = templateService.previewTemplate(id);

        assertThat(preview.getSubject()).contains("Priya Kulkarni");
        assertThat(preview.getBodyHtml()).contains("Backend Engineer");
    }
}
