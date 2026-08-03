export enum JobStatus {
  DRAFT = 'DRAFT',
  PUBLISHED = 'PUBLISHED',
  PAUSED = 'PAUSED',
  CLOSED = 'CLOSED',
  ARCHIVED = 'ARCHIVED',
  EXPIRED = 'EXPIRED',
}

export enum EmploymentType {
  FULL_TIME = 'FULL_TIME',
  PART_TIME = 'PART_TIME',
  CONTRACT = 'CONTRACT',
  INTERNSHIP = 'INTERNSHIP',
}

export enum WorkType {
  ON_SITE = 'ON_SITE',
  REMOTE = 'REMOTE',
  HYBRID = 'HYBRID',
}

export enum SkillType {
  REQUIRED = 'REQUIRED',
  PREFERRED = 'PREFERRED',
}

export interface JobBasicInfoRequest {
  title: string;
  departmentId?: string;
  numberOfOpenings?: number;
  location?: string;
  workType?: WorkType;
  employmentType?: EmploymentType;
  applicationDeadline?: string;
  descriptionHtml?: string;
}

export interface SkillRequest {
  skillName: string;
  skillType: SkillType;
}

export interface JobRequirementsRequest {
  minExperienceYears?: number;
  maxExperienceYears?: number;
  skills?: SkillRequest[];
  education?: string;
  certifications?: string;
  languages?: string;
  minSalary?: number;
  maxSalary?: number;
  salaryCurrency?: string;
  noticePeriodDays?: number;
  responsibilities?: string;
  benefits?: string[];
}

export interface PipelineStageRequest {
  id?: string;
  stageName: string;
  stageOrder: number;
  enabled: boolean;
}

export interface UpdatePipelineStagesRequest {
  stages: PipelineStageRequest[];
}

export interface StatusChangeRequest {
  reason?: string;
}

export interface AssignJobRequest {
  recruiterId?: string;
  hiringManagerId?: string;
}

export interface SaveTemplateRequest {
  templateName: string;
}

export interface SkillResponse {
  id: string;
  skillName: string;
  skillType: SkillType;
}

export interface PipelineStageResponse {
  id: string;
  stageName: string;
  stageOrder: number;
  enabled: boolean;
  systemDefault: boolean;
}

export interface JobRequirementsResponse {
  minExperienceYears: number | null;
  maxExperienceYears: number | null;
  skills: SkillResponse[];
  education: string | null;
  certifications: string | null;
  languages: string | null;
  minSalary: number | null;
  maxSalary: number | null;
  salaryCurrency: string | null;
  noticePeriodDays: number | null;
  responsibilities: string | null;
  benefits: string[];
}

export interface JobDetailResponse {
  id: string;
  jobCode: string;
  title: string;
  companyId: string;
  departmentId: string | null;
  location: string | null;
  workType: WorkType | null;
  employmentType: EmploymentType | null;
  numberOfOpenings: number | null;
  applicationDeadline: string | null;
  status: JobStatus;
  descriptionHtml: string | null;
  requirements: JobRequirementsResponse | null;
  pipelineStages: PipelineStageResponse[];
  recruiterId: string | null;
  hiringManagerId: string | null;
  publishedAt: string | null;
  closedAt: string | null;
  archivedAt: string | null;
  createdAt: string;
  updatedAt: string;
}

export interface JobSummaryResponse {
  id: string;
  jobCode: string;
  title: string;
  departmentId: string | null;
  departmentName: string | null;
  location: string | null;
  workType: WorkType | null;
  employmentType: EmploymentType | null;
  numberOfOpenings: number | null;
  applicantsCount: number;
  status: JobStatus;
  createdAt: string;
  publishedAt: string | null;
}

export interface JobCountsResponse {
  total: number;
  published: number;
  draft: number;
  archived: number;
  paused: number;
  closed: number;
  expired: number;
  distinctDepartments: number;
}

export interface JobDashboardResponse {
  totalJobs: number;
  publishedJobs: number;
  draftJobs: number;
  archivedJobs: number;
  recentlyCreated: JobSummaryResponse[];
  closingSoon: JobSummaryResponse[];
  applicationsCount: number;
}

export interface JobTemplateResponse {
  id: string;
  templateName: string;
  createdAt: string;
}

export interface JobSearchCriteria {
  title?: string;
  departmentId?: string;
  location?: string;
  status?: JobStatus;
  employmentType?: EmploymentType;
  recruiterId?: string;
  hiringManagerId?: string;
  createdFrom?: string;
  createdTo?: string;
}

export interface CandidateJobBrowseCriteria {
  title?: string;
  location?: string;
  workType?: WorkType;
}

export interface CandidateJobSummaryResponse {
  id: string;
  title: string;
  companyId: string;
  companyName: string | null;
  departmentName: string | null;
  location: string | null;
  workType: WorkType | null;
  employmentType: EmploymentType | null;
  skills: string[];
  minSalary: number | null;
  maxSalary: number | null;
  salaryCurrency: string | null;
  publishedAt: string | null;
}
