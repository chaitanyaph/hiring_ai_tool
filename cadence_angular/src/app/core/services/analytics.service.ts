import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse } from '../models/api-response.model';
import {
  AssessmentAnalyticsResponse,
  CandidateAnalyticsResponse,
  DashboardResponse,
  FunnelResponse,
  InterviewAnalyticsResponse,
  JobAnalyticsResponse,
  OfferAnalyticsResponse,
  RecruiterPerformanceResponse,
  ReportFormat,
  ReportPeriod,
  ReportResponse,
  ResumeAnalyticsResponse,
} from '../models/analytics.model';

const BASE_URL = `${environment.apiBaseUrl}/analytics/api/v1`;
const DASHBOARD_URL = `${BASE_URL}/dashboard`;
const ANALYTICS_URL = `${BASE_URL}/analytics`;
const FUNNEL_URL = `${BASE_URL}/funnel`;
const RECRUITER_PERFORMANCE_URL = `${BASE_URL}/recruiter-performance`;
const REPORTS_URL = `${BASE_URL}/reports`;

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private http = inject(HttpClient);

  // ---- Dashboards ----

  getExecutiveDashboard(): Observable<ApiResponse<DashboardResponse>> {
    return this.http.get<ApiResponse<DashboardResponse>>(`${DASHBOARD_URL}/executive`);
  }

  getCompanyDashboard(): Observable<ApiResponse<DashboardResponse>> {
    return this.http.get<ApiResponse<DashboardResponse>>(`${DASHBOARD_URL}/company`);
  }

  getRecruiterDashboard(): Observable<ApiResponse<DashboardResponse>> {
    return this.http.get<ApiResponse<DashboardResponse>>(`${DASHBOARD_URL}/recruiter`);
  }

  getHrDashboard(): Observable<ApiResponse<DashboardResponse>> {
    return this.http.get<ApiResponse<DashboardResponse>>(`${DASHBOARD_URL}/hr`);
  }

  getHiringManagerDashboard(): Observable<ApiResponse<DashboardResponse>> {
    return this.http.get<ApiResponse<DashboardResponse>>(`${DASHBOARD_URL}/hiring-manager`);
  }

  // ---- Domain analytics ----

  getJobAnalytics(): Observable<ApiResponse<JobAnalyticsResponse>> {
    return this.http.get<ApiResponse<JobAnalyticsResponse>>(`${ANALYTICS_URL}/jobs`);
  }

  getCandidateAnalytics(): Observable<ApiResponse<CandidateAnalyticsResponse>> {
    return this.http.get<ApiResponse<CandidateAnalyticsResponse>>(`${ANALYTICS_URL}/candidates`);
  }

  getResumeAnalytics(): Observable<ApiResponse<ResumeAnalyticsResponse>> {
    return this.http.get<ApiResponse<ResumeAnalyticsResponse>>(`${ANALYTICS_URL}/resumes`);
  }

  getInterviewAnalytics(): Observable<ApiResponse<InterviewAnalyticsResponse>> {
    return this.http.get<ApiResponse<InterviewAnalyticsResponse>>(`${ANALYTICS_URL}/interviews`);
  }

  getAssessmentAnalytics(): Observable<ApiResponse<AssessmentAnalyticsResponse>> {
    return this.http.get<ApiResponse<AssessmentAnalyticsResponse>>(`${ANALYTICS_URL}/assessments`);
  }

  getOfferAnalytics(): Observable<ApiResponse<OfferAnalyticsResponse>> {
    return this.http.get<ApiResponse<OfferAnalyticsResponse>>(`${ANALYTICS_URL}/offers`);
  }

  // ---- Funnel ----

  getFunnel(): Observable<ApiResponse<FunnelResponse>> {
    return this.http.get<ApiResponse<FunnelResponse>>(FUNNEL_URL);
  }

  // ---- Recruiter performance ----

  getRecruiterPerformance(): Observable<ApiResponse<RecruiterPerformanceResponse[]>> {
    return this.http.get<ApiResponse<RecruiterPerformanceResponse[]>>(RECRUITER_PERFORMANCE_URL);
  }

  // ---- Reports ----

  getDailyReport(date?: string): Observable<ApiResponse<ReportResponse>> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.get<ApiResponse<ReportResponse>>(`${REPORTS_URL}/daily`, { params });
  }

  getMonthlyReport(month?: string): Observable<ApiResponse<ReportResponse>> {
    let params = new HttpParams();
    if (month) params = params.set('month', month);
    return this.http.get<ApiResponse<ReportResponse>>(`${REPORTS_URL}/monthly`, { params });
  }

  getYearlyReport(year?: number): Observable<ApiResponse<ReportResponse>> {
    let params = new HttpParams();
    if (year) params = params.set('year', year);
    return this.http.get<ApiResponse<ReportResponse>>(`${REPORTS_URL}/yearly`, { params });
  }

  exportReport(format: ReportFormat, period: ReportPeriod, date?: string, year?: number): Observable<Blob> {
    let params = new HttpParams().set('period', period);
    if (date) params = params.set('date', date);
    if (year) params = params.set('year', year);
    const path = format === ReportFormat.CSV ? 'csv' : format === ReportFormat.EXCEL ? 'excel' : 'pdf';
    return this.http.get(`${REPORTS_URL}/export/${path}`, { params, responseType: 'blob' });
  }
}
