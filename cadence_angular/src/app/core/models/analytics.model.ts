export enum ReportPeriod {
  DAILY = 'DAILY',
  MONTHLY = 'MONTHLY',
  YEARLY = 'YEARLY',
}

export enum ReportFormat {
  CSV = 'CSV',
  EXCEL = 'EXCEL',
  PDF = 'PDF',
}

export interface FunnelStageResponse {
  stage: string;
  count: number;
  percentOfFirstStage?: number;
  conversionFromPreviousStage?: number;
}

export interface MonthlyPointResponse {
  monthLabel: string;
  value: number;
}

export interface LabeledValueResponse {
  label: string;
  value: number;
}

export interface RecruiterPerformanceResponse {
  recruiterId: string;
  recruiterName: string;
  openReqs: number;
  applicationsReviewed: number;
  hiresCount: number;
  avgTimeToHireDays: number;
  avgInterviewRating: number;
  avgOfferAcceptancePct: number;
}

export interface DashboardResponse {
  totalCompanies?: number;
  activeCompanies?: number;
  totalJobs: number;
  publishedJobs: number;
  closedJobs: number;
  candidatesRegistered: number;
  totalApplications: number;
  offersSent: number;
  offersAccepted: number;
  offersRejected: number;
  totalHires: number;
  offerAcceptanceRatePercent?: number;
  candidateDropoffRatePercent?: number;
  diversityRatioPercent?: number;
  avgTimeToHireDays?: number;
  funnel: FunnelStageResponse[];
  monthlyHiring: MonthlyPointResponse[];
  sourceBreakdown: LabeledValueResponse[];
  recruiterPerformance: RecruiterPerformanceResponse[];
}

export interface FunnelResponse {
  scope: string;
  stages: FunnelStageResponse[];
}

export interface JobAnalyticsResponse {
  totalJobs: number;
  publishedJobs: number;
  closedJobs: number;
}

export interface CandidateAnalyticsResponse {
  candidatesRegistered: number;
  totalApplications: number;
  shortlistedCount: number;
}

export interface ResumeAnalyticsResponse {
  resumesParsed: number;
  parseSuccessCount: number;
  parseFailureCount: number;
  avgMatchScore?: number;
}

export interface InterviewAnalyticsResponse {
  interviewsCompleted: number;
  interviewsCancelled: number;
  completionRatePercent?: number;
  avgAiInterviewScore?: number;
  avgTechnicalScore?: number;
  avgHrScore?: number;
}

export interface AssessmentAnalyticsResponse {
  assessmentsCompleted: number;
  avgScore?: number;
}

export interface OfferAnalyticsResponse {
  offersGenerated: number;
  offersSent: number;
  offersAccepted: number;
  offersRejected: number;
  negotiationRequestedCount: number;
  acceptanceRatePercent?: number;
  negotiationRatePercent?: number;
}

export interface ReportResponse {
  reportType: string;
  periodLabel: string;
  generatedAt: string;
  totalApplications: number;
  totalHires: number;
  offersSent: number;
  offersAccepted: number;
  offersRejected: number;
  offerAcceptanceRatePercent?: number;
  funnel: FunnelStageResponse[];
  hiringTrend: MonthlyPointResponse[];
  recruiterPerformance: RecruiterPerformanceResponse[];
}
