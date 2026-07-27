export enum InterviewStatus {
  SCHEDULED = 'SCHEDULED',
  RESCHEDULED = 'RESCHEDULED',
  COMPLETED = 'COMPLETED',
  CANCELLED = 'CANCELLED',
}

export enum RoundType {
  TECHNICAL = 'TECHNICAL',
  MANAGER = 'MANAGER',
  ARCHITECT = 'ARCHITECT',
  HR = 'HR',
  CUSTOM = 'CUSTOM',
}

export enum InterviewMode {
  ONLINE = 'ONLINE',
  OFFLINE = 'OFFLINE',
  HYBRID = 'HYBRID',
}

export enum RecommendationType {
  PROCEED = 'PROCEED',
  HOLD = 'HOLD',
  REJECT = 'REJECT',
}

export enum DecisionType {
  MOVE_TO_HR = 'MOVE_TO_HR',
  NEXT_ROUND = 'NEXT_ROUND',
  SELECT = 'SELECT',
  REJECT = 'REJECT',
  HOLD = 'HOLD',
  REQUEST_ANOTHER_INTERVIEW = 'REQUEST_ANOTHER_INTERVIEW',
}

export enum TimelineStage {
  AI_INTERVIEW = 'AI_INTERVIEW',
  CODING_ASSESSMENT = 'CODING_ASSESSMENT',
  TECHNICAL_INTERVIEW = 'TECHNICAL_INTERVIEW',
  MANAGER_INTERVIEW = 'MANAGER_INTERVIEW',
  HR_INTERVIEW = 'HR_INTERVIEW',
}

export enum TimelineStatus {
  PENDING = 'PENDING',
  IN_PROGRESS = 'IN_PROGRESS',
  DONE = 'DONE',
  SKIPPED = 'SKIPPED',
}

export interface ScheduleInterviewRequest {
  applicationId: string;
  jobId: string;
  candidateId: string;
  interviewRoundId?: string;
  roundType: RoundType;
  scheduledDate: string;
  scheduledTime: string;
  durationMinutes: number;
  panelistIds: string[];
  autoGenerateMeetLink?: boolean;
  notifyCandidateByEmail?: boolean;
  notesForPanel?: string;
}

export interface RescheduleInterviewRequest {
  scheduledDate: string;
  scheduledTime: string;
  durationMinutes: number;
  rescheduleReason?: string;
}

export interface CancelInterviewRequest {
  cancelReason?: string;
}

export interface SubmitFeedbackRequest {
  communicationScore: number;
  technicalScore: number;
  cultureFitScore: number;
  codingSkillsScore?: number;
  problemSolvingScore?: number;
  systemDesignScore?: number;
  leadershipScore?: number;
  overallRating?: number;
  strengths?: string;
  weaknesses?: string;
  comments?: string;
  recommendation: RecommendationType;
}

export interface RecruiterDecisionRequest {
  decisionType: DecisionType;
  notes?: string;
}

export interface RequestRescheduleRequest {
  reason?: string;
}

export interface CreateInterviewRoundRequest {
  name: string;
  type: RoundType;
  description?: string;
}

export interface UpdateInterviewRoundRequest {
  name: string;
  description?: string;
  active: boolean;
  roundOrder?: number;
}

export interface PanelistResponse {
  interviewerId: string;
  interviewerRole?: string;
  feedbackSubmitted: boolean;
}

export interface InterviewListItemResponse {
  id: string;
  applicationId: string;
  candidateId: string;
  candidateName: string;
  jobId: string;
  jobTitle: string;
  roundType: RoundType;
  status: InterviewStatus;
  scheduledDate: string;
  scheduledTime: string;
  durationMinutes: number;
  panelists: PanelistResponse[];
  feedbackSubmitted: boolean;
}

export interface InterviewDetailResponse {
  id: string;
  applicationId: string;
  candidateId: string;
  candidateName: string;
  jobId: string;
  jobTitle: string;
  companyName: string;
  roundType: RoundType;
  status: InterviewStatus;
  scheduledDate: string;
  scheduledTime: string;
  durationMinutes: number;
  mode: InterviewMode;
  meetingLink?: string;
  panelists: PanelistResponse[];
  notesForPanel?: string;
  cancelReason?: string;
  rescheduleReason?: string;
  feedbackSubmittable: boolean;
}

export interface InterviewFeedbackResponse {
  id: string;
  interviewId: string;
  interviewerId: string;
  interviewerName: string;
  communicationScore?: number;
  technicalScore?: number;
  cultureFitScore?: number;
  codingSkillsScore?: number;
  problemSolvingScore?: number;
  systemDesignScore?: number;
  leadershipScore?: number;
  overallRating?: number;
  strengths?: string;
  weaknesses?: string;
  comments?: string;
  recommendation: RecommendationType;
  submittedAt: string;
}

export interface ActivityLogResponse {
  eventType: string;
  actorId: string;
  occurredAt: string;
  details?: string;
}

export interface CandidateTimelineResponse {
  stage: TimelineStage;
  status: TimelineStatus;
  occurredAt?: string;
  score?: number;
  note?: string;
}

export interface InterviewRoundResponse {
  id: string;
  companyId: string;
  name: string;
  type: RoundType;
  roundOrder: number;
  description?: string;
  active: boolean;
}

export interface CandidateInterviewResponse {
  id: string;
  jobTitle: string;
  companyName: string;
  roundType: RoundType;
  status: InterviewStatus;
  scheduledDate: string;
  scheduledTime: string;
  mode: InterviewMode;
  meetingLink?: string;
  interviewerNames: string[];
  upcoming: boolean;
}
