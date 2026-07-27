export enum NotificationStatus {
  UNREAD = 'UNREAD',
  READ = 'READ',
  ARCHIVED = 'ARCHIVED',
}

export enum NotificationCategory {
  APPLICATION = 'APPLICATION',
  INTERVIEW = 'INTERVIEW',
  ASSESSMENT = 'ASSESSMENT',
  OFFER = 'OFFER',
  ACCOUNT = 'ACCOUNT',
  JOB = 'JOB',
  SYSTEM = 'SYSTEM',
}

export enum ColorTone {
  SUCCESS = 'SUCCESS',
  INFO = 'INFO',
  WARNING = 'WARNING',
  DANGER = 'DANGER',
}

export enum PreferenceCategory {
  APPLICATION_STATUS_UPDATES = 'APPLICATION_STATUS_UPDATES',
  INTERVIEW_REMINDERS = 'INTERVIEW_REMINDERS',
  RECOMMENDED_JOBS = 'RECOMMENDED_JOBS',
  MARKETING_EMAILS = 'MARKETING_EMAILS',
}

export enum EmailStatus {
  PENDING = 'PENDING',
  SENDING = 'SENDING',
  SENT = 'SENT',
  DELIVERED = 'DELIVERED',
  OPENED = 'OPENED',
  BOUNCED = 'BOUNCED',
  FAILED = 'FAILED',
  CANCELLED = 'CANCELLED',
}

export enum TemplateCategory {
  RECRUITER_INVITATION = 'RECRUITER_INVITATION',
  CANDIDATE_REGISTRATION = 'CANDIDATE_REGISTRATION',
  WELCOME_EMAIL = 'WELCOME_EMAIL',
  EMAIL_VERIFICATION = 'EMAIL_VERIFICATION',
  PASSWORD_RESET = 'PASSWORD_RESET',
  APPLICATION_RECEIVED = 'APPLICATION_RECEIVED',
  RESUME_UPLOADED = 'RESUME_UPLOADED',
  RESUME_SHORTLISTED = 'RESUME_SHORTLISTED',
  RESUME_REJECTED = 'RESUME_REJECTED',
  AI_INTERVIEW_INVITATION = 'AI_INTERVIEW_INVITATION',
  AI_INTERVIEW_REMINDER = 'AI_INTERVIEW_REMINDER',
  CODING_ASSESSMENT_INVITATION = 'CODING_ASSESSMENT_INVITATION',
  CODING_ASSESSMENT_REMINDER = 'CODING_ASSESSMENT_REMINDER',
  TECHNICAL_INTERVIEW_INVITATION = 'TECHNICAL_INTERVIEW_INVITATION',
  TECHNICAL_INTERVIEW_REMINDER = 'TECHNICAL_INTERVIEW_REMINDER',
  HR_INTERVIEW_INVITATION = 'HR_INTERVIEW_INVITATION',
  HR_INTERVIEW_REMINDER = 'HR_INTERVIEW_REMINDER',
  INTERVIEW_RESCHEDULED = 'INTERVIEW_RESCHEDULED',
  INTERVIEW_CANCELLED = 'INTERVIEW_CANCELLED',
  OFFER_LETTER = 'OFFER_LETTER',
  OFFER_ACCEPTED = 'OFFER_ACCEPTED',
  OFFER_REJECTED = 'OFFER_REJECTED',
  BACKGROUND_VERIFICATION = 'BACKGROUND_VERIFICATION',
}

export enum TriggerEvent {
  TEAM_INVITATION_CREATED = 'TEAM_INVITATION_CREATED',
  USER_REGISTERED = 'USER_REGISTERED',
  PASSWORD_RESET_REQUESTED = 'PASSWORD_RESET_REQUESTED',
  APPLICATION_SUBMITTED = 'APPLICATION_SUBMITTED',
  RESUME_UPLOADED = 'RESUME_UPLOADED',
  CANDIDATE_SHORTLISTED = 'CANDIDATE_SHORTLISTED',
  INTERVIEW_SCHEDULED = 'INTERVIEW_SCHEDULED',
  INTERVIEW_RESCHEDULED = 'INTERVIEW_RESCHEDULED',
  INTERVIEW_CANCELLED = 'INTERVIEW_CANCELLED',
  OFFER_ACCEPTED = 'OFFER_ACCEPTED',
  OFFER_REJECTED = 'OFFER_REJECTED',
  NONE = 'NONE',
}

export enum LogLevel {
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR',
}

export interface NotificationResponse {
  id: string;
  category: NotificationCategory;
  title: string;
  message: string;
  colorTone: ColorTone;
  entityType?: string;
  entityId?: string;
  status: NotificationStatus;
  readAt?: string;
  createdAt: string;
}

export interface PreferenceResponse {
  category: PreferenceCategory;
  enabled: boolean;
}

export interface UpdatePreferencesRequest {
  preferences: { category: PreferenceCategory; enabled: boolean }[];
}

export interface EmailQueueItemResponse {
  id: string;
  recipientEmail: string;
  recipientName?: string;
  subject: string;
  templateCategory: TemplateCategory;
  status: EmailStatus;
  attempts: number;
  scheduledAt?: string;
  sentAt?: string;
  deliveredAt?: string;
  openedAt?: string;
  failureReason?: string;
  createdAt: string;
}

export interface EmailQueueDetailResponse {
  id: string;
  recipientEmail: string;
  recipientName?: string;
  subject: string;
  bodyHtml: string;
  status: EmailStatus;
  sentAt?: string;
  deliveredAt?: string;
  openedAt?: string;
  failureReason?: string;
  attachments: { id: string; fileName: string; contentType: string; sizeBytes: number }[];
}

export interface EmailDashboardStatsResponse {
  sentToday: number;
  deliveryRatePercent: number;
  failedCount: number;
  scheduledUpcomingCount: number;
  sentByTemplateCategory: Record<string, number>;
}

export interface RetryBulkRequest {
  emailQueueIds: string[];
}

export interface TemplateResponse {
  id: string;
  name: string;
  triggerEvent: TriggerEvent;
  category: TemplateCategory;
  subject: string;
  bodyHtml: string;
  variablesHint?: string;
  active: boolean;
  createdAt: string;
  updatedAt: string;
}

export interface TemplatePreviewResponse {
  subject: string;
  bodyHtml: string;
  sampleRecipient: string;
}

export interface CreateTemplateRequest {
  name: string;
  triggerEvent: TriggerEvent;
  category: TemplateCategory;
  subject: string;
  bodyHtml: string;
  variablesHint?: string;
}

export interface UpdateTemplateRequest {
  name: string;
  subject: string;
  bodyHtml: string;
  variablesHint?: string;
  active: boolean;
}

export interface NotificationLogResponse {
  level: LogLevel;
  source: string;
  eventType: string;
  message: string;
  occurredAt: string;
}
