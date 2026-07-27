package com.cadence.applicationservice.service;

import com.cadence.applicationservice.dto.request.*;
import com.cadence.applicationservice.dto.response.*;
import com.cadence.applicationservice.security.CurrentUser;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ApplicationService {

    // ---- Candidate-facing ----
    ApplicationResponse apply(CurrentUser candidate, ApplyRequest request);
    ApplicationResponse getForCandidate(CurrentUser candidate, UUID applicationId);
    List<ApplicationResponse> listMyApplications(CurrentUser candidate);
    ApplicationResponse withdraw(CurrentUser candidate, UUID applicationId);
    ApplicationResponse acceptOffer(CurrentUser candidate, UUID applicationId);
    ApplicationResponse rejectOffer(CurrentUser candidate, UUID applicationId);

    // ---- Recruiter-facing ----
    ApplicationResponse getForRecruiter(CurrentUser recruiter, UUID applicationId);
    PagedResponse<ApplicationResponse> searchCompanyApplications(CurrentUser recruiter, ApplicationSearchCriteria criteria, Pageable pageable);
    PagedResponse<ApplicationResponse> getJobApplications(CurrentUser recruiter, UUID jobId, Pageable pageable);
    ApplicationResponse changeStatus(CurrentUser recruiter, UUID applicationId, StatusChangeRequest request);
    ApplicationResponse assignRecruiter(CurrentUser recruiter, UUID applicationId, AssignRecruiterRequest request);
    ApplicationResponse assignHiringManager(CurrentUser recruiter, UUID applicationId, AssignHiringManagerRequest request);
    NoteResponse addNote(CurrentUser recruiter, UUID applicationId, AddNoteRequest request);

    // ---- Shared: accessible to the owning candidate OR a recruiter from the same company ----
    List<TimelineEntryResponse> getTimeline(CurrentUser caller, UUID applicationId);
    ApplicationHistoryResponse getHistory(CurrentUser caller, UUID applicationId);

    // ---- Internal: trusted-network queries for other services ----
    /** Used by Resume Service to block deleting a resume still attached to a non-terminal application. */
    boolean isResumeInUse(UUID resumeId);

    /** Used by Resume Service to scope a recruiter's resume preview/download to candidates who applied to their own company. */
    boolean hasApplicationFromCandidateToCompany(UUID candidateId, UUID companyId);

    /** Used by Resume Parser Service to build a per-job candidate ranking for resume matching. */
    List<ApplicationResponse> getApplicationsByJobInternal(UUID jobId);
}
