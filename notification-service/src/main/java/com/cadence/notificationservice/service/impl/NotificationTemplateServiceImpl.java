package com.cadence.notificationservice.service.impl;

import com.cadence.notificationservice.dto.request.CreateTemplateRequest;
import com.cadence.notificationservice.dto.request.UpdateTemplateRequest;
import com.cadence.notificationservice.dto.response.TemplatePreviewResponse;
import com.cadence.notificationservice.dto.response.TemplateResponse;
import com.cadence.notificationservice.email.TemplateRenderer;
import com.cadence.notificationservice.entity.NotificationTemplate;
import com.cadence.notificationservice.exception.ErrorCode;
import com.cadence.notificationservice.exception.ResourceNotFoundException;
import com.cadence.notificationservice.exception.NotificationServiceException;
import com.cadence.notificationservice.mapper.TemplateMapper;
import com.cadence.notificationservice.repository.NotificationTemplateRepository;
import com.cadence.notificationservice.service.NotificationTemplateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationTemplateServiceImpl implements NotificationTemplateService {

    private static final Map<String, String> SAMPLE_VARIABLES = Map.ofEntries(
            Map.entry("candidate_name", "Priya Kulkarni"),
            Map.entry("recipient_name", "Priya Kulkarni"),
            Map.entry("job_title", "Backend Engineer"),
            Map.entry("company_name", "Acme Corp"),
            Map.entry("interview_date", "Jul 21, 2026"),
            Map.entry("interview_time", "3:30 PM"),
            Map.entry("role", "Technical Recruiter"),
            Map.entry("invite_link", "https://cadence-hiring.com/invite/sample"),
            Map.entry("verification_link", "https://cadence-hiring.com/verify/sample"),
            Map.entry("reset_link", "https://cadence-hiring.com/reset/sample"),
            Map.entry("reschedule_reason", " Reason: panel conflict"),
            Map.entry("cancel_reason", " Reason: role put on hold"),
            Map.entry("expiry_date", "Jul 25, 2026"),
            Map.entry("verification_status", "in progress")
    );

    private final NotificationTemplateRepository templateRepository;
    private final TemplateMapper templateMapper;
    private final TemplateRenderer templateRenderer;

    @Override
    @Transactional
    public TemplateResponse createTemplate(CreateTemplateRequest request) {
        if (templateRepository.findByCategory(request.getCategory()).isPresent()) {
            throw new NotificationServiceException(ErrorCode.TEMPLATE_ALREADY_EXISTS,
                    "A template already exists for category " + request.getCategory(), HttpStatus.CONFLICT);
        }
        NotificationTemplate template = NotificationTemplate.builder()
                .name(request.getName())
                .triggerEvent(request.getTriggerEvent())
                .category(request.getCategory())
                .subject(request.getSubject())
                .bodyHtml(request.getBodyHtml())
                .variablesHint(request.getVariablesHint())
                .active(true)
                .build();
        return templateMapper.toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public TemplateResponse updateTemplate(UUID id, UpdateTemplateRequest request) {
        NotificationTemplate template = findTemplate(id);
        template.setName(request.getName());
        template.setSubject(request.getSubject());
        template.setBodyHtml(request.getBodyHtml());
        template.setVariablesHint(request.getVariablesHint());
        template.setActive(request.isActive());
        return templateMapper.toResponse(templateRepository.save(template));
    }

    @Override
    @Transactional
    public void deleteTemplate(UUID id) {
        templateRepository.delete(findTemplate(id));
    }

    @Override
    public TemplateResponse getTemplate(UUID id) {
        return templateMapper.toResponse(findTemplate(id));
    }

    @Override
    public List<TemplateResponse> listTemplates() {
        return templateRepository.findAll().stream().map(templateMapper::toResponse).toList();
    }

    @Override
    public TemplatePreviewResponse previewTemplate(UUID id) {
        NotificationTemplate template = findTemplate(id);
        return TemplatePreviewResponse.builder()
                .subject(templateRenderer.render(template.getSubject(), SAMPLE_VARIABLES))
                .bodyHtml(templateRenderer.render(template.getBodyHtml(), SAMPLE_VARIABLES))
                .sampleRecipient(SAMPLE_VARIABLES.get("candidate_name"))
                .build();
    }

    private NotificationTemplate findTemplate(UUID id) {
        return templateRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException(ErrorCode.TEMPLATE_NOT_FOUND, "Template not found: " + id));
    }
}
