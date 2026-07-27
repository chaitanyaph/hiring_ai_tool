/** Mirrors company-service's TeamRole enum exactly. */
export enum TeamRole {
  COMPANY_ADMIN = 'COMPANY_ADMIN',
  HR_MANAGER = 'HR_MANAGER',
  HR_RECRUITER = 'HR_RECRUITER',
  TECHNICAL_RECRUITER = 'TECHNICAL_RECRUITER',
  HIRING_MANAGER = 'HIRING_MANAGER',
  INTERVIEWER = 'INTERVIEWER',
}

/** Mirrors company-service's InvitationStatus enum. */
export enum InvitationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  EXPIRED = 'EXPIRED',
  CANCELLED = 'CANCELLED',
}

export interface CreateCompanyRequest {
  companyName: string;
  industry?: string;
  website?: string;
  companyEmail?: string;
  companyPhone?: string;
  headquarters?: string;
  description?: string;
  companyLogo?: string;
  subscriptionPlan?: string;
}

export type UpdateCompanyRequest = CreateCompanyRequest;

export interface CompanyResponse {
  id: string;
  companyName: string;
  companySlug: string;
  industry: string | null;
  website: string | null;
  companyEmail: string | null;
  companyPhone: string | null;
  headquarters: string | null;
  description: string | null;
  companyLogo: string | null;
  subscriptionPlan: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface DepartmentRequest {
  departmentName: string;
  description?: string;
}

export interface DepartmentResponse {
  id: string;
  companyId: string;
  departmentName: string;
  description: string | null;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface OfficeRequest {
  officeName: string;
  country?: string;
  state?: string;
  city?: string;
  address?: string;
  postalCode?: string;
  timezone?: string;
  latitude?: number;
  longitude?: number;
  isPrimaryOffice?: boolean;
}

export interface OfficeResponse {
  id: string;
  companyId: string;
  officeName: string;
  country: string | null;
  state: string | null;
  city: string | null;
  address: string | null;
  postalCode: string | null;
  timezone: string | null;
  latitude: number | null;
  longitude: number | null;
  isPrimaryOffice: boolean;
  status: string;
  createdAt: string;
  updatedAt: string;
}

export interface CreateTeamInvitationRequest {
  departmentId?: string;
  email: string;
  firstName?: string;
  lastName?: string;
  role: TeamRole;
}

export interface UpdateTeamInvitationRequest {
  departmentId?: string;
  role?: TeamRole;
}

export interface TeamInvitationResponse {
  id: string;
  companyId: string;
  departmentId: string | null;
  email: string;
  firstName: string | null;
  lastName: string | null;
  role: TeamRole;
  inviteToken: string;
  expiryDate: string;
  status: InvitationStatus;
  createdBy: string | null;
  acceptedAt: string | null;
  createdAt: string;
  updatedAt: string;
}
