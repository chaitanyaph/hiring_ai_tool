import { ApplicationResponse, ApplicationStage, ApplicationStatus } from '../models/application.model';

const STAGE_LABEL: Record<ApplicationStage, string> = {
  [ApplicationStage.APPLICATION]: 'Applied',
  [ApplicationStage.AI_RESUME_SCREENING]: 'Resume screening',
  [ApplicationStage.AI_INTERVIEW]: 'AI Interview',
  [ApplicationStage.CODING_ASSESSMENT]: 'Coding assessment',
  [ApplicationStage.TECHNICAL_INTERVIEW]: 'Technical Round',
  [ApplicationStage.MANAGER_INTERVIEW]: 'Manager Round',
  [ApplicationStage.HR_INTERVIEW]: 'HR round',
  [ApplicationStage.BACKGROUND_VERIFICATION]: 'Background Verification',
  [ApplicationStage.OFFER]: 'Offer',
  [ApplicationStage.HIRED]: 'Hired',
};

export function applicationStageLabel(stage: ApplicationStage): string {
  return STAGE_LABEL[stage] ?? stage;
}

/**
 * The existing candidate-facing UI only has three status buckets
 * (active/offer/rejected) predating application-service's fuller 20-value
 * status enum -- everything still in progress buckets as 'active', any
 * offer-adjacent or hired status as 'offer', anything terminal-negative as
 * 'rejected'. This only drives which filter tab an application shows under;
 * the actual stage label (above) still reflects the real granular status.
 */
export function applicationStatusBucket(status: ApplicationStatus): 'active' | 'offer' | 'rejected' {
  switch (status) {
    case ApplicationStatus.REJECTED:
    case ApplicationStatus.WITHDRAWN:
    case ApplicationStatus.OFFER_DECLINED:
      return 'rejected';
    case ApplicationStatus.OFFER_RELEASED:
    case ApplicationStatus.OFFER_ACCEPTED:
    case ApplicationStatus.HIRED:
      return 'offer';
    default:
      return 'active';
  }
}

function relativeOrDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

export interface ApplicationRow {
  id: string;
  title: string;
  company: string;
  appliedDate: string;
  stage: string;
  status: 'active' | 'offer' | 'rejected';
}

/** company name isn't in ApplicationResponse (no companyNameSnapshot field) -- flagged as '—' rather than guessed. */
export function applicationResponseToRow(app: ApplicationResponse): ApplicationRow {
  return {
    id: app.id,
    title: app.jobTitleSnapshot,
    company: '—',
    appliedDate: relativeOrDate(app.appliedAt),
    stage: applicationStageLabel(app.currentStage),
    status: applicationStatusBucket(app.currentStatus),
  };
}

const STAGE_ORDER: ApplicationStage[] = [
  ApplicationStage.APPLICATION,
  ApplicationStage.AI_RESUME_SCREENING,
  ApplicationStage.AI_INTERVIEW,
  ApplicationStage.CODING_ASSESSMENT,
  ApplicationStage.TECHNICAL_INTERVIEW,
  ApplicationStage.MANAGER_INTERVIEW,
  ApplicationStage.HR_INTERVIEW,
  ApplicationStage.BACKGROUND_VERIFICATION,
  ApplicationStage.OFFER,
  ApplicationStage.HIRED,
];

/** -1/0/1 exactly like Array.sort comparators: whether `stage` is before/at/after the application's currentStage. */
export function compareToCurrentStage(app: ApplicationResponse, stage: ApplicationStage): -1 | 0 | 1 {
  const currentIdx = STAGE_ORDER.indexOf(app.currentStage);
  const targetIdx = STAGE_ORDER.indexOf(stage);
  if (targetIdx < currentIdx) return -1;
  if (targetIdx === currentIdx) return 0;
  return 1;
}

/**
 * Mirrors ApplicationStatus.java's own LINEAR_NEXT map exactly. Several of these
 * transitions (RESUME_PARSING..SHORTLISTED, and the *_PENDING -> *_COMPLETED pairs)
 * are meant to be recruiter-invisible, event-driven advances triggered by other
 * services (Resume Parser, AI Interview, Coding Assessment) rather than a manual
 * "Next round" click -- kept here anyway since canTransitionTo() on the backend is
 * the actual source of truth and will reject anything not legal for the caller.
 */
const LINEAR_NEXT: Partial<Record<ApplicationStatus, ApplicationStatus>> = {
  [ApplicationStatus.APPLIED]: ApplicationStatus.RESUME_PARSING,
  [ApplicationStatus.RESUME_PARSING]: ApplicationStatus.RESUME_PARSED,
  [ApplicationStatus.RESUME_PARSED]: ApplicationStatus.AI_MATCHING,
  [ApplicationStatus.AI_MATCHING]: ApplicationStatus.AI_MATCHED,
  [ApplicationStatus.AI_MATCHED]: ApplicationStatus.SHORTLISTED,
  [ApplicationStatus.SHORTLISTED]: ApplicationStatus.AI_INTERVIEW_PENDING,
  [ApplicationStatus.AI_INTERVIEW_PENDING]: ApplicationStatus.AI_INTERVIEW_COMPLETED,
  [ApplicationStatus.AI_INTERVIEW_COMPLETED]: ApplicationStatus.CODING_ASSESSMENT_PENDING,
  [ApplicationStatus.CODING_ASSESSMENT_PENDING]: ApplicationStatus.CODING_ASSESSMENT_COMPLETED,
  [ApplicationStatus.CODING_ASSESSMENT_COMPLETED]: ApplicationStatus.TECHNICAL_INTERVIEW,
  [ApplicationStatus.TECHNICAL_INTERVIEW]: ApplicationStatus.MANAGER_INTERVIEW,
  [ApplicationStatus.MANAGER_INTERVIEW]: ApplicationStatus.HR_INTERVIEW,
  [ApplicationStatus.HR_INTERVIEW]: ApplicationStatus.BACKGROUND_VERIFICATION,
  [ApplicationStatus.BACKGROUND_VERIFICATION]: ApplicationStatus.OFFER_RELEASED,
  [ApplicationStatus.OFFER_ACCEPTED]: ApplicationStatus.HIRED,
};

export function nextStatus(current: ApplicationStatus): ApplicationStatus | null {
  return LINEAR_NEXT[current] ?? null;
}
