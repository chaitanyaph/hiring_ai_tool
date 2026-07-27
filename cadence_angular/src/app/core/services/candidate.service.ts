import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  BasicInfoRequest,
  CandidateProfileResponse,
  DashboardResponse,
  JobPreferencesRequest,
  PortfolioRequest,
  ResumeUploadResponse,
  SavedJobResponse,
  UpdateCertificationsRequest,
  UpdateEducationRequest,
  UpdateExperienceRequest,
  UpdateLanguagesRequest,
  UpdateProjectsRequest,
  UpdateSkillsRequest,
} from '../models/candidate.model';

/**
 * Gateway prefix "/candidate" chosen to mirror /auth and /company (Eureka app
 * name "candidate-service" minus the -service suffix) -- api-gateway-service's
 * route config (task #212) isn't built yet, so this prefix is a naming
 * convention carried from the earlier modules, not something verified against
 * a live route table.
 */
const BASE_URL = `${environment.apiBaseUrl}/candidate/api/v1`;

@Injectable({ providedIn: 'root' })
export class CandidateService {
  private http = inject(HttpClient);

  // ---- Profile wizard (10 steps) + read-only profile ----

  getProfile(): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.get<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me`);
  }

  updateBasicInfo(request: BasicInfoRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/basic-info`, request);
  }

  uploadResume(file: File): Observable<ApiResponse<ResumeUploadResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    return this.http.post<ApiResponse<ResumeUploadResponse>>(`${BASE_URL}/candidates/me/resume`, formData);
  }

  updateEducation(request: UpdateEducationRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/education`, request);
  }

  updateExperience(request: UpdateExperienceRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/experience`, request);
  }

  updateSkills(request: UpdateSkillsRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/skills`, request);
  }

  updateProjects(request: UpdateProjectsRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/projects`, request);
  }

  updateCertifications(request: UpdateCertificationsRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/certifications`, request);
  }

  updateLanguages(request: UpdateLanguagesRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/languages`, request);
  }

  updateJobPreferences(request: JobPreferencesRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/job-preferences`, request);
  }

  updatePortfolio(request: PortfolioRequest): Observable<ApiResponse<CandidateProfileResponse>> {
    return this.http.put<ApiResponse<CandidateProfileResponse>>(`${BASE_URL}/candidates/me/portfolio`, request);
  }

  // ---- Dashboard ----

  getDashboard(): Observable<ApiResponse<DashboardResponse>> {
    return this.http.get<ApiResponse<DashboardResponse>>(`${BASE_URL}/dashboard`);
  }

  // ---- Saved jobs ----

  listSavedJobs(): Observable<ApiResponse<SavedJobResponse[]>> {
    return this.http.get<ApiResponse<SavedJobResponse[]>>(`${BASE_URL}/saved-jobs`);
  }

  saveJob(jobId: string): Observable<ApiResponse<SavedJobResponse>> {
    return this.http.post<ApiResponse<SavedJobResponse>>(`${BASE_URL}/saved-jobs`, { jobId });
  }

  unsaveJob(jobId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/saved-jobs/${jobId}`);
  }
}
