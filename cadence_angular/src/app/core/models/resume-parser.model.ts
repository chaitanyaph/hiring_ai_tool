// Mirrors resume-parser-service's DTOs (dto/response) -- read-only recruiter-facing views, no request DTOs (nothing here is user-submitted).

export enum ParsingStatus {
  QUEUED = 'QUEUED',
  EXTRACTING_TEXT = 'EXTRACTING_TEXT',
  PARSING_FIELDS = 'PARSING_FIELDS',
  PARSED = 'PARSED',
  FAILED = 'FAILED',
}

export enum LogLevel {
  INFO = 'INFO',
  WARN = 'WARN',
  ERROR = 'ERROR',
}

export enum ResumeMatchStatus {
  AWAITING_RESUME = 'AWAITING_RESUME',
  AWAITING_PARSE = 'AWAITING_PARSE',
  ANALYZING = 'ANALYZING',
  ANALYZED = 'ANALYZED',
  FAILED = 'FAILED',
}

export enum ExperienceMatchLabel {
  STRONG = 'STRONG',
  ADEQUATE = 'ADEQUATE',
  BELOW_RANGE = 'BELOW_RANGE',
}

export enum EducationMatchLabel {
  MATCH = 'MATCH',
  PARTIAL = 'PARTIAL',
  MISMATCH = 'MISMATCH',
}

export enum CertificationMatchLabel {
  MATCH = 'MATCH',
  PARTIAL = 'PARTIAL',
  NONE = 'NONE',
}

export enum SkillCategory {
  TECHNICAL = 'TECHNICAL',
  SOFT = 'SOFT',
  PROGRAMMING_LANGUAGE = 'PROGRAMMING_LANGUAGE',
  FRAMEWORK = 'FRAMEWORK',
  LIBRARY = 'LIBRARY',
  DATABASE = 'DATABASE',
  CLOUD = 'CLOUD',
  DEVOPS = 'DEVOPS',
}

export enum NoteType {
  STRENGTH = 'STRENGTH',
  WEAKNESS = 'WEAKNESS',
}

export enum HiringRecommendation {
  PROCEED = 'PROCEED',
  HOLD = 'HOLD',
  REJECT = 'REJECT',
}

export interface ParsingQueueItemResponse {
  resumeId: string;
  candidateId: string;
  fullName?: string;
  email?: string;
  status: ParsingStatus;
  progressPercent?: number;
  submittedAt: string;
}

export interface ParsingQueueSummaryResponse {
  queuedCount: number;
  processingCount: number;
  parsedTodayCount: number;
  failedCount: number;
}

export interface SkillResponse {
  skillName: string;
  skillCategory: SkillCategory;
}

export interface ExperienceResponse {
  companyName: string;
  designation: string;
  startDate?: string;
  endDate?: string;
  current: boolean;
  description?: string;
}

export interface EducationResponse {
  institutionName: string;
  degree: string;
  fieldOfStudy?: string;
  startDate?: string;
  endDate?: string;
  grade?: string;
}

export interface ProjectResponse {
  projectName: string;
  description?: string;
  technologies?: string;
}

export interface CertificationResponse {
  certificationName: string;
  issuingOrganization?: string;
  issuedDate?: string;
  expiryDate?: string;
  credentialId?: string;
}

export interface ParsedResumeResponse {
  resumeId: string;
  candidateId: string;
  status: ParsingStatus;
  attemptCount: number;
  fullName?: string;
  email?: string;
  phone?: string;
  location?: string;
  linkedinUrl?: string;
  githubUrl?: string;
  portfolioUrl?: string;
  professionalSummary?: string;
  currentCompany?: string;
  currentDesignation?: string;
  totalExperienceYears?: number;
  noticePeriod?: string;
  expectedSalary?: string;
  failureReason?: string;
  parsedAt?: string;
  skills: SkillResponse[];
  experience: ExperienceResponse[];
  education: EducationResponse[];
  projects: ProjectResponse[];
  certifications: CertificationResponse[];
}

export interface ParsingStatusResponse {
  status: ParsingStatus;
  attemptCount: number;
  failureReason?: string;
}

export interface ParserLogResponse {
  logLevel: LogLevel;
  message: string;
  createdAt: string;
}

export interface ResumeMatchRankingItemResponse {
  applicationId: string;
  candidateId: string;
  resumeId: string;
  fullName?: string;
  email?: string;
  status: ResumeMatchStatus;
  overallMatchScore?: number;
  experienceMatchLabel?: ExperienceMatchLabel;
  analyzedAt?: string;
}

export interface ResumeMatchSummaryResponse {
  totalCount: number;
  analyzedCount: number;
  awaitingCount: number;
  failedCount: number;
  averageMatchScore?: number;
  topMatchScore?: number;
  belowThresholdCount: number;
}

export interface MatchedSkillResponse {
  skillName: string;
  skillCategory: SkillCategory;
}

export interface MissingSkillResponse {
  skillName: string;
  skillCategory: SkillCategory;
  required: boolean;
}

export interface StrengthWeaknessResponse {
  noteType: NoteType;
  description: string;
}

export interface AiRecommendationResponse {
  hiringRecommendation: HiringRecommendation;
  overallAiSummary: string;
  recommendedLearningTopics?: string;
}

export interface ResumeMatchResponse {
  id: string;
  applicationId: string;
  jobId: string;
  departmentId?: string;
  candidateId: string;
  resumeId: string;
  fullName?: string;
  email?: string;
  professionalSummary?: string;
  status: ResumeMatchStatus;
  attemptCount: number;
  overallMatchScore?: number;
  technicalSkillScore?: number;
  programmingLanguageScore?: number;
  frameworkScore?: number;
  databaseScore?: number;
  cloudScore?: number;
  devopsScore?: number;
  experienceMatchLabel?: ExperienceMatchLabel;
  projectMatchScore?: number;
  educationMatchLabel?: EducationMatchLabel;
  certificationMatchLabel?: CertificationMatchLabel;
  communicationPrediction?: number;
  problemSolvingPrediction?: number;
  learningAbilityPrediction?: number;
  matchedSkills: MatchedSkillResponse[];
  missingSkills: MissingSkillResponse[];
  strengths: StrengthWeaknessResponse[];
  weaknesses: StrengthWeaknessResponse[];
  aiRecommendation?: AiRecommendationResponse;
  failureReason?: string;
  analyzedAt?: string;
}

export interface CandidateAnalysisItemResponse {
  applicationId: string;
  jobId: string;
  status: ResumeMatchStatus;
  overallMatchScore?: number;
  analyzedAt?: string;
}
