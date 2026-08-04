// Mirrors coding-assessment-service's DTOs (dto/request, dto/response).

export enum AssessmentType {
  CODING = 'CODING',
  MCQ = 'MCQ',
  COMBINED = 'COMBINED',
}

export enum AssessmentStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  CLOSED = 'CLOSED',
  ARCHIVED = 'ARCHIVED',
}

export enum Difficulty {
  EASY = 'EASY',
  MEDIUM = 'MEDIUM',
  HARD = 'HARD',
  MIXED = 'MIXED',
}

export enum ProgrammingLanguage {
  JAVA = 'JAVA',
  PYTHON = 'PYTHON',
  JAVASCRIPT = 'JAVASCRIPT',
  CPP = 'CPP',
  SQL = 'SQL',
}

export enum QuestionStatus {
  DRAFT = 'DRAFT',
  ACTIVE = 'ACTIVE',
  INACTIVE = 'INACTIVE',
  ARCHIVED = 'ARCHIVED',
}

export enum TestCaseVisibility {
  VISIBLE = 'VISIBLE',
  HIDDEN = 'HIDDEN',
}

export enum CandidateAssessmentStatus {
  NOT_STARTED = 'NOT_STARTED',
  IN_PROGRESS = 'IN_PROGRESS',
  COMPLETED = 'COMPLETED',
  EXPIRED = 'EXPIRED',
}

export enum SubmissionStatus {
  PENDING = 'PENDING',
  ACCEPTED = 'ACCEPTED',
  WRONG_ANSWER = 'WRONG_ANSWER',
  COMPILE_ERROR = 'COMPILE_ERROR',
  RUNTIME_ERROR = 'RUNTIME_ERROR',
  TIME_LIMIT_EXCEEDED = 'TIME_LIMIT_EXCEEDED',
  MEMORY_LIMIT_EXCEEDED = 'MEMORY_LIMIT_EXCEEDED',
}

export enum AntiCheatEventType {
  TAB_SWITCH = 'TAB_SWITCH',
  FULLSCREEN_EXIT = 'FULLSCREEN_EXIT',
  COPY = 'COPY',
  PASTE = 'PASTE',
}

// ---- Requests ----

export interface CreateAssessmentRequest {
  name: string;
  jobId: string;
  type: AssessmentType;
  allowedLanguages: ProgrammingLanguage[];
  difficulty: Difficulty;
  durationMinutes: number;
  questionCount: number;
  passingScorePercent: number;
  totalMarks: number;
  compilerVersion?: string;
  negativeMarking?: boolean;
  antiCheatMonitoring?: boolean;
  plagiarismDetection?: boolean;
  startDate?: string;
  expiryDate?: string;
  candidateInstructions?: string;
  publishNow?: boolean;
  questionIds?: string[];
}

export interface StartAssessmentRequest {
  preferredLanguage?: ProgrammingLanguage;
}

export interface RunCodeRequest {
  candidateAssessmentId: string;
  questionId: string;
  language: ProgrammingLanguage;
  code: string;
  customInput?: string;
}

export interface SubmitCodeRequest {
  candidateAssessmentId: string;
  questionId: string;
  language: ProgrammingLanguage;
  code: string;
}

export interface MarkForReviewRequest {
  questionId: string;
  marked: boolean;
}

export interface FinishAssessmentRequest {
  autoSubmitted?: boolean;
}

export interface AntiCheatEventRequest {
  eventType: AntiCheatEventType;
  metadata?: string;
}

// ---- Responses ----

export interface AssessmentListItemResponse {
  id: string;
  name: string;
  jobTitle: string;
  allowedLanguages: string[];
  durationMinutes: number;
  questionCount: number;
  status: AssessmentStatus;
  invitedCount: number;
}

export interface AssessmentResponse {
  id: string;
  companyId: string;
  jobId: string;
  jobTitle: string;
  name: string;
  type: AssessmentType;
  status: AssessmentStatus;
  difficulty: Difficulty;
  durationMinutes: number;
  questionCount: number;
  passingScorePercent: number;
  totalMarks: number;
  compilerVersion?: string;
  allowedLanguages: string[];
  negativeMarking: boolean;
  antiCheatMonitoring: boolean;
  plagiarismDetection: boolean;
  startDate?: string;
  expiryDate?: string;
  candidateInstructions?: string;
  createdAt: string;
}

export interface AssessmentDetailsResponse extends AssessmentResponse {
  invitedCandidates?: { applicationId: string; candidateName: string; status: CandidateAssessmentStatus }[];
}

export interface CodingQueueItemResponse {
  applicationId: string;
  candidateName: string;
  candidateEmail: string;
  jobTitle: string;
  status: CandidateAssessmentStatus;
  scorePercent?: number;
  passed?: boolean;
  dueOrCompletedAt?: string;
}

export interface CodingResultsSummaryResponse {
  completedCount: number;
  avgScore?: number;
  highestScore?: number;
  highestScoreCandidateName?: string;
  lowestScore?: number;
  lowestScoreCandidateName?: string;
}

export interface LeaderboardItemResponse {
  rank: number;
  applicationId: string;
  candidateName: string;
  jobTitle: string;
  score?: number;
  timeUsedSeconds?: number;
  completedAt?: string;
}

export interface SubmissionDrawerAntiCheatSignal {
  label: string;
  value: string;
}

export interface SubmissionDrawerTestCaseSummary {
  label: string;
  passed: boolean;
}

export interface SubmissionDrawerQuestionBlock {
  title: string;
  difficulty: string;
  code: string;
  testCases: SubmissionDrawerTestCaseSummary[];
}

export interface SubmissionDrawerResponse {
  applicationId: string;
  candidateName: string;
  jobTitle: string;
  scorePercent?: number;
  testCaseSummary?: string;
  timeUsedMinutes?: number;
  plagiarismBadge?: string;
  aiReviewBadge?: string;
  antiCheatSignals: SubmissionDrawerAntiCheatSignal[];
  questions: SubmissionDrawerQuestionBlock[];
}

export interface CodingAnalyticsDifficultyBreakdown {
  difficulty: string;
  successRatePercent: number;
}

export interface CodingAnalyticsLanguageUsage {
  language: string;
  usagePercent: number;
}

export interface CodingAnalyticsResponse {
  completionRatePercent?: number;
  avgSuccessRatePercent?: number;
  mostUsedLanguage?: string;
  avgTimeToSolveMinutes?: number;
  difficultyBreakdown: CodingAnalyticsDifficultyBreakdown[];
  languageUsage: CodingAnalyticsLanguageUsage[];
}

export interface AiCodeReviewResponse {
  timeComplexity?: string;
  spaceComplexity?: string;
  namingConventionNotes?: string;
  codeQualityScore?: number;
  solidPrinciplesNotes?: string;
  designPatternsNotes?: string;
  securityIssues?: string;
  optimizationSuggestions?: string;
  cleanCodeNotes?: string;
  overallRating?: number;
  badge?: string;
  strengths: string[];
  weaknesses: string[];
  suggestions: string[];
}

// ---- Candidate-facing ----

export interface CandidateAssessmentHistoryItemResponse {
  candidateAssessmentId: string;
  assessmentId: string;
  assessmentName: string;
  companyName: string;
  status: CandidateAssessmentStatus;
  score?: number;
  relevantDate?: string;
}

export interface CandidateAssessmentIntroResponse {
  candidateAssessmentId: string;
  jobTitle: string;
  companyName: string;
  durationMinutes: number;
  questionCount: number;
  allowedLanguages: string[];
  status: CandidateAssessmentStatus;
  candidateInstructions?: string;
}

export interface IdeVisibleTestCase {
  inputData: string;
  expectedOutput: string;
}

export interface IdeQuestionResponse {
  questionId: string;
  questionOrder: number;
  totalQuestions: number;
  difficulty: Difficulty;
  marks: number;
  hiddenTestCaseCount: number;
  title: string;
  description: string;
  exampleText?: string;
  constraintsText?: string;
  allowedLanguages: string[];
  starterCode?: string;
  visibleTestCases: IdeVisibleTestCase[];
  navigatorStatus: 'NOT_VISITED' | 'VISITED' | 'COMPLETED';
  markedForReview: boolean;
}

export interface RunCodeResponse {
  output?: string;
  stderr?: string;
  compileOutput?: string;
  runtimeMs?: number;
  memoryKb?: number;
}

export interface SubmitCodeTestCaseResult {
  visible: boolean;
  passed: boolean;
  inputData?: string;
  expectedOutput?: string;
  actualOutput?: string;
}

export interface SubmitCodeResponse {
  submissionId: string;
  status: SubmissionStatus;
  score?: number;
  testCasesPassed?: number;
  testCasesTotal?: number;
  runtimeMs?: number;
  memoryKb?: number;
  compileOutput?: string;
  testCaseResults: SubmitCodeTestCaseResult[];
}

export interface AssessmentResultResponse {
  candidateAssessmentId: string;
  scorePercent?: number;
  passed?: boolean;
  testCasesPassed?: number;
  testCasesTotal?: number;
  timeUsedMinutes?: number;
  autoSubmitted: boolean;
  submittedAt: string;
}

export interface SubmissionHistoryItemResponse {
  submissionId: string;
  questionOrder: number;
  submittedAt: string;
  language: string;
  status: SubmissionStatus;
  runtimeMs?: number;
  memoryKb?: number;
  testCasesPassed?: number;
  testCasesTotal?: number;
  score?: number;
}

// ---- Question Bank ----

export interface TestCaseItem {
  visibility: TestCaseVisibility;
  inputData?: string;
  expectedOutput: string;
  explanation?: string;
  weight?: number;
}

export interface StarterCodeItem {
  language: ProgrammingLanguage;
  code: string;
}

export interface CreateQuestionRequest {
  title: string;
  difficulty: Difficulty;
  marks: number;
  description: string;
  exampleText?: string;
  constraintsText?: string;
  inputFormat?: string;
  outputFormat?: string;
  explanation?: string;
  tags?: string[];
  topics?: string[];
  hints?: string[];
  timeLimitMs?: number;
  memoryLimitMb?: number;
  allowedLanguages: ProgrammingLanguage[];
  starterCodes?: StarterCodeItem[];
  testCases: TestCaseItem[];
  activateNow?: boolean;
}

export type UpdateQuestionRequest = Omit<CreateQuestionRequest, 'activateNow'>;

export interface QuestionTestCaseResponse {
  id: string;
  visibility: TestCaseVisibility;
  inputData?: string;
  expectedOutput: string;
  explanation?: string;
  weight: number;
  displayOrder: number;
}

export interface QuestionResponse {
  id: string;
  title: string;
  status: QuestionStatus;
  difficulty: Difficulty;
  marks: number;
  description: string;
  exampleText?: string;
  constraintsText?: string;
  inputFormat?: string;
  outputFormat?: string;
  explanation?: string;
  tags: string[];
  topics: string[];
  hints: string[];
  timeLimitMs: number;
  memoryLimitMb: number;
  allowedLanguages: string[];
  starterCodes: Record<string, string>;
  testCases: QuestionTestCaseResponse[];
  hiddenTestCaseCount: number;
  usedInAssessmentCount: number;
}

export interface CreateTestCaseRequest {
  visibility: TestCaseVisibility;
  inputData?: string;
  expectedOutput: string;
  explanation?: string;
  weight?: number;
}

export interface BulkImportTestCasesRequest {
  testCases: CreateTestCaseRequest[];
}

export interface ReorderTestCasesRequest {
  orderedTestCaseIds: string[];
}
