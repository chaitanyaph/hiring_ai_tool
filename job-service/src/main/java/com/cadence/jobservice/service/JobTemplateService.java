package com.cadence.jobservice.service;

import com.cadence.jobservice.dto.request.SaveTemplateRequest;
import com.cadence.jobservice.dto.response.JobDetailResponse;
import com.cadence.jobservice.dto.response.JobTemplateResponse;
import com.cadence.jobservice.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface JobTemplateService {
    JobTemplateResponse saveAsTemplate(UUID jobId, SaveTemplateRequest request, CurrentUser currentUser);
    List<JobTemplateResponse> listTemplates(CurrentUser currentUser);
    JobDetailResponse createDraftFromTemplate(UUID templateId, CurrentUser currentUser);
    void deleteTemplate(UUID templateId, CurrentUser currentUser);
}
