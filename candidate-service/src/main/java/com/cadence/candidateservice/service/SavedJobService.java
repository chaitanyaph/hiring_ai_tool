package com.cadence.candidateservice.service;

import com.cadence.candidateservice.dto.response.SavedJobResponse;
import com.cadence.candidateservice.security.CurrentUser;

import java.util.List;
import java.util.UUID;

public interface SavedJobService {
    SavedJobResponse saveJob(CurrentUser candidate, UUID jobId);
    void unsaveJob(CurrentUser candidate, UUID jobId);
    List<SavedJobResponse> listSavedJobs(CurrentUser candidate);
}
