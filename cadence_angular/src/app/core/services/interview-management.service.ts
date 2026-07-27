import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  ActivityLogResponse,
  CancelInterviewRequest,
  CandidateInterviewResponse,
  CandidateTimelineResponse,
  CreateInterviewRoundRequest,
  InterviewDetailResponse,
  InterviewFeedbackResponse,
  InterviewListItemResponse,
  InterviewRoundResponse,
  InterviewStatus,
  RecruiterDecisionRequest,
  RequestRescheduleRequest,
  RescheduleInterviewRequest,
  ScheduleInterviewRequest,
  SubmitFeedbackRequest,
  UpdateInterviewRoundRequest,
} from '../models/interview-management.model';

const BASE_URL = `${environment.apiBaseUrl}/interview-management/api/v1`;
const RECRUITER_URL = `${BASE_URL}/recruiter/interviews`;
const ROUNDS_URL = `${BASE_URL}/recruiter/interview-rounds`;
const CANDIDATE_URL = `${BASE_URL}/candidate`;

@Injectable({ providedIn: 'root' })
export class InterviewManagementService {
  private http = inject(HttpClient);

  // ---- Recruiter: interviews ----

  schedule(request: ScheduleInterviewRequest): Observable<ApiResponse<InterviewDetailResponse>> {
    return this.http.post<ApiResponse<InterviewDetailResponse>>(RECRUITER_URL, request);
  }

  reschedule(id: string, request: RescheduleInterviewRequest): Observable<ApiResponse<InterviewDetailResponse>> {
    return this.http.put<ApiResponse<InterviewDetailResponse>>(`${RECRUITER_URL}/${id}/reschedule`, request);
  }

  cancel(id: string, request?: CancelInterviewRequest): Observable<ApiResponse<InterviewDetailResponse>> {
    return this.http.put<ApiResponse<InterviewDetailResponse>>(`${RECRUITER_URL}/${id}/cancel`, request ?? {});
  }

  list(status?: InterviewStatus, page = 0, size = 20): Observable<ApiResponse<PagedResponse<InterviewListItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PagedResponse<InterviewListItemResponse>>>(RECRUITER_URL, { params });
  }

  getDetail(id: string): Observable<ApiResponse<InterviewDetailResponse>> {
    return this.http.get<ApiResponse<InterviewDetailResponse>>(`${RECRUITER_URL}/${id}`);
  }

  getActivity(id: string): Observable<ApiResponse<ActivityLogResponse[]>> {
    return this.http.get<ApiResponse<ActivityLogResponse[]>>(`${RECRUITER_URL}/${id}/activity`);
  }

  submitFeedback(id: string, request: SubmitFeedbackRequest): Observable<ApiResponse<InterviewFeedbackResponse>> {
    return this.http.post<ApiResponse<InterviewFeedbackResponse>>(`${RECRUITER_URL}/${id}/feedback`, request);
  }

  getFeedback(id: string): Observable<ApiResponse<InterviewFeedbackResponse[]>> {
    return this.http.get<ApiResponse<InterviewFeedbackResponse[]>>(`${RECRUITER_URL}/${id}/feedback`);
  }

  decide(id: string, request: RecruiterDecisionRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${RECRUITER_URL}/${id}/decision`, request);
  }

  getTimeline(applicationId: string): Observable<ApiResponse<CandidateTimelineResponse[]>> {
    return this.http.get<ApiResponse<CandidateTimelineResponse[]>>(`${RECRUITER_URL}/timeline/${applicationId}`);
  }

  // ---- Recruiter: interview rounds ----

  createRound(request: CreateInterviewRoundRequest): Observable<ApiResponse<InterviewRoundResponse>> {
    return this.http.post<ApiResponse<InterviewRoundResponse>>(ROUNDS_URL, request);
  }

  updateRound(id: string, request: UpdateInterviewRoundRequest): Observable<ApiResponse<InterviewRoundResponse>> {
    return this.http.put<ApiResponse<InterviewRoundResponse>>(`${ROUNDS_URL}/${id}`, request);
  }

  deleteRound(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${ROUNDS_URL}/${id}`);
  }

  listRounds(activeOnly = false): Observable<ApiResponse<InterviewRoundResponse[]>> {
    return this.http.get<ApiResponse<InterviewRoundResponse[]>>(ROUNDS_URL, {
      params: new HttpParams().set('activeOnly', activeOnly),
    });
  }

  // ---- Candidate ----

  listMyInterviews(upcomingOnly = false): Observable<ApiResponse<CandidateInterviewResponse[]>> {
    return this.http.get<ApiResponse<CandidateInterviewResponse[]>>(`${CANDIDATE_URL}/interviews`, {
      params: new HttpParams().set('upcomingOnly', upcomingOnly),
    });
  }

  getMyInterviewDetail(id: string): Observable<ApiResponse<CandidateInterviewResponse>> {
    return this.http.get<ApiResponse<CandidateInterviewResponse>>(`${CANDIDATE_URL}/interviews/${id}`);
  }

  requestReschedule(id: string, request?: RequestRescheduleRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${CANDIDATE_URL}/interviews/${id}/request-reschedule`, request ?? {});
  }

  getMyTimeline(applicationId: string): Observable<ApiResponse<CandidateTimelineResponse[]>> {
    return this.http.get<ApiResponse<CandidateTimelineResponse[]>>(`${CANDIDATE_URL}/interviews/timeline/${applicationId}`);
  }
}
