import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  CandidateAnalysisItemResponse,
  ParsedResumeResponse,
  ParserLogResponse,
  ParsingQueueItemResponse,
  ParsingQueueSummaryResponse,
  ParsingStatus,
  ParsingStatusResponse,
  ResumeMatchRankingItemResponse,
  ResumeMatchResponse,
  ResumeMatchSummaryResponse,
} from '../models/resume-parser.model';

const BASE_URL = `${environment.apiBaseUrl}/resume-parser/api/v1`;

@Injectable({ providedIn: 'root' })
export class ResumeParserService {
  private http = inject(HttpClient);

  // ---- Parsing queue ----

  getQueue(status?: ParsingStatus, search?: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<ParsingQueueItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);
    return this.http.get<ApiResponse<PagedResponse<ParsingQueueItemResponse>>>(`${BASE_URL}/parser/queue`, { params });
  }

  getQueueSummary(): Observable<ApiResponse<ParsingQueueSummaryResponse>> {
    return this.http.get<ApiResponse<ParsingQueueSummaryResponse>>(`${BASE_URL}/parser/queue/summary`);
  }

  // ---- Parsed resume detail ----

  getParsedResume(resumeId: string): Observable<ApiResponse<ParsedResumeResponse>> {
    return this.http.get<ApiResponse<ParsedResumeResponse>>(`${BASE_URL}/parser/resumes/${resumeId}`);
  }

  getParsingStatus(resumeId: string): Observable<ApiResponse<ParsingStatusResponse>> {
    return this.http.get<ApiResponse<ParsingStatusResponse>>(`${BASE_URL}/parser/resumes/${resumeId}/status`);
  }

  getParsingLogs(resumeId: string): Observable<ApiResponse<ParserLogResponse[]>> {
    return this.http.get<ApiResponse<ParserLogResponse[]>>(`${BASE_URL}/parser/resumes/${resumeId}/logs`);
  }

  retryParsing(resumeId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/parser/resumes/${resumeId}/retry`, {});
  }

  // ---- Resume analysis / matching ----

  getRanking(jobId: string, search?: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<ResumeMatchRankingItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (search) params = params.set('search', search);
    return this.http.get<ApiResponse<PagedResponse<ResumeMatchRankingItemResponse>>>(`${BASE_URL}/resume-analysis/jobs/${jobId}`, { params });
  }

  getMatchSummary(jobId: string): Observable<ApiResponse<ResumeMatchSummaryResponse>> {
    return this.http.get<ApiResponse<ResumeMatchSummaryResponse>>(`${BASE_URL}/resume-analysis/jobs/${jobId}/summary`);
  }

  getTop(jobId: string): Observable<ApiResponse<ResumeMatchRankingItemResponse[]>> {
    return this.http.get<ApiResponse<ResumeMatchRankingItemResponse[]>>(`${BASE_URL}/resume-analysis/top/${jobId}`);
  }

  getRecommendations(jobId?: string, departmentId?: string, minScore?: number, page = 0, size = 20): Observable<ApiResponse<ResumeMatchRankingItemResponse[]>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (jobId) params = params.set('jobId', jobId);
    if (departmentId) params = params.set('departmentId', departmentId);
    if (minScore != null) params = params.set('minScore', minScore);
    return this.http.get<ApiResponse<ResumeMatchRankingItemResponse[]>>(`${BASE_URL}/resume-analysis/recommendations`, { params });
  }

  getAnalysisByApplication(applicationId: string): Observable<ApiResponse<ResumeMatchResponse>> {
    return this.http.get<ApiResponse<ResumeMatchResponse>>(`${BASE_URL}/resume-analysis/applications/${applicationId}`);
  }

  getAnalysisByCandidate(candidateId: string): Observable<ApiResponse<CandidateAnalysisItemResponse[]>> {
    return this.http.get<ApiResponse<CandidateAnalysisItemResponse[]>>(`${BASE_URL}/resume-analysis/candidates/${candidateId}`);
  }

  recalculate(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/resume-analysis/recalculate/${applicationId}`, {});
  }
}
