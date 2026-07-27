package com.cadence.resumeservice.service;

import com.cadence.resumeservice.dto.request.RenameResumeRequest;
import com.cadence.resumeservice.dto.response.ResumeObjectDetailsResponse;
import com.cadence.resumeservice.dto.response.ResumeResponse;
import com.cadence.resumeservice.security.CurrentUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

public interface ResumeService {

    // ---- Candidate self-service ----
    ResumeResponse upload(CurrentUser candidate, MultipartFile file, String displayName);
    List<ResumeResponse> listMyResumes(CurrentUser candidate);
    ResumeResponse getDetail(CurrentUser candidate, UUID resumeId);
    ResumeResponse setDefault(CurrentUser candidate, UUID resumeId);
    ResumeResponse rename(CurrentUser candidate, UUID resumeId, RenameResumeRequest request);
    void delete(CurrentUser candidate, UUID resumeId);
    ResumeContent downloadForCandidate(CurrentUser candidate, UUID resumeId);
    ResumeContent previewForCandidate(CurrentUser candidate, UUID resumeId);

    // ---- Recruiter (preview/download only, company-scoped unless ADMIN) ----
    ResumeContent downloadForRecruiter(CurrentUser recruiter, UUID resumeId);
    ResumeContent previewForRecruiter(CurrentUser recruiter, UUID resumeId);

    // ---- Internal / trusted network ----
    ResumeResponse getMetadataInternal(UUID resumeId);
    ResumeObjectDetailsResponse getObjectDetailsInternal(UUID resumeId);

    // ---- Kafka-driven ----
    void handleCandidateDeleted(UUID candidateId);
}
