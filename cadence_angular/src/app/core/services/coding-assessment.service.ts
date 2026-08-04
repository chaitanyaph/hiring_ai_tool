import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  AntiCheatEventRequest,
  AssessmentDetailsResponse,
  AssessmentListItemResponse,
  AssessmentResponse,
  AssessmentResultResponse,
  AssessmentStatus,
  BulkImportTestCasesRequest,
  CandidateAssessmentHistoryItemResponse,
  CandidateAssessmentIntroResponse,
  CandidateAssessmentStatus,
  CodingAnalyticsResponse,
  CodingQueueItemResponse,
  CodingResultsSummaryResponse,
  CreateAssessmentRequest,
  CreateQuestionRequest,
  CreateTestCaseRequest,
  Difficulty,
  FinishAssessmentRequest,
  IdeQuestionResponse,
  LeaderboardItemResponse,
  MarkForReviewRequest,
  QuestionResponse,
  QuestionStatus,
  QuestionTestCaseResponse,
  ReorderTestCasesRequest,
  RunCodeRequest,
  RunCodeResponse,
  StartAssessmentRequest,
  SubmissionDrawerResponse,
  SubmissionHistoryItemResponse,
  SubmitCodeRequest,
  SubmitCodeResponse,
  UpdateQuestionRequest,
  AiCodeReviewResponse,
} from '../models/coding-assessment.model';

const BASE_URL = `${environment.apiBaseUrl}/coding-assessment/api/v1`;

@Injectable({ providedIn: 'root' })
export class CodingAssessmentService {
  private http = inject(HttpClient);

  // ---- Recruiter: assessment CRUD ----

  create(request: CreateAssessmentRequest): Observable<ApiResponse<AssessmentResponse>> {
    return this.http.post<ApiResponse<AssessmentResponse>>(`${BASE_URL}/assessments`, request);
  }

  publish(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/assessments/${id}/publish`, {});
  }

  clone(id: string): Observable<ApiResponse<AssessmentResponse>> {
    return this.http.post<ApiResponse<AssessmentResponse>>(`${BASE_URL}/assessments/${id}/clone`, {});
  }

  archive(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/assessments/${id}/archive`, {});
  }

  close(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/assessments/${id}/close`, {});
  }

  list(status?: AssessmentStatus, jobId?: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<AssessmentListItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (jobId) params = params.set('jobId', jobId);
    return this.http.get<ApiResponse<PagedResponse<AssessmentListItemResponse>>>(`${BASE_URL}/assessments`, { params });
  }

  get(id: string): Observable<ApiResponse<AssessmentResponse>> {
    return this.http.get<ApiResponse<AssessmentResponse>>(`${BASE_URL}/assessments/${id}`);
  }

  getDetails(id: string): Observable<ApiResponse<AssessmentDetailsResponse>> {
    return this.http.get<ApiResponse<AssessmentDetailsResponse>>(`${BASE_URL}/assessments/${id}/details`);
  }

  getQueue(id: string, status?: CandidateAssessmentStatus, page = 0, size = 20): Observable<ApiResponse<PagedResponse<CodingQueueItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PagedResponse<CodingQueueItemResponse>>>(`${BASE_URL}/assessments/${id}/queue`, { params });
  }

  sendReminders(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/assessments/${id}/send-reminders`, {});
  }

  remindCandidate(id: string, applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/assessments/${id}/candidates/${applicationId}/remind`, {});
  }

  // ---- Recruiter: results / leaderboard / analytics ----

  getResultsSummary(assessmentId: string): Observable<ApiResponse<CodingResultsSummaryResponse>> {
    return this.http.get<ApiResponse<CodingResultsSummaryResponse>>(`${BASE_URL}/assessments/${assessmentId}/results/summary`);
  }

  getLeaderboard(assessmentId: string): Observable<ApiResponse<LeaderboardItemResponse[]>> {
    return this.http.get<ApiResponse<LeaderboardItemResponse[]>>(`${BASE_URL}/leaderboard/${assessmentId}`);
  }

  getCompleted(assessmentId: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<LeaderboardItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<LeaderboardItemResponse>>>(`${BASE_URL}/submissions/${assessmentId}`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  getSubmissionDrawer(assessmentId: string, applicationId: string): Observable<ApiResponse<SubmissionDrawerResponse>> {
    return this.http.get<ApiResponse<SubmissionDrawerResponse>>(`${BASE_URL}/submissions/${assessmentId}/${applicationId}`);
  }

  moveToNextStage(assessmentId: string, applicationId: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/submissions/${assessmentId}/${applicationId}/move-to-next-stage`, {});
  }

  getAnalytics(assessmentId: string): Observable<ApiResponse<CodingAnalyticsResponse>> {
    return this.http.get<ApiResponse<CodingAnalyticsResponse>>(`${BASE_URL}/analytics/${assessmentId}`);
  }

  getAiCodeReview(submissionId: string): Observable<ApiResponse<AiCodeReviewResponse>> {
    return this.http.get<ApiResponse<AiCodeReviewResponse>>(`${BASE_URL}/submissions/detail/${submissionId}/ai-review`);
  }

  // ---- Candidate-facing ----

  getHistory(page = 0, size = 20): Observable<ApiResponse<PagedResponse<CandidateAssessmentHistoryItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<CandidateAssessmentHistoryItemResponse>>>(`${BASE_URL}/candidate/assessments`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  getIntro(id: string): Observable<ApiResponse<CandidateAssessmentIntroResponse>> {
    return this.http.get<ApiResponse<CandidateAssessmentIntroResponse>>(`${BASE_URL}/candidate/assessments/${id}`);
  }

  acceptRules(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/candidate/assessments/${id}/accept-rules`, {});
  }

  start(id: string, request?: StartAssessmentRequest): Observable<ApiResponse<IdeQuestionResponse>> {
    return this.http.post<ApiResponse<IdeQuestionResponse>>(`${BASE_URL}/candidate/assessments/${id}/start`, request ?? {});
  }

  goToQuestion(id: string, questionIndex: number): Observable<ApiResponse<IdeQuestionResponse>> {
    return this.http.get<ApiResponse<IdeQuestionResponse>>(`${BASE_URL}/candidate/assessments/${id}/questions/${questionIndex}`);
  }

  runCode(request: RunCodeRequest): Observable<ApiResponse<RunCodeResponse>> {
    return this.http.post<ApiResponse<RunCodeResponse>>(`${BASE_URL}/candidate/run`, request);
  }

  submitCode(request: SubmitCodeRequest): Observable<ApiResponse<SubmitCodeResponse>> {
    return this.http.post<ApiResponse<SubmitCodeResponse>>(`${BASE_URL}/candidate/submit`, request);
  }

  markForReview(id: string, request: MarkForReviewRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/candidate/assessments/${id}/mark-for-review`, request);
  }

  finish(id: string, request?: FinishAssessmentRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/candidate/assessments/${id}/finish`, request ?? {});
  }

  getSubmissionHistory(id: string): Observable<ApiResponse<SubmissionHistoryItemResponse[]>> {
    return this.http.get<ApiResponse<SubmissionHistoryItemResponse[]>>(`${BASE_URL}/candidate/submissions/${id}`);
  }

  getResult(id: string): Observable<ApiResponse<AssessmentResultResponse>> {
    return this.http.get<ApiResponse<AssessmentResultResponse>>(`${BASE_URL}/candidate/result/${id}`);
  }

  recordAntiCheatEvent(id: string, request: AntiCheatEventRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${BASE_URL}/candidate/assessments/${id}/anti-cheat-events`, request);
  }

  // ---- Question bank ----

  createQuestion(request: CreateQuestionRequest): Observable<ApiResponse<QuestionResponse>> {
    return this.http.post<ApiResponse<QuestionResponse>>(`${BASE_URL}/questions`, request);
  }

  updateQuestion(id: string, request: UpdateQuestionRequest): Observable<ApiResponse<QuestionResponse>> {
    return this.http.put<ApiResponse<QuestionResponse>>(`${BASE_URL}/questions/${id}`, request);
  }

  deleteQuestion(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/questions/${id}`);
  }

  getQuestion(id: string): Observable<ApiResponse<QuestionResponse>> {
    return this.http.get<ApiResponse<QuestionResponse>>(`${BASE_URL}/questions/${id}`);
  }

  listQuestions(
    difficulty?: Difficulty, status?: QuestionStatus, search?: string, page = 0, size = 20
  ): Observable<ApiResponse<PagedResponse<QuestionResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (difficulty) params = params.set('difficulty', difficulty);
    if (status) params = params.set('status', status);
    if (search) params = params.set('search', search);
    return this.http.get<ApiResponse<PagedResponse<QuestionResponse>>>(`${BASE_URL}/questions`, { params });
  }

  listActiveQuestions(): Observable<ApiResponse<QuestionResponse[]>> {
    return this.http.get<ApiResponse<QuestionResponse[]>>(`${BASE_URL}/questions/active`);
  }

  duplicateQuestion(id: string): Observable<ApiResponse<QuestionResponse>> {
    return this.http.post<ApiResponse<QuestionResponse>>(`${BASE_URL}/questions/${id}/duplicate`, {});
  }

  activateQuestion(id: string): Observable<ApiResponse<QuestionResponse>> {
    return this.http.post<ApiResponse<QuestionResponse>>(`${BASE_URL}/questions/${id}/activate`, {});
  }

  deactivateQuestion(id: string): Observable<ApiResponse<QuestionResponse>> {
    return this.http.post<ApiResponse<QuestionResponse>>(`${BASE_URL}/questions/${id}/deactivate`, {});
  }

  archiveQuestion(id: string): Observable<ApiResponse<QuestionResponse>> {
    return this.http.post<ApiResponse<QuestionResponse>>(`${BASE_URL}/questions/${id}/archive`, {});
  }

  // ---- Question bank: test cases ----

  listTestCases(questionId: string): Observable<ApiResponse<QuestionTestCaseResponse[]>> {
    return this.http.get<ApiResponse<QuestionTestCaseResponse[]>>(`${BASE_URL}/questions/${questionId}/test-cases`);
  }

  addTestCase(questionId: string, request: CreateTestCaseRequest): Observable<ApiResponse<QuestionTestCaseResponse>> {
    return this.http.post<ApiResponse<QuestionTestCaseResponse>>(`${BASE_URL}/questions/${questionId}/test-cases`, request);
  }

  updateTestCase(questionId: string, testCaseId: string, request: CreateTestCaseRequest): Observable<ApiResponse<QuestionTestCaseResponse>> {
    return this.http.put<ApiResponse<QuestionTestCaseResponse>>(`${BASE_URL}/questions/${questionId}/test-cases/${testCaseId}`, request);
  }

  deleteTestCase(questionId: string, testCaseId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/questions/${questionId}/test-cases/${testCaseId}`);
  }

  duplicateTestCase(questionId: string, testCaseId: string): Observable<ApiResponse<QuestionTestCaseResponse>> {
    return this.http.post<ApiResponse<QuestionTestCaseResponse>>(`${BASE_URL}/questions/${questionId}/test-cases/${testCaseId}/duplicate`, {});
  }

  reorderTestCases(questionId: string, request: ReorderTestCasesRequest): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${BASE_URL}/questions/${questionId}/test-cases/reorder`, request);
  }

  bulkImportTestCases(questionId: string, request: BulkImportTestCasesRequest): Observable<ApiResponse<QuestionTestCaseResponse[]>> {
    return this.http.post<ApiResponse<QuestionTestCaseResponse[]>>(`${BASE_URL}/questions/${questionId}/test-cases/bulk-import`, request);
  }
}
