// Mirrors candidate-service's DTOs exactly (dto/request, dto/response).

export interface BasicInfoRequest {
  fullName: string;
  headline?: string;
  phone?: string;
  location?: string;
  currentCompany?: string;
  noticePeriodDays?: number;
}

export interface EducationItemRequest {
  id?: string;
  degree: string;
  institution: string;
  startYear?: number;
  endYear?: number;
  grade?: string;
}

export interface UpdateEducationRequest {
  items: EducationItemRequest[];
}

export interface ExperienceItemRequest {
  id?: string;
  jobTitle: string;
  companyName: string;
  startDate?: string;
  endDate?: string;
  currentlyWorking: boolean;
  achievements?: string;
}

export interface UpdateExperienceRequest {
  items: ExperienceItemRequest[];
}

export interface UpdateSkillsRequest {
  skills: string[];
}

export interface ProjectItemRequest {
  id?: string;
  title: string;
  description?: string;
  projectUrl?: string;
}

export interface UpdateProjectsRequest {
  items: ProjectItemRequest[];
}

export interface CertificationItemRequest {
  id?: string;
  name: string;
  issuedBy?: string;
  issueDate?: string;
  credentialUrl?: string;
}

export interface UpdateCertificationsRequest {
  items: CertificationItemRequest[];
}

export interface UpdateLanguagesRequest {
  languages: string[];
}

export interface JobPreferencesRequest {
  preferredWorkType?: string;
  preferredEmploymentType?: string;
  expectedSalary?: number;
  salaryCurrency?: string;
  noticePeriod?: string;
  preferredLocations?: string;
}

export interface PortfolioRequest {
  websiteUrl?: string;
  linkedinUrl?: string;
  githubUrl?: string;
}

export interface SaveJobRequest {
  jobId: string;
}

// ---- Responses ----

export interface EducationResponse {
  id: string;
  degree: string;
  institution: string;
  startYear?: number;
  endYear?: number;
  grade?: string;
}

export interface ExperienceResponse {
  id: string;
  jobTitle: string;
  companyName: string;
  startDate?: string;
  endDate?: string;
  currentlyWorking: boolean;
  achievements?: string;
}

export interface ProjectResponse {
  id: string;
  title: string;
  description?: string;
  projectUrl?: string;
}

export interface CertificationResponse {
  id: string;
  name: string;
  issuedBy?: string;
  issueDate?: string;
  credentialUrl?: string;
}

export interface JobPreferencesResponse {
  preferredWorkType?: string;
  preferredEmploymentType?: string;
  expectedSalary?: number;
  salaryCurrency?: string;
  noticePeriod?: string;
  preferredLocations?: string;
}

export interface PortfolioResponse {
  websiteUrl?: string;
  linkedinUrl?: string;
  githubUrl?: string;
}

export interface CandidateProfileResponse {
  id: string;
  fullName: string;
  headline?: string;
  email: string;
  phone?: string;
  location?: string;
  currentCompany?: string;
  noticePeriodDays?: number;
  profilePhotoUrl?: string;
  resumeUrl?: string;
  resumeFilename?: string;
  resumeParsedAt?: string;
  aiResumeScore?: number;
  profileCompletionPercent?: number;
  status?: string;
  education: EducationResponse[];
  experience: ExperienceResponse[];
  skills: string[];
  projects: ProjectResponse[];
  certifications: CertificationResponse[];
  languages: string[];
  jobPreferences?: JobPreferencesResponse;
  portfolio?: PortfolioResponse;
  createdAt?: string;
  updatedAt?: string;
  version?: number;
}

export interface ResumeUploadResponse {
  resumeUrl: string;
  resumeFilename: string;
  uploadedAt: string;
  profileCompletionPercent?: number;
}

export interface SavedJobResponse {
  id: string;
  jobId: string;
  savedAt: string;
}

/** candidate-service's own local ApplicationResponse -- only used inside DashboardResponse.recentApplications. */
export interface CandidateLocalApplicationResponse {
  id: string;
  jobId: string;
  companyId: string;
  jobTitleSnapshot: string;
  companyNameSnapshot: string;
  locationSnapshot?: string;
  employmentTypeSnapshot?: string;
  status: string;
  matchScore?: number;
  appliedAt: string;
  withdrawnAt?: string;
}

/**
 * profileViews and upcomingInterviewsCount are placeholders (0) on the backend
 * until Notification/Interview Scheduling services feed them -- not faked here either.
 */
export interface DashboardResponse {
  profileCompletionPercent: number;
  aiResumeScore?: number;
  activeApplicationsCount: number;
  savedJobsCount: number;
  profileViews: number;
  upcomingInterviewsCount: number;
  recentApplications: CandidateLocalApplicationResponse[];
  savedJobs: SavedJobResponse[];
}
