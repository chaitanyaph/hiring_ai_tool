import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  CreateTemplateRequest,
  EmailDashboardStatsResponse,
  EmailQueueDetailResponse,
  EmailQueueItemResponse,
  EmailStatus,
  NotificationLogResponse,
  NotificationResponse,
  NotificationStatus,
  PreferenceResponse,
  RetryBulkRequest,
  TemplatePreviewResponse,
  TemplateResponse,
  UpdatePreferencesRequest,
  UpdateTemplateRequest,
} from '../models/notification.model';

const BASE_URL = `${environment.apiBaseUrl}/notification/api/v1`;
const NOTIFICATIONS_URL = `${BASE_URL}/notifications`;
const TEMPLATES_URL = `${BASE_URL}/templates`;
const EMAIL_QUEUE_URL = `${BASE_URL}/email-queue`;
const EMAIL_HISTORY_URL = `${BASE_URL}/email-history`;
const NOTIFICATION_LOGS_URL = `${BASE_URL}/notification-logs`;

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private http = inject(HttpClient);

  // ---- Personal in-app notifications ----

  list(status?: NotificationStatus, page = 0, size = 20): Observable<ApiResponse<PagedResponse<NotificationResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PagedResponse<NotificationResponse>>>(NOTIFICATIONS_URL, { params });
  }

  get(id: string): Observable<ApiResponse<NotificationResponse>> {
    return this.http.get<ApiResponse<NotificationResponse>>(`${NOTIFICATIONS_URL}/${id}`);
  }

  markRead(id: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${NOTIFICATIONS_URL}/${id}/read`, {});
  }

  markAllRead(): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${NOTIFICATIONS_URL}/read-all`, {});
  }

  archive(id: string): Observable<ApiResponse<void>> {
    return this.http.put<ApiResponse<void>>(`${NOTIFICATIONS_URL}/${id}/archive`, {});
  }

  delete(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${NOTIFICATIONS_URL}/${id}`);
  }

  unreadCount(): Observable<ApiResponse<number>> {
    return this.http.get<ApiResponse<number>>(`${NOTIFICATIONS_URL}/unread-count`);
  }

  history(page = 0, size = 20): Observable<ApiResponse<PagedResponse<NotificationResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<NotificationResponse>>>(`${NOTIFICATIONS_URL}/history`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  getPreferences(): Observable<ApiResponse<PreferenceResponse[]>> {
    return this.http.get<ApiResponse<PreferenceResponse[]>>(`${NOTIFICATIONS_URL}/preferences`);
  }

  updatePreferences(request: UpdatePreferencesRequest): Observable<ApiResponse<PreferenceResponse[]>> {
    return this.http.put<ApiResponse<PreferenceResponse[]>>(`${NOTIFICATIONS_URL}/preferences`, request);
  }

  // ---- Templates ----

  createTemplate(request: CreateTemplateRequest): Observable<ApiResponse<TemplateResponse>> {
    return this.http.post<ApiResponse<TemplateResponse>>(TEMPLATES_URL, request);
  }

  updateTemplate(id: string, request: UpdateTemplateRequest): Observable<ApiResponse<TemplateResponse>> {
    return this.http.put<ApiResponse<TemplateResponse>>(`${TEMPLATES_URL}/${id}`, request);
  }

  deleteTemplate(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${TEMPLATES_URL}/${id}`);
  }

  getTemplate(id: string): Observable<ApiResponse<TemplateResponse>> {
    return this.http.get<ApiResponse<TemplateResponse>>(`${TEMPLATES_URL}/${id}`);
  }

  listTemplates(): Observable<ApiResponse<TemplateResponse[]>> {
    return this.http.get<ApiResponse<TemplateResponse[]>>(TEMPLATES_URL);
  }

  previewTemplate(id: string): Observable<ApiResponse<TemplatePreviewResponse>> {
    return this.http.get<ApiResponse<TemplatePreviewResponse>>(`${TEMPLATES_URL}/${id}/preview`);
  }

  // ---- Email queue ----

  listEmailQueue(status?: EmailStatus, recipient?: string, page = 0, size = 20): Observable<ApiResponse<PagedResponse<EmailQueueItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    if (recipient) params = params.set('recipient', recipient);
    return this.http.get<ApiResponse<PagedResponse<EmailQueueItemResponse>>>(EMAIL_QUEUE_URL, { params });
  }

  getEmailDetail(id: string): Observable<ApiResponse<EmailQueueDetailResponse>> {
    return this.http.get<ApiResponse<EmailQueueDetailResponse>>(`${EMAIL_QUEUE_URL}/${id}`);
  }

  retryEmail(id: string): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${EMAIL_QUEUE_URL}/retry/${id}`, {});
  }

  retryBulk(request: RetryBulkRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${EMAIL_QUEUE_URL}/retry-bulk`, request);
  }

  cancelScheduled(id: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${EMAIL_QUEUE_URL}/${id}`);
  }

  getDashboardStats(): Observable<ApiResponse<EmailDashboardStatsResponse>> {
    return this.http.get<ApiResponse<EmailDashboardStatsResponse>>(`${EMAIL_QUEUE_URL}/stats`);
  }

  // ---- Email history ----

  listEmailHistory(recipient?: string, status?: EmailStatus, page = 0, size = 20): Observable<ApiResponse<PagedResponse<EmailQueueItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (recipient) params = params.set('recipient', recipient);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PagedResponse<EmailQueueItemResponse>>>(EMAIL_HISTORY_URL, { params });
  }

  // ---- Notification logs ----

  listLogs(source?: string, page = 0, size = 30): Observable<ApiResponse<PagedResponse<NotificationLogResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (source) params = params.set('source', source);
    return this.http.get<ApiResponse<PagedResponse<NotificationLogResponse>>>(NOTIFICATION_LOGS_URL, { params });
  }
}
