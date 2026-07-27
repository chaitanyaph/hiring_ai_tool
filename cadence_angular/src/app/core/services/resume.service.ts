import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import { RenameResumeRequest, ResumeResponse } from '../models/resume.model';

const BASE_URL = `${environment.apiBaseUrl}/resume/api/v1/resumes`;
const RECRUITER_BASE_URL = `${environment.apiBaseUrl}/resume/api/v1/internal/resumes`;

@Injectable({ providedIn: 'root' })
export class ResumeService {
  private http = inject(HttpClient);

  upload(file: File, displayName?: string): Observable<ApiResponse<ResumeResponse>> {
    const formData = new FormData();
    formData.append('file', file);
    if (displayName) formData.append('displayName', displayName);
    return this.http.post<ApiResponse<ResumeResponse>>(`${BASE_URL}/upload`, formData);
  }

  listMyResumes(): Observable<ApiResponse<ResumeResponse[]>> {
    return this.http.get<ApiResponse<ResumeResponse[]>>(BASE_URL);
  }

  getDetail(resumeId: string): Observable<ApiResponse<ResumeResponse>> {
    return this.http.get<ApiResponse<ResumeResponse>>(`${BASE_URL}/${resumeId}`);
  }

  download(resumeId: string): Observable<Blob> {
    return this.http.get(`${BASE_URL}/${resumeId}/download`, { responseType: 'blob' });
  }

  preview(resumeId: string): Observable<Blob> {
    return this.http.get(`${BASE_URL}/${resumeId}/preview`, { responseType: 'blob' });
  }

  setDefault(resumeId: string): Observable<ApiResponse<ResumeResponse>> {
    return this.http.put<ApiResponse<ResumeResponse>>(`${BASE_URL}/${resumeId}/default`, {});
  }

  rename(resumeId: string, request: RenameResumeRequest): Observable<ApiResponse<ResumeResponse>> {
    return this.http.put<ApiResponse<ResumeResponse>>(`${BASE_URL}/${resumeId}/rename`, request);
  }

  delete(resumeId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/${resumeId}`);
  }

  /**
   * Recruiter preview/download, scoped server-side to candidates who applied to the
   * recruiter's own company. Not yet called from any component -- the recruiter-facing
   * candidate-detail screen (candidate-detail.component.ts) still only has a local mock
   * candidateId/no resumeId until Module 6 wires it to application-service's real
   * candidate/application data, which is what will supply a real resumeId to pass here.
   */
  recruiterDownload(resumeId: string): Observable<Blob> {
    return this.http.get(`${RECRUITER_BASE_URL}/${resumeId}/download`, { responseType: 'blob' });
  }

  recruiterPreview(resumeId: string): Observable<Blob> {
    return this.http.get(`${RECRUITER_BASE_URL}/${resumeId}/preview`, { responseType: 'blob' });
  }
}
