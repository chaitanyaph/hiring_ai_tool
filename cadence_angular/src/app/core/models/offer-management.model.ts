export enum OfferStatus {
  DRAFT = 'DRAFT',
  PENDING_APPROVAL = 'PENDING_APPROVAL',
  SENT = 'SENT',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
  WITHDRAWN = 'WITHDRAWN',
  EXPIRED = 'EXPIRED',
}

export enum SendMode {
  DRAFT = 'DRAFT',
  APPROVAL = 'APPROVAL',
  SEND = 'SEND',
}

export enum EmploymentType {
  FULL_TIME = 'FULL_TIME',
  CONTRACT = 'CONTRACT',
}

export enum ApprovalStatus {
  PENDING = 'PENDING',
  APPROVED = 'APPROVED',
  REJECTED = 'REJECTED',
}

export enum NegotiationStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  DECLINED = 'DECLINED',
}

export enum DeclineReason {
  OTHER_OFFER_ACCEPTED = 'OTHER_OFFER_ACCEPTED',
  COMPENSATION_MISMATCH = 'COMPENSATION_MISMATCH',
  ROLE_NOT_FIT = 'ROLE_NOT_FIT',
  PERSONAL_REASONS = 'PERSONAL_REASONS',
  OTHER = 'OTHER',
}

export interface CreateOrUpdateOfferRequest {
  applicationId: string;
  jobId: string;
  candidateId: string;
  department?: string;
  employmentType: EmploymentType;
  startDate: string;
  baseSalary: number;
  variableBonus?: number;
  esopEquity?: number;
  benefits?: string[];
  approverId?: string;
  expiryDate?: string;
  sendMode: SendMode;
}

export interface ApproveOfferRequest {
  approve: boolean;
  notes?: string;
}

export interface WithdrawOfferRequest {
  reason?: string;
}

export interface CandidateDeclineRequest {
  reason?: DeclineReason;
}

export interface CandidateNegotiationRequest {
  proposedCtc?: number;
  message?: string;
}

export interface ActivityLogResponse {
  eventType: string;
  actorId: string;
  occurredAt: string;
  details?: string;
}

export interface NegotiationResponse {
  id: string;
  proposedCtc?: number;
  message?: string;
  status: NegotiationStatus;
  recruiterNotes?: string;
  requestedAt: string;
  respondedAt?: string;
}

export interface OfferListItemResponse {
  id: string;
  candidateName: string;
  candidateEmail: string;
  jobTitle: string;
  totalCtc: number;
  status: OfferStatus;
  updatedAt: string;
}

export interface OfferDetailResponse {
  id: string;
  applicationId: string;
  candidateId: string;
  candidateName: string;
  candidateEmail: string;
  jobId: string;
  jobTitle: string;
  companyName: string;
  department?: string;
  employmentType: EmploymentType;
  startDate: string;
  baseSalary: number;
  variableBonus?: number;
  esopEquity?: number;
  totalCtc: number;
  benefits: string[];
  approverId?: string;
  approvalStatus: ApprovalStatus;
  approvalNotes?: string;
  expiryDate?: string;
  status: OfferStatus;
  documentGenerated: boolean;
  timeline: ActivityLogResponse[];
  negotiations: NegotiationResponse[];
}

export interface OfferDashboardStatsResponse {
  offersSent: number;
  acceptanceRatePercent: number;
  avgTimeToAcceptDays: number;
  pendingApprovalCount: number;
}

export interface CandidateOfferResponse {
  id: string;
  jobTitle: string;
  companyName: string;
  status: OfferStatus;
  baseSalary: number;
  variableBonus?: number;
  esopEquity?: number;
  totalCtc: number;
  startDate: string;
  expiryDate?: string;
  daysUntilExpiry?: number;
}
