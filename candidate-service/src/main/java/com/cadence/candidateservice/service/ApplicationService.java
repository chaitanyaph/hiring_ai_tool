package com.cadence.candidateservice.service;

import com.cadence.candidateservice.dto.request.ApplyToJobRequest;
import com.cadence.candidateservice.dto.request.ChangeApplicationStageRequest;
import com.cadence.candidateservice.dto.response.ApplicationResponse;
import com.cadence.candidateservice.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {

    ApplicationResponse apply(CurrentUser candidate, ApplyToJobRequest request);

    List<ApplicationResponse> listMyApplications(CurrentUser candidate, String filter);

    ApplicationResponse getApplicationDetail(CurrentUser candidate, UUID applicationId);

    ApplicationResponse withdraw(CurrentUser candidate, UUID applicationId);

    /** Called by recruiting-side roles (not candidates) to advance an application's pipeline stage. */
    ApplicationResponse changeStage(CurrentUser recruiter, UUID applicationId, ChangeApplicationStageRequest request);
}
