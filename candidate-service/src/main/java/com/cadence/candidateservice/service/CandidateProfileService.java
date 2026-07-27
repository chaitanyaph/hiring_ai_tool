package com.cadence.candidateservice.service;

import com.cadence.candidateservice.dto.request.*;
import com.cadence.candidateservice.dto.response.CandidateProfileResponse;
import com.cadence.candidateservice.dto.response.CandidateSummaryResponse;
import com.cadence.candidateservice.dto.response.ResumeUploadResponse;
import com.cadence.candidateservice.security.CurrentUser;
import org.springframework.web.multipart.MultipartFile;

import java.util.UUID;

public interface CandidateProfileService {

    CandidateProfileResponse getProfile(CurrentUser user);

    /** Service-to-service lookup by id -- used by Application Service to validate apply() eligibility. */
    CandidateSummaryResponse getSummary(UUID candidateId);

    /** Also creates the Candidate row on first call -- Step 1 of the wizard doubles as profile creation. */
    CandidateProfileResponse updateBasicInfo(CurrentUser user, BasicInfoRequest request);

    ResumeUploadResponse uploadResume(CurrentUser user, MultipartFile file);

    CandidateProfileResponse updateEducation(CurrentUser user, UpdateEducationRequest request);

    CandidateProfileResponse updateExperience(CurrentUser user, UpdateExperienceRequest request);

    CandidateProfileResponse updateSkills(CurrentUser user, UpdateSkillsRequest request);

    CandidateProfileResponse updateProjects(CurrentUser user, UpdateProjectsRequest request);

    CandidateProfileResponse updateCertifications(CurrentUser user, UpdateCertificationsRequest request);

    CandidateProfileResponse updateLanguages(CurrentUser user, UpdateLanguagesRequest request);

    CandidateProfileResponse updateJobPreferences(CurrentUser user, JobPreferencesRequest request);

    CandidateProfileResponse updatePortfolio(CurrentUser user, PortfolioRequest request);
}
