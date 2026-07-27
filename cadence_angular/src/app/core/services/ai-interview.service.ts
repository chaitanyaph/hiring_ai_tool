import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  AnswerRequest,
  AssignRecruiterRequest,
  BulkApplicationIdsRequest,
  InterviewAnalysisSummaryResponse,
  InterviewCompletedItemResponse,
  InterviewDetailsResponse,
  InterviewEvaluationReportResponse,
  InterviewQueueItemResponse,
  InterviewQuestionResponse,
  InterviewResultResponse,
  InterviewSessionStatus,
  ShortlistItemResponse,
  ShortlistSummaryResponse,
  StartInterviewRequest,
} from '../models/ai-interview.model';

const BASE_URL = `${environment.apiBaseUrl}/ai-interview/api/v1/ai-interviews`;
const CANDIDATE_BASE_URL = `${environment.apiBaseUrl}/ai-interview/api/v1/candidate/interview`;

@Injectable({ providedIn: 'root' })
export class AiInterviewService {
  private http = inject(HttpClient);

  // ---- Shortlisting ----

  getShortlisted(jobId: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<ShortlistItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<ShortlistItemResponse>>>(`${BASE_URL}/shortlisted/${jobId}`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  getRejected(jobId: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<ShortlistItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<ShortlistItemResponse>>>(`${BASE_URL}/rejected/${jobId}`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  getManualReview(jobId: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<ShortlistItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<ShortlistItemResponse>>>(`${BASE_URL}/manual-review/${jobId}`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  getShortlistSummary(jobId: string): Observable<ApiResponse<ShortlistSummaryResponse>> {
    return this.http.get<ApiResponse<ShortlistSummaryResponse>>(`${BASE_URL}/shortlisting/summary/${jobId}`);
  }

  shortlistOne(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/${applicationId}/shortlist`, {});
  }

  rejectOneManual(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/${applicationId}/reject-manual`, {});
  }

  bulkShortlist(request: BulkApplicationIdsRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/manual-review/bulk-shortlist`, request);
  }

  bulkReject(request: BulkApplicationIdsRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/manual-review/bulk-reject`, request);
  }

  assignRecruiter(request: AssignRecruiterRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/manual-review/assign-recruiter`, request);
  }

  // ---- Interviews ----

  getQueue(jobId: string, status?: InterviewSessionStatus, page = 0, size = 20): Observable<ApiResponse<PagedResponse<InterviewQueueItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PagedResponse<InterviewQueueItemResponse>>>(`${BASE_URL}/queue/${jobId}`, { params });
  }

  getAnalysisSummary(jobId: string): Observable<ApiResponse<InterviewAnalysisSummaryResponse>> {
    return this.http.get<ApiResponse<InterviewAnalysisSummaryResponse>>(`${BASE_URL}/analysis-summary/${jobId}`);
  }

  getCompleted(jobId: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<InterviewCompletedItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<InterviewCompletedItemResponse>>>(`${BASE_URL}/completed/${jobId}`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  getReport(applicationId: string): Observable<ApiResponse<InterviewEvaluationReportResponse>> {
    return this.http.get<ApiResponse<InterviewEvaluationReportResponse>>(`${BASE_URL}/${applicationId}/report`);
  }

  start(applicationId: string, jobId: string, candidateId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/${applicationId}/start`, null, {
      params: new HttpParams().set('jobId', jobId).set('candidateId', candidateId),
    });
  }

  sendReminder(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/${applicationId}/send-reminder`, {});
  }

  resendInvite(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/${applicationId}/resend-invite`, {});
  }

  moveToCoding(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/${applicationId}/move-to-coding`, {});
  }

  reject(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/${applicationId}/reject`, {});
  }

  manualReviewFlag(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/${applicationId}/manual-review`, {});
  }

  // ---- Candidate-facing: the live interview flow ----

  getCandidateInterviewDetails(applicationId: string): Observable<ApiResponse<InterviewDetailsResponse>> {
    return this.http.get<ApiResponse<InterviewDetailsResponse>>(`${CANDIDATE_BASE_URL}/details`, {
      params: new HttpParams().set('applicationId', applicationId),
    });
  }

  startCandidateInterview(request: StartInterviewRequest): Observable<ApiResponse<InterviewQuestionResponse>> {
    return this.http.post<ApiResponse<InterviewQuestionResponse>>(`${CANDIDATE_BASE_URL}/start`, request);
  }

  submitCandidateAnswer(request: AnswerRequest): Observable<ApiResponse<InterviewQuestionResponse>> {
    return this.http.post<ApiResponse<InterviewQuestionResponse>>(`${CANDIDATE_BASE_URL}/answer`, request);
  }

  finishCandidateInterview(applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${CANDIDATE_BASE_URL}/finish`, null, {
      params: new HttpParams().set('applicationId', applicationId),
    });
  }

  getCandidateInterviewResult(applicationId: string): Observable<ApiResponse<InterviewResultResponse>> {
    return this.http.get<ApiResponse<InterviewResultResponse>>(`${CANDIDATE_BASE_URL}/result`, {
      params: new HttpParams().set('applicationId', applicationId),
    });
  }
}
