package com.cadence.notificationservice.service;

import com.cadence.notificationservice.dto.request.CreateTemplateRequest;
import com.cadence.notificationservice.dto.request.UpdateTemplateRequest;
import com.cadence.notificationservice.dto.response.TemplatePreviewResponse;
import com.cadence.notificationservice.dto.response.TemplateResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationTemplateService {

    TemplateResponse createTemplate(CreateTemplateRequest request);

    TemplateResponse updateTemplate(UUID id, UpdateTemplateRequest request);

    void deleteTemplate(UUID id);

    TemplateResponse getTemplate(UUID id);

    List<TemplateResponse> listTemplates();

    TemplatePreviewResponse previewTemplate(UUID id);
}
