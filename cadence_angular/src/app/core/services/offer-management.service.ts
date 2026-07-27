import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  ApproveOfferRequest,
  CandidateDeclineRequest,
  CandidateNegotiationRequest,
  CandidateOfferResponse,
  CreateOrUpdateOfferRequest,
  OfferDashboardStatsResponse,
  OfferDetailResponse,
  OfferListItemResponse,
  OfferStatus,
  WithdrawOfferRequest,
} from '../models/offer-management.model';

const BASE_URL = `${environment.apiBaseUrl}/offer-management/api/v1`;
const RECRUITER_URL = `${BASE_URL}/offers`;
const CANDIDATE_URL = `${BASE_URL}/candidate/offers`;

@Injectable({ providedIn: 'root' })
export class OfferManagementService {
  private http = inject(HttpClient);

  // ---- Recruiter ----

  create(request: CreateOrUpdateOfferRequest): Observable<ApiResponse<OfferDetailResponse>> {
    return this.http.post<ApiResponse<OfferDetailResponse>>(RECRUITER_URL, request);
  }

  update(offerId: string, request: CreateOrUpdateOfferRequest): Observable<ApiResponse<OfferDetailResponse>> {
    return this.http.put<ApiResponse<OfferDetailResponse>>(`${RECRUITER_URL}/${offerId}`, request);
  }

  deleteDraft(offerId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${RECRUITER_URL}/${offerId}`);
  }

  list(status?: OfferStatus, page = 0, size = 20): Observable<ApiResponse<PagedResponse<OfferListItemResponse>>> {
    let params = new HttpParams().set('page', page).set('size', size);
    if (status) params = params.set('status', status);
    return this.http.get<ApiResponse<PagedResponse<OfferListItemResponse>>>(RECRUITER_URL, { params });
  }

  getDetail(offerId: string): Observable<ApiResponse<OfferDetailResponse>> {
    return this.http.get<ApiResponse<OfferDetailResponse>>(`${RECRUITER_URL}/${offerId}`);
  }

  generateDocument(offerId: string): Observable<Blob> {
    return this.http.post(`${RECRUITER_URL}/${offerId}/generate`, {}, { responseType: 'blob' });
  }

  previewDocument(offerId: string): Observable<Blob> {
    return this.http.post(`${RECRUITER_URL}/${offerId}/preview`, {}, { responseType: 'blob' });
  }

  downloadDocument(offerId: string): Observable<Blob> {
    return this.http.get(`${RECRUITER_URL}/${offerId}/document`, { responseType: 'blob' });
  }

  approve(offerId: string, request: ApproveOfferRequest): Observable<ApiResponse<OfferDetailResponse>> {
    return this.http.post<ApiResponse<OfferDetailResponse>>(`${RECRUITER_URL}/${offerId}/approve`, request);
  }

  send(offerId: string): Observable<ApiResponse<OfferDetailResponse>> {
    return this.http.post<ApiResponse<OfferDetailResponse>>(`${RECRUITER_URL}/${offerId}/send`, {});
  }

  withdraw(offerId: string, request?: WithdrawOfferRequest): Observable<ApiResponse<OfferDetailResponse>> {
    return this.http.post<ApiResponse<OfferDetailResponse>>(`${RECRUITER_URL}/${offerId}/withdraw`, request ?? {});
  }

  history(page = 0, size = 20): Observable<ApiResponse<PagedResponse<OfferListItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<OfferListItemResponse>>>(`${RECRUITER_URL}/history`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  pending(page = 0, size = 20): Observable<ApiResponse<PagedResponse<OfferListItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<OfferListItemResponse>>>(`${RECRUITER_URL}/pending`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  accepted(page = 0, size = 20): Observable<ApiResponse<PagedResponse<OfferListItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<OfferListItemResponse>>>(`${RECRUITER_URL}/accepted`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  rejected(page = 0, size = 20): Observable<ApiResponse<PagedResponse<OfferListItemResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<OfferListItemResponse>>>(`${RECRUITER_URL}/rejected`, {
      params: new HttpParams().set('page', page).set('size', size),
    });
  }

  stats(): Observable<ApiResponse<OfferDashboardStatsResponse>> {
    return this.http.get<ApiResponse<OfferDashboardStatsResponse>>(`${RECRUITER_URL}/stats`);
  }

  // ---- Candidate ----

  listMyOffers(): Observable<ApiResponse<CandidateOfferResponse[]>> {
    return this.http.get<ApiResponse<CandidateOfferResponse[]>>(CANDIDATE_URL);
  }

  getMyOffer(offerId: string): Observable<ApiResponse<CandidateOfferResponse>> {
    return this.http.get<ApiResponse<CandidateOfferResponse>>(`${CANDIDATE_URL}/${offerId}`);
  }

  downloadMyOffer(offerId: string): Observable<Blob> {
    return this.http.get(`${CANDIDATE_URL}/${offerId}/download`, { responseType: 'blob' });
  }

  acceptMyOffer(offerId: string): Observable<ApiResponse<CandidateOfferResponse>> {
    return this.http.post<ApiResponse<CandidateOfferResponse>>(`${CANDIDATE_URL}/${offerId}/accept`, {});
  }

  rejectMyOffer(offerId: string, request?: CandidateDeclineRequest): Observable<ApiResponse<CandidateOfferResponse>> {
    return this.http.post<ApiResponse<CandidateOfferResponse>>(`${CANDIDATE_URL}/${offerId}/reject`, request ?? {});
  }

  requestNegotiation(offerId: string, request?: CandidateNegotiationRequest): Observable<ApiResponse<void>> {
    return this.http.post<ApiResponse<void>>(`${CANDIDATE_URL}/${offerId}/request-negotiation`, request ?? {});
  }
}
