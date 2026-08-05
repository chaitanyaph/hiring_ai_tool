// Mirrors ai-interview-service's DTOs (dto/request, dto/response).

export enum InterviewSessionStatus {
  NOT_STARTED = 'NOT_STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  EXPIRED = 'EXPIRED',
}

export enum HiringRecommendation {
  PROCEED = 'PROCEED',
  HOLD = 'HOLD',
  REJECT = 'REJECT',
}

export enum ShortlistDecision {
  SHORTLISTED = 'SHORTLISTED',
  REJECTED = 'REJECTED',
  MANUAL_REVIEW = 'MANUAL_REVIEW',
}

export interface BulkApplicationIdsRequest {
  applicationIds: string[];
}

export interface AssignRecruiterRequest {
  applicationIds: string[];
  recruiterId: string;
}

export interface ShortlistItemResponse {
  applicationId: string;
  candidateId: string;
  fullName: string;
  email: string;
  jobId: string;
  jobTitle: string;
  overallMatchScore?: number;
  decision: ShortlistDecision;
  reason?: string;
  assignedRecruiterId?: string;
  decidedAt?: string;
}

export interface ShortlistSummaryResponse {
  shortlistedCount: number;
  rejectedCount: number;
  manualReviewCount: number;
  autoShortlistRatePercent: number;
}

export interface InterviewQueueItemResponse {
  applicationId: string;
  candidateId: string;
  fullName: string;
  email: string;
  jobId: string;
  jobTitle: string;
  status: InterviewSessionStatus;
  invitedAt?: string;
  startedAt?: string;
  completedAt?: string;
  expiresAt?: string;
}

export interface InterviewAnalysisSummaryResponse {
  completedCount: number;
  completedThisWeekCount: number;
  avgOverallScore?: number;
  avgCommunicationScore?: number;
  flaggedForReviewCount: number;
}

export interface InterviewCompletedItemResponse {
  applicationId: string;
  candidateId: string;
  fullName: string;
  jobId: string;
  jobTitle: string;
  overallScore?: number;
  hiringRecommendation: HiringRecommendation;
  completedAt: string;
}

export interface TranscriptTurnResponse {
  speaker: 'AI' | 'CANDIDATE';
  text: string;
}

// ---- Candidate-facing (the live interview flow) ----

export enum InterviewMode {
  CHAT = 'CHAT',
  VOICE = 'VOICE',
  VIDEO = 'VIDEO',
}

export enum QuestionCategory {
  INTRODUCTION = 'INTRODUCTION',
  RESUME = 'RESUME',
  JAVA = 'JAVA',
  SPRING_BOOT = 'SPRING_BOOT',
  MICROSERVICES = 'MICROSERVICES',
  SYSTEM_DESIGN = 'SYSTEM_DESIGN',
  SQL = 'SQL',
  BEHAVIORAL = 'BEHAVIORAL',
  HR = 'HR',
  SCENARIO_BASED = 'SCENARIO_BASED',
}

export interface StartInterviewRequest {
  applicationId: string;
  mode: InterviewMode;
}

export interface AnswerRequest {
  applicationId: string;
  questionId: string;
  answerText: string;
  responseTimeSeconds?: number;
}

export interface InterviewDetailsResponse {
  applicationId: string;
  jobTitle: string;
  status: InterviewSessionStatus;
  totalQuestions: number;
  estimatedDurationMinutes: number;
  modeOptions: string[];
  expiresAt?: string;
}

export interface InterviewQuestionResponse {
  questionId: string;
  questionOrder: number;
  totalQuestions: number;
  category: QuestionCategory;
  questionText: string;
  /** Base64 MP3, absent when server-side TTS is disabled/unavailable -- always fall back to text-only silently. */
  audioBase64?: string;
  interviewCompleted: boolean;
}

export interface InterviewResultResponse {
  status: InterviewSessionStatus;
  message?: string;
  transcript: TranscriptTurnResponse[];
}

export interface InterviewEvaluationReportResponse {
  applicationId: string;
  candidateId: string;
  fullName: string;
  jobId: string;
  jobTitle: string;

  overallScore?: number;
  communicationScore?: number;
  confidenceScore?: number;
  technicalAccuracyScore?: number;
  problemSolvingScore?: number;
  grammarScore?: number;
  behaviorScore?: number;
  leadershipScore?: number;
  domainKnowledgeScore?: number;

  eyeContactScore?: number;
  speakingPaceScore?: number;
  fillerWordCount?: number;
  avgResponseLatencySeconds?: number;

  strengths: string[];
  weaknesses: string[];
  improvementAreas: string[];
  hiringRecommendation: HiringRecommendation;
  interviewSummary?: string;
  recruiterSummary?: string;

  transcript: TranscriptTurnResponse[];
  completedAt: string;
}
