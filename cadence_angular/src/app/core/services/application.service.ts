import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  AddNoteRequest,
  ApplicationHistoryResponse,
  ApplicationResponse,
  ApplicationSearchCriteria,
  AssignHiringManagerRequest,
  AssignRecruiterRequest,
  NoteResponse,
  StatusChangeRequest,
  TimelineEntryResponse,
} from '../models/application.model';

const BASE_URL = `${environment.apiBaseUrl}/application/api/v1/applications`;

function toParams(criteria: ApplicationSearchCriteria, page: number, size: number): HttpParams {
  let params = new HttpParams().set('page', page).set('size', size);
  Object.entries(criteria).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      params = params.set(key, String(value));
    }
  });
  return params;
}

@Injectable({ providedIn: 'root' })
export class ApplicationService {
  private http = inject(HttpClient);

  // ---- Candidate-facing ----

  apply(jobId: string, resumeId: string): Observable<ApiResponse<ApplicationResponse>> {
    return this.http.post<ApiResponse<ApplicationResponse>>(BASE_URL, { jobId, resumeId });
  }

  listMyApplications(): Observable<ApiResponse<ApplicationResponse[]>> {
    return this.http.get<ApiResponse<ApplicationResponse[]>>(`${BASE_URL}/my`);
  }

  withdraw(id: string): Observable<ApiResponse<ApplicationResponse>> {
    return this.http.delete<ApiResponse<ApplicationResponse>>(`${BASE_URL}/${id}`);
  }

  acceptOffer(id: string): Observable<ApiResponse<ApplicationResponse>> {
    return this.http.post<ApiResponse<ApplicationResponse>>(`${BASE_URL}/${id}/accept-offer`, {});
  }

  rejectOffer(id: string): Observable<ApiResponse<ApplicationResponse>> {
    return this.http.post<ApiResponse<ApplicationResponse>>(`${BASE_URL}/${id}/reject-offer`, {});
  }

  // ---- Recruiter-facing ----

  searchCompanyApplications(
    companyId: string,
    criteria: ApplicationSearchCriteria = {},
    page = 0,
    size = 20
  ): Observable<ApiResponse<PagedResponse<ApplicationResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<ApplicationResponse>>>(`${BASE_URL}/company/${companyId}`, {
      params: toParams(criteria, page, size),
    });
  }

  getJobApplications(jobId: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<ApplicationResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<ApplicationResponse>>>(`${BASE_URL}/job/${jobId}`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  changeStatus(id: string, request: StatusChangeRequest): Observable<ApiResponse<ApplicationResponse>> {
    return this.http.put<ApiResponse<ApplicationResponse>>(`${BASE_URL}/${id}/status`, request);
  }

  assignRecruiter(id: string, request: AssignRecruiterRequest): Observable<ApiResponse<ApplicationResponse>> {
    return this.http.put<ApiResponse<ApplicationResponse>>(`${BASE_URL}/${id}/assign-recruiter`, request);
  }

  assignHiringManager(id: string, request: AssignHiringManagerRequest): Observable<ApiResponse<ApplicationResponse>> {
    return this.http.put<ApiResponse<ApplicationResponse>>(`${BASE_URL}/${id}/assign-hiring-manager`, request);
  }

  addNote(id: string, request: AddNoteRequest): Observable<ApiResponse<NoteResponse>> {
    return this.http.post<ApiResponse<NoteResponse>>(`${BASE_URL}/${id}/notes`, request);
  }

  // ---- Shared ----

  getDetail(id: string): Observable<ApiResponse<ApplicationResponse>> {
    return this.http.get<ApiResponse<ApplicationResponse>>(`${BASE_URL}/${id}`);
  }

  getTimeline(id: string): Observable<ApiResponse<TimelineEntryResponse[]>> {
    return this.http.get<ApiResponse<TimelineEntryResponse[]>>(`${BASE_URL}/${id}/timeline`);
  }

  getHistory(id: string): Observable<ApiResponse<ApplicationHistoryResponse>> {
    return this.http.get<ApiResponse<ApplicationHistoryResponse>>(`${BASE_URL}/${id}/history`);
  }
}
