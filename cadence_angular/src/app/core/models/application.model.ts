// Mirrors application-service's DTOs (dto/request, dto/response).

export enum ApplicationStatus {
  APPLIED = 'APPLIED',
  RESUME_PARSING = 'RESUME_PARSING',
  RESUME_PARSED = 'RESUME_PARSED',
  AI_MATCHING = 'AI_MATCHING',
  AI_MATCHED = 'AI_MATCHED',
  SHORTLISTED = 'SHORTLISTED',
  AI_INTERVIEW_PENDING = 'AI_INTERVIEW_PENDING',
  AI_INTERVIEW_COMPLETED = 'AI_INTERVIEW_COMPLETED',
  CODING_ASSESSMENT_PENDING = 'CODING_ASSESSMENT_PENDING',
  CODING_ASSESSMENT_COMPLETED = 'CODING_ASSESSMENT_COMPLETED',
  TECHNICAL_INTERVIEW = 'TECHNICAL_INTERVIEW',
  MANAGER_INTERVIEW = 'MANAGER_INTERVIEW',
  HR_INTERVIEW = 'HR_INTERVIEW',
  BACKGROUND_VERIFICATION = 'BACKGROUND_VERIFICATION',
  OFFER_RELEASED = 'OFFER_RELEASED',
  OFFER_ACCEPTED = 'OFFER_ACCEPTED',
  OFFER_DECLINED = 'OFFER_DECLINED',
  HIRED = 'HIRED',
  REJECTED = 'REJECTED',
  WITHDRAWN = 'WITHDRAWN',
}

export enum ApplicationStage {
  APPLICATION = 'APPLICATION',
  AI_RESUME_SCREENING = 'AI_RESUME_SCREENING',
  AI_INTERVIEW = 'AI_INTERVIEW',
  CODING_ASSESSMENT = 'CODING_ASSESSMENT',
  TECHNICAL_INTERVIEW = 'TECHNICAL_INTERVIEW',
  MANAGER_INTERVIEW = 'MANAGER_INTERVIEW',
  HR_INTERVIEW = 'HR_INTERVIEW',
  BACKGROUND_VERIFICATION = 'BACKGROUND_VERIFICATION',
  OFFER = 'OFFER',
  HIRED = 'HIRED',
}

export enum Priority {
  LOW = 'LOW',
  MEDIUM = 'MEDIUM',
  HIGH = 'HIGH',
  URGENT = 'URGENT',
}

export interface ApplyRequest {
  jobId: string;
  resumeId: string;
}

export interface StatusChangeRequest {
  toStatus: ApplicationStatus;
  reason?: string;
}

export interface AssignRecruiterRequest {
  recruiterId: string;
}

export interface AssignHiringManagerRequest {
  hiringManagerId: string;
}

export interface AddNoteRequest {
  note: string;
}

export interface ApplicationSearchCriteria {
  jobId?: string;
  recruiterId?: string;
  hiringManagerId?: string;
  status?: ApplicationStatus;
  stage?: ApplicationStage;
  priority?: Priority;
  candidateName?: string;
  candidateEmail?: string;
  jobTitle?: string;
  minOverallScore?: number;
  appliedFrom?: string;
  appliedTo?: string;
}

export interface NoteResponse {
  id: string;
  authorId: string;
  note: string;
  createdAt: string;
}

export interface ApplicationResponse {
  id: string;
  companyId: string;
  jobId: string;
  candidateId: string;
  resumeId: string;
  assignedRecruiterId?: string;
  assignedHiringManagerId?: string;
  candidateNameSnapshot: string;
  candidateEmailSnapshot: string;
  jobTitleSnapshot: string;
  currentStatus: ApplicationStatus;
  currentStage: ApplicationStage;
  resumeMatchScore?: number;
  aiInterviewScore?: number;
  codingScore?: number;
  overallScore?: number;
  priority?: Priority;
  remarks?: string;
  appliedAt: string;
  lastStatusChangedAt: string;
  createdAt: string;
  updatedAt: string;
  version: number;
  /** Only populated on the single-application detail endpoint, not on list/search results. */
  notes?: NoteResponse[];
}

export interface TimelineEntryResponse {
  type: 'STATUS' | 'STAGE';
  from: string;
  to: string;
  changedBy?: string;
  changedAt: string;
  reason?: string;
}

export interface StatusHistoryResponse {
  fromStatus?: ApplicationStatus;
  toStatus: ApplicationStatus;
  changedBy?: string;
  changedAt: string;
  reason?: string;
}

export interface StageHistoryResponse {
  fromStage?: ApplicationStage;
  toStage: ApplicationStage;
  changedAt: string;
}

export interface ApplicationHistoryResponse {
  statusHistory: StatusHistoryResponse[];
  stageHistory: StageHistoryResponse[];
}
