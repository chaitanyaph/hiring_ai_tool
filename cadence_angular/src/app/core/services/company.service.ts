import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';
import { ApiResponse, PagedResponse } from '../models/api-response.model';
import {
  CompanyResponse,
  CreateCompanyRequest,
  CreateTeamInvitationRequest,
  DepartmentRequest,
  DepartmentResponse,
  OfficeRequest,
  OfficeResponse,
  TeamInvitationResponse,
  UpdateCompanyRequest,
  UpdateTeamInvitationRequest,
} from '../models/company.model';

const BASE_URL = `${environment.apiBaseUrl}/company/api/v1`;

@Injectable({ providedIn: 'root' })
export class CompanyService {
  private http = inject(HttpClient);

  getCompany(companyId: string): Observable<ApiResponse<CompanyResponse>> {
    return this.http.get<ApiResponse<CompanyResponse>>(`${BASE_URL}/companies/${companyId}`);
  }

  updateCompany(companyId: string, request: UpdateCompanyRequest): Observable<ApiResponse<CompanyResponse>> {
    return this.http.put<ApiResponse<CompanyResponse>>(`${BASE_URL}/companies/${companyId}`, request);
  }

  /** Called internally by auth-service during COMPANY_ADMIN registration -- exposed here only for completeness, not used by any Angular screen. */
  createCompany(request: CreateCompanyRequest): Observable<ApiResponse<CompanyResponse>> {
    return this.http.post<ApiResponse<CompanyResponse>>(`${BASE_URL}/companies`, request);
  }

  listDepartments(companyId: string, page = 0, size = 100): Observable<ApiResponse<PagedResponse<DepartmentResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<DepartmentResponse>>>(`${BASE_URL}/companies/${companyId}/departments`, {
      params: { page, size },
    });
  }

  createDepartment(companyId: string, request: DepartmentRequest): Observable<ApiResponse<DepartmentResponse>> {
    return this.http.post<ApiResponse<DepartmentResponse>>(`${BASE_URL}/companies/${companyId}/departments`, request);
  }

  updateDepartment(departmentId: string, request: DepartmentRequest): Observable<ApiResponse<DepartmentResponse>> {
    return this.http.put<ApiResponse<DepartmentResponse>>(`${BASE_URL}/departments/${departmentId}`, request);
  }

  deleteDepartment(departmentId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/departments/${departmentId}`);
  }

  listOffices(companyId: string, page = 0, size = 100): Observable<ApiResponse<PagedResponse<OfficeResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<OfficeResponse>>>(`${BASE_URL}/companies/${companyId}/offices`, {
      params: { page, size },
    });
  }

  createOffice(companyId: string, request: OfficeRequest): Observable<ApiResponse<OfficeResponse>> {
    return this.http.post<ApiResponse<OfficeResponse>>(`${BASE_URL}/companies/${companyId}/offices`, request);
  }

  updateOffice(officeId: string, request: OfficeRequest): Observable<ApiResponse<OfficeResponse>> {
    return this.http.put<ApiResponse<OfficeResponse>>(`${BASE_URL}/offices/${officeId}`, request);
  }

  deleteOffice(officeId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/offices/${officeId}`);
  }

  listTeamInvitations(companyId: string, page = 0, size = 100): Observable<ApiResponse<PagedResponse<TeamInvitationResponse>>> {
    return this.http.get<ApiResponse<PagedResponse<TeamInvitationResponse>>>(`${BASE_URL}/companies/${companyId}/team-invitations`, {
      params: { page, size },
    });
  }

  createTeamInvitation(companyId: string, request: CreateTeamInvitationRequest): Observable<ApiResponse<TeamInvitationResponse>> {
    return this.http.post<ApiResponse<TeamInvitationResponse>>(`${BASE_URL}/companies/${companyId}/team-invitations`, request);
  }

  updateTeamInvitation(invitationId: string, request: UpdateTeamInvitationRequest): Observable<ApiResponse<TeamInvitationResponse>> {
    return this.http.put<ApiResponse<TeamInvitationResponse>>(`${BASE_URL}/team-invitations/${invitationId}`, request);
  }

  cancelTeamInvitation(invitationId: string): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`${BASE_URL}/team-invitations/${invitationId}`);
  }

  resendTeamInvitation(token: string): Observable<ApiResponse<TeamInvitationResponse>> {
    return this.http.post<ApiResponse<TeamInvitationResponse>>(`${BASE_URL}/team-invitations/${token}/resend`, {});
  }
}
