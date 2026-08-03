import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  AssignJobRequest,
  CandidateJobBrowseCriteria,
  CandidateJobDetailResponse,
  CandidateJobSummaryResponse,
  JobBasicInfoRequest,
  JobCountsResponse,
  JobDashboardResponse,
  JobDetailResponse,
  JobRequirementsRequest,
  JobSearchCriteria,
  JobSummaryResponse,
  JobTemplateResponse,
  SaveTemplateRequest,
  StatusChangeRequest,
  UpdatePipelineStagesRequest,
} from '../models/job.model';

const BASE_URL = `${environment.apiBaseUrl}/jobs/api/v1`;

@Injectable({ providedIn: 'root' })
export class JobService {
  private http = inject(HttpClient);

  createDraft(request: JobBasicInfoRequest): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs`, request);
  }

  updateBasicInfo(jobId: string, request: JobBasicInfoRequest): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.put<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/basic-info`, request);
  }

  updateRequirements(jobId: string, request: JobRequirementsRequest): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.put<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/requirements`, request);
  }

  updatePipelineStages(jobId: string, request: UpdatePipelineStagesRequest): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.put<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/pipeline-stages`, request);
  }

  getJobDetail(jobId: string): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.get<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}`);
  }

  publish(jobId: string): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/publish`, {});
  }

  pause(jobId: string, request?: StatusChangeRequest): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/pause`, request ?? {});
  }

  resume(jobId: string): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/resume`, {});
  }

  close(jobId: string, request?: StatusChangeRequest): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/close`, request ?? {});
  }

  archive(jobId: string, request?: StatusChangeRequest): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/archive`, request ?? {});
  }

  restore(jobId: string): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/restore`, {});
  }

  deleteJob(jobId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/jobs/${jobId}`);
  }

  duplicate(jobId: string): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/duplicate`, {});
  }

  assign(jobId: string, request: AssignJobRequest): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.put<ApiResponse<JobDetailResponse>>(`${BASE_URL}/jobs/${jobId}/assignment`, request);
  }

  search(criteria: JobSearchCriteria, page = 0, size = 100): Observable<ApiResponse<PagedResponse<JobSummaryResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    for (const [key, value] of Object.entries(criteria)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<ApiResponse<PagedResponse<JobSummaryResponse>>>(`${BASE_URL}/jobs`, { params });
  }

  getCounts(): Observable<ApiResponse<JobCountsResponse>> {
    return this.http.get<ApiResponse<JobCountsResponse>>(`${BASE_URL}/jobs/counts`);
  }

  getDashboard(): Observable<ApiResponse<JobDashboardResponse>> {
    return this.http.get<ApiResponse<JobDashboardResponse>>(`${BASE_URL}/jobs/dashboard`);
  }

  saveAsTemplate(jobId: string, request: SaveTemplateRequest): Observable<ApiResponse<JobTemplateResponse>> {
    return this.http.post<ApiResponse<JobTemplateResponse>>(`${BASE_URL}/job-templates/from-job/${jobId}`, request);
  }

  listTemplates(): Observable<ApiResponse<JobTemplateResponse[]>> {
    return this.http.get<ApiResponse<JobTemplateResponse[]>>(`${BASE_URL}/job-templates`);
  }

  createDraftFromTemplate(templateId: string): Observable<ApiResponse<JobDetailResponse>> {
    return this.http.post<ApiResponse<JobDetailResponse>>(`${BASE_URL}/job-templates/${templateId}/create-draft`, {});
  }

  deleteTemplate(templateId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/job-templates/${templateId}`);
  }

  browsePublicJobs(criteria: CandidateJobBrowseCriteria = {}, page = 0, size = 100): Observable<ApiResponse<PagedResponse<CandidateJobSummaryResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    for (const [key, value] of Object.entries(criteria)) {
      if (value !== undefined && value !== null && value !== '') {
        params = params.set(key, String(value));
      }
    }
    return this.http.get<ApiResponse<PagedResponse<CandidateJobSummaryResponse>>>(`${BASE_URL}/jobs/public`, { params });
  }

  getPublicJobDetail(jobId: string): Observable<ApiResponse<CandidateJobDetailResponse>> {
    return this.http.get<ApiResponse<CandidateJobDetailResponse>>(`${BASE_URL}/jobs/public/${jobId}`);
  }
}
