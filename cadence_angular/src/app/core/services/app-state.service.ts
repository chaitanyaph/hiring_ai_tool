import { Injectable, inject, signal, computed } from '@angular/core';
import { UserModel, Job, Candidate, ChatMessage } from '../models/models';
import {
  CompanyResponse,
  CreateTeamInvitationRequest,
  DepartmentRequest,
  DepartmentResponse,
  OfficeRequest,
  OfficeResponse,
  TeamInvitationResponse,
  UpdateCompanyRequest,
} from '../models/company.model';
import { CompanyService } from './company.service';
import { TokenStorageService } from './token-storage.service';
import { JobService } from './job.service';
import { jobSummaryToJob } from '../utils/job.mapper';
import {
  EmploymentType,
  JobStatus,
  PipelineStageRequest,
  StatusChangeRequest,
  WorkType,
} from '../models/job.model';
import { map, Observable, switchMap, tap } from 'rxjs';
import { CandidateService } from './candidate.service';
import { ResumeService } from './resume.service';
import { ResumeResponse } from '../models/resume.model';
import { ApplicationService } from './application.service';
import { ApplicationResponse, ApplicationSearchCriteria, ApplicationStatus } from '../models/application.model';
import { ResumeParserService } from './resume-parser.service';
import { AiInterviewService } from './ai-interview.service';
import { CodingAssessmentService } from './coding-assessment.service';
import {
  AntiCheatEventRequest,
  AssessmentDetailsResponse,
  AssessmentListItemResponse,
  AssessmentResponse,
  AssessmentStatus,
  CandidateAssessmentHistoryItemResponse,
  CandidateAssessmentIntroResponse,
  CandidateAssessmentStatus,
  CodingAnalyticsResponse,
  CodingQueueItemResponse,
  CodingResultsSummaryResponse,
  CreateAssessmentRequest,
  FinishAssessmentRequest,
  IdeQuestionResponse,
  LeaderboardItemResponse,
  MarkForReviewRequest,
  RunCodeRequest,
  StartAssessmentRequest,
  SubmissionDrawerResponse,
  SubmitCodeRequest,
} from '../models/coding-assessment.model';
import {
  AnswerRequest,
  AssignRecruiterRequest,
  InterviewAnalysisSummaryResponse,
  InterviewCompletedItemResponse,
  InterviewDetailsResponse,
  InterviewEvaluationReportResponse,
  InterviewQueueItemResponse,
  InterviewQuestionResponse,
  InterviewResultResponse,
  InterviewSessionStatus,
  ShortlistItemResponse,
  ShortlistSummaryResponse,
  StartInterviewRequest,
} from '../models/ai-interview.model';
import {
  ParsedResumeResponse,
  ParserLogResponse,
  ParsingQueueItemResponse,
  ParsingQueueSummaryResponse,
  ParsingStatus,
  ResumeMatchRankingItemResponse,
  ResumeMatchResponse,
  ResumeMatchSummaryResponse,
} from '../models/resume-parser.model';
import {
  BasicInfoRequest,
  CandidateProfileResponse,
  CertificationItemRequest,
  DashboardResponse,
  EducationItemRequest,
  ExperienceItemRequest,
  JobPreferencesRequest,
  PortfolioRequest,
  ProjectItemRequest,
} from '../models/candidate.model';
import { InterviewManagementService } from './interview-management.service';
import {
  ActivityLogResponse,
  CandidateInterviewResponse,
  CandidateTimelineResponse,
  CancelInterviewRequest,
  InterviewDetailResponse,
  InterviewFeedbackResponse,
  InterviewListItemResponse,
  InterviewStatus as RecruiterInterviewStatus,
  RecruiterDecisionRequest,
  RequestRescheduleRequest,
  RescheduleInterviewRequest,
  ScheduleInterviewRequest,
  SubmitFeedbackRequest,
} from '../models/interview-management.model';
import { NotificationService } from './notification.service';
import {
  EmailQueueItemResponse,
  EmailStatus,
  NotificationResponse,
  NotificationStatus,
  PreferenceCategory,
  PreferenceResponse,
  TemplateResponse,
  UpdateTemplateRequest,
} from '../models/notification.model';
import { AnalyticsService } from './analytics.service';
import { DashboardResponse as AnalyticsDashboardResponse, FunnelResponse, ReportFormat, ReportPeriod } from '../models/analytics.model';
import { OfferManagementService } from './offer-management.service';
import {
  CandidateDeclineRequest,
  CandidateNegotiationRequest,
  CandidateOfferResponse,
  CreateOrUpdateOfferRequest,
  OfferDashboardStatsResponse,
  OfferDetailResponse,
  OfferListItemResponse,
  OfferStatus,
} from '../models/offer-management.model';

@Injectable({
  providedIn: 'root'
})
export class AppStateService {
  private companyService = inject(CompanyService);
  private tokenStorage = inject(TokenStorageService);
  private jobService = inject(JobService);
  private candidateService = inject(CandidateService);
  private resumeService = inject(ResumeService);
  private applicationService = inject(ApplicationService);
  private resumeParserService = inject(ResumeParserService);
  private aiInterviewService = inject(AiInterviewService);
  private codingAssessmentService = inject(CodingAssessmentService);
  private interviewManagementService = inject(InterviewManagementService);
  private notificationService = inject(NotificationService);
  private analyticsService = inject(AnalyticsService);
  private offerManagementService = inject(OfferManagementService);

  // Signals State
  currentUser = signal<UserModel | null>(null);
  toastMessage = signal<string | null>(null);

  jobs = signal<Job[]>([]);
  candidates = signal<Candidate[]>([]);

  recruiterChatHistory = signal<ChatMessage[]>([]);
  candidateChatHistory = signal<ChatMessage[]>([]);

  selectedCandidate = signal<Candidate | null>(null);
  selectedBrowseJob = signal<Job | null>(null);

  viewingBrowseJobDetail = signal<boolean>(false);
  viewingApplicationDetail = signal<boolean>(false);

  // Notification module state (notification-service) -- real backend data, not mock.
  candidateNotifications = signal<NotificationResponse[]>([]);
  unreadNotificationCount = signal<number>(0);
  notificationPreferences = signal<PreferenceResponse[]>([]);
  emailTemplates = signal<TemplateResponse[]>([]);
  emailHistory = signal<EmailQueueItemResponse[]>([]);
  scheduledNotifications = signal<EmailQueueItemResponse[]>([]);
  failedNotifications = signal<EmailQueueItemResponse[]>([]);

  // Company module state (company-service) -- real backend data, not mock.
  company = signal<CompanyResponse | null>(null);
  departments = signal<DepartmentResponse[]>([]);
  offices = signal<OfficeResponse[]>([]);
  teamInvitations = signal<TeamInvitationResponse[]>([]);

  // Candidate module state (candidate-service) -- real backend data, not mock.
  candidateProfile = signal<CandidateProfileResponse | null>(null);
  candidateDashboard = signal<DashboardResponse | null>(null);
  savedJobIds = signal<Set<string>>(new Set());

  // Resume module state (resume-service) -- real backend data, not mock.
  candidateResumes = signal<ResumeResponse[]>([]);

  // Application module state (application-service) -- real backend data, not mock.
  // candidateApplications: the signed-in candidate's own applications ("My Applications").
  // companyApplications / selectedApplication: the recruiter's company-wide pipeline view.
  candidateApplications = signal<ApplicationResponse[]>([]);
  companyApplications = signal<ApplicationResponse[]>([]);
  selectedApplication = signal<ApplicationResponse | null>(null);

  // Resume Parser module state (resume-parser-service) -- real backend data, not mock.
  parsingQueue = signal<ParsingQueueItemResponse[]>([]);
  parsingQueueSummary = signal<ParsingQueueSummaryResponse | null>(null);
  parsedResumeDetail = signal<ParsedResumeResponse | null>(null);
  parsingLogs = signal<ParserLogResponse[]>([]);
  matchRanking = signal<ResumeMatchRankingItemResponse[]>([]);
  matchSummary = signal<ResumeMatchSummaryResponse | null>(null);
  matchAnalysisDetail = signal<ResumeMatchResponse | null>(null);
  recommendations = signal<ResumeMatchRankingItemResponse[]>([]);

  // AI Interview module state (ai-interview-service) -- real backend data, not mock.
  aiShortlisted = signal<ShortlistItemResponse[]>([]);
  aiRejected = signal<ShortlistItemResponse[]>([]);
  aiManualReview = signal<ShortlistItemResponse[]>([]);
  aiShortlistSummary = signal<ShortlistSummaryResponse | null>(null);
  aiInterviewQueue = signal<InterviewQueueItemResponse[]>([]);
  aiInterviewAnalysisSummary = signal<InterviewAnalysisSummaryResponse | null>(null);
  aiCompletedInterviews = signal<InterviewCompletedItemResponse[]>([]);
  aiInterviewReport = signal<InterviewEvaluationReportResponse | null>(null);

  // Candidate-facing live AI interview flow (ai-interview-service) -- real backend data, not mock.
  candidateInterviewDetails = signal<InterviewDetailsResponse | null>(null);
  candidateInterviewQuestion = signal<InterviewQuestionResponse | null>(null);
  candidateInterviewResult = signal<InterviewResultResponse | null>(null);

  // Coding Assessment module state (coding-assessment-service) -- real backend data, not mock.
  codingAssessmentsList = signal<AssessmentListItemResponse[]>([]);
  codingAssessmentQueue = signal<CodingQueueItemResponse[]>([]);
  codingResultsSummary = signal<CodingResultsSummaryResponse | null>(null);
  codingLeaderboard = signal<LeaderboardItemResponse[]>([]);
  codingCompletedList = signal<LeaderboardItemResponse[]>([]);
  codingSubmissionDrawer = signal<SubmissionDrawerResponse | null>(null);
  codingAnalytics = signal<CodingAnalyticsResponse | null>(null);
  candidateAssessmentHistory = signal<CandidateAssessmentHistoryItemResponse[]>([]);
  candidateAssessmentIntro = signal<CandidateAssessmentIntroResponse | null>(null);

  // Analytics module state (analytics-service) -- real backend data, not mock.
  analyticsDashboard = signal<AnalyticsDashboardResponse | null>(null);
  analyticsFunnel = signal<FunnelResponse | null>(null);

  // Offer Management module state (offer-management-service) -- real backend data, not mock.
  offersList = signal<OfferListItemResponse[]>([]);
  offersLoading = signal<boolean>(false);
  offerDetail = signal<OfferDetailResponse | null>(null);
  offerStats = signal<OfferDashboardStatsResponse | null>(null);
  myOffers = signal<CandidateOfferResponse[]>([]);
  myOffersLoading = signal<boolean>(false);

  // Interview Management module state (interview-management-service) -- real backend data, not mock.
  interviews = signal<InterviewListItemResponse[]>([]);
  interviewDetail = signal<InterviewDetailResponse | null>(null);
  interviewFeedbackList = signal<InterviewFeedbackResponse[]>([]);
  interviewActivityLog = signal<ActivityLogResponse[]>([]);
  applicationTimeline = signal<CandidateTimelineResponse[]>([]);
  myInterviews = signal<CandidateInterviewResponse[]>([]);
  myInterviewsLoading = signal<boolean>(false);
  myInterviewDetail = signal<CandidateInterviewResponse | null>(null);

  // Global modal system signal
  activeModal = signal<string | null>(null);

  constructor() {
    this.resetData();
  }

  private get companyId(): string | null {
    return this.tokenStorage.getUser()?.companyId ?? null;
  }

  currentUserId(): string | null {
    return this.tokenStorage.getUser()?.id ?? null;
  }

  // ---- Company module actions ----

  loadCompany() {
    const companyId = this.companyId;
    if (!companyId) return;
    this.companyService.getCompany(companyId).subscribe({
      next: (res) => this.company.set(res.data),
      error: () => this.showToast('Could not load company details.'),
    });
  }

  updateCompanyDetails(request: UpdateCompanyRequest) {
    const companyId = this.companyId;
    if (!companyId) return;
    this.companyService.updateCompany(companyId, request).subscribe({
      next: (res) => {
        this.company.set(res.data);
        this.showToast('Company details updated');
      },
      error: () => this.showToast('Could not update company details.'),
    });
  }

  loadDepartments() {
    const companyId = this.companyId;
    if (!companyId) return;
    this.companyService.listDepartments(companyId).subscribe({
      next: (res) => this.departments.set(res.data.content),
      error: () => this.showToast('Could not load departments.'),
    });
  }

  addDepartment(request: DepartmentRequest) {
    const companyId = this.companyId;
    if (!companyId) return;
    this.companyService.createDepartment(companyId, request).subscribe({
      next: (res) => {
        this.departments.update((list) => [...list, res.data]);
        this.showToast('Department added');
        this.closeModal();
      },
      error: () => this.showToast('Could not add that department.'),
    });
  }

  removeDepartment(departmentId: string) {
    this.companyService.deleteDepartment(departmentId).subscribe({
      next: () => {
        this.departments.update((list) => list.filter((d) => d.id !== departmentId));
        this.showToast('Department removed');
      },
      error: () => this.showToast('Could not remove that department.'),
    });
  }

  loadOffices() {
    const companyId = this.companyId;
    if (!companyId) return;
    this.companyService.listOffices(companyId).subscribe({
      next: (res) => this.offices.set(res.data.content),
      error: () => this.showToast('Could not load offices.'),
    });
  }

  addOffice(request: OfficeRequest) {
    const companyId = this.companyId;
    if (!companyId) return;
    this.companyService.createOffice(companyId, request).subscribe({
      next: (res) => {
        this.offices.update((list) => [...list, res.data]);
        this.showToast('Office added');
        this.closeModal();
      },
      error: () => this.showToast('Could not add that office.'),
    });
  }

  removeOffice(officeId: string) {
    this.companyService.deleteOffice(officeId).subscribe({
      next: () => {
        this.offices.update((list) => list.filter((o) => o.id !== officeId));
        this.showToast('Office removed');
      },
      error: () => this.showToast('Could not remove that office.'),
    });
  }

  loadTeamInvitations() {
    const companyId = this.companyId;
    if (!companyId) return;
    this.companyService.listTeamInvitations(companyId).subscribe({
      next: (res) => this.teamInvitations.set(res.data.content),
      error: () => this.showToast('Could not load team invitations.'),
    });
  }

  inviteTeammate(request: CreateTeamInvitationRequest) {
    const companyId = this.companyId;
    if (!companyId) return;
    this.companyService.createTeamInvitation(companyId, request).subscribe({
      next: (res) => {
        this.teamInvitations.update((list) => [...list, res.data]);
        this.showToast(`Teammate invite sent to ${request.email}`);
        this.closeModal();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not send that invite.'),
    });
  }

  openModal(name: string) {
    this.activeModal.set(name);
  }

  closeModal() {
    this.activeModal.set(null);
  }

  // Toast Helper
  showToast(msg: string) {
    this.toastMessage.set(msg);
    setTimeout(() => {
      if (this.toastMessage() === msg) {
        this.toastMessage.set(null);
      }
    }, 2400);
  }

  logout() {
    this.currentUser.set(null);
    this.selectedCandidate.set(null);
    this.selectedBrowseJob.set(null);
    this.selectedApplication.set(null);
    this.viewingBrowseJobDetail.set(false);
    this.viewingApplicationDetail.set(false);
    this.showToast('You have been logged out');
  }

  // ---- Jobs actions (job-service) ----

  loadJobs() {
    this.jobService.search({}).subscribe({
      next: (res) => this.jobs.set(res.data.content.map(jobSummaryToJob)),
      error: () => this.showToast('Could not load jobs.'),
    });
  }

  private static readonly WORK_TYPE_MAP: Record<string, WorkType> = {
    Hybrid: WorkType.HYBRID,
    Remote: WorkType.REMOTE,
    'On-site': WorkType.ON_SITE,
  };

  private static readonly EMPLOYMENT_TYPE_MAP: Record<string, EmploymentType> = {
    'Full-time': EmploymentType.FULL_TIME,
    'Part-time': EmploymentType.PART_TIME,
    Contract: EmploymentType.CONTRACT,
    Internship: EmploymentType.INTERNSHIP,
  };

  /** "3-6 years" -> [3,6]; "5+ years" -> [5, undefined]. */
  private parseExperienceRange(range: string): [number | undefined, number | undefined] {
    const plusMatch = range.match(/^(\d+)\+/);
    if (plusMatch) return [Number(plusMatch[1]), undefined];
    const rangeMatch = range.match(/^(\d+)-(\d+)/);
    if (rangeMatch) return [Number(rangeMatch[1]), Number(rangeMatch[2])];
    return [undefined, undefined];
  }

  /** "30 days" -> 30; "Any" -> undefined. */
  private parseNoticePeriod(label: string): number | undefined {
    const match = label.match(/^(\d+)/);
    return match ? Number(match[1]) : undefined;
  }

  /**
   * Backs the job creation wizard's final "Continue"/"Publish job" step. The
   * wizard collects everything locally across 4 UI steps then submits once,
   * same as before -- this just chains the calls job-service's own 3-step
   * incremental save actually requires (create draft -> requirements ->
   * pipeline stages -> optional publish) behind that single existing action.
   */
  createJobFromWizard(params: {
    title: string;
    departmentName: string;
    description: string;
    workType: string;
    location: string;
    empType: string;
    openings: number;
    experienceRange: string;
    salaryMin: number;
    salaryMax: number;
    noticePeriod: string;
    deadline: string;
    stages: { name: string; enabled: boolean }[];
    publish: boolean;
  }) {
    const dept = this.departments().find((d) => d.departmentName === params.departmentName);
    const [minExperienceYears, maxExperienceYears] = this.parseExperienceRange(params.experienceRange);

    this.jobService
      .createDraft({
        title: params.title,
        departmentId: dept?.id,
        numberOfOpenings: params.openings,
        location: params.location,
        workType: AppStateService.WORK_TYPE_MAP[params.workType],
        employmentType: AppStateService.EMPLOYMENT_TYPE_MAP[params.empType],
        applicationDeadline: params.deadline || undefined,
        descriptionHtml: params.description,
      })
      .pipe(
        switchMap((created) =>
          this.jobService.updateRequirements(created.data.id, {
            minExperienceYears,
            maxExperienceYears,
            minSalary: params.salaryMin,
            maxSalary: params.salaryMax,
            salaryCurrency: 'INR',
            noticePeriodDays: this.parseNoticePeriod(params.noticePeriod),
          })
        ),
        switchMap((updated) => {
          const stages: PipelineStageRequest[] = params.stages.map((s, i) => ({
            stageName: s.name,
            stageOrder: i + 1,
            enabled: s.enabled,
          }));
          return this.jobService.updatePipelineStages(updated.data.id, { stages });
        }),
        switchMap((updated) => (params.publish ? this.jobService.publish(updated.data.id) : [updated]))
      )
      .subscribe({
        next: () => {
          this.loadJobs();
          this.showToast(params.publish ? 'Job published' : 'Job saved as draft');
          this.closeModal();
        },
        error: (err) => this.showToast(err?.error?.message ?? 'Could not save this job.'),
      });
  }

  private transitionJob(jobId: string, action: 'publish' | 'archive' | 'restore', successMessage: string, request?: StatusChangeRequest) {
    const call =
      action === 'publish'
        ? this.jobService.publish(jobId)
        : action === 'archive'
          ? this.jobService.archive(jobId, request)
          : this.jobService.restore(jobId);

    call.subscribe({
      next: (res) => {
        this.jobs.update((list) => list.map((j) => (j.id === jobId ? { ...j, status: res.data.status.toLowerCase() } : j)));
        this.showToast(successMessage);
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not update that job.'),
    });
  }

  publishJob(jobId: string) {
    this.transitionJob(jobId, 'publish', 'Job published');
  }

  archiveJob(jobId: string, reason?: string) {
    this.transitionJob(jobId, 'archive', 'Job archived', reason ? { reason } : undefined);
  }

  restoreJob(jobId: string) {
    this.transitionJob(jobId, 'restore', 'Job restored to draft');
  }

  // ---- Candidate module actions (candidate-service) ----
  // Each wizard-step method returns its Observable (rather than subscribing
  // internally) so CandidateLayoutComponent's wizard can advance to the next
  // step only once the call actually succeeds, and stay put + surface the
  // error otherwise -- matching the existing wizardNext()/wizardBack() flow.

  loadCandidateProfile() {
    this.candidateService.getProfile().subscribe({
      next: (res) => this.candidateProfile.set(res.data),
      error: () => this.showToast('Could not load your profile.'),
    });
  }

  saveCandidateBasicInfo(request: BasicInfoRequest) {
    return this.candidateService.updateBasicInfo(request).pipe(tap((res) => this.candidateProfile.set(res.data)));
  }

  uploadCandidateResume(file: File) {
    return this.candidateService.uploadResume(file).pipe(
      tap((res) =>
        this.candidateProfile.update((profile) =>
          profile
            ? {
                ...profile,
                resumeUrl: res.data.resumeUrl,
                resumeFilename: res.data.resumeFilename,
                profileCompletionPercent: res.data.profileCompletionPercent ?? profile.profileCompletionPercent,
              }
            : profile
        )
      )
    );
  }

  saveCandidateEducation(items: EducationItemRequest[]) {
    return this.candidateService.updateEducation({ items }).pipe(tap((res) => this.candidateProfile.set(res.data)));
  }

  saveCandidateExperience(items: ExperienceItemRequest[]) {
    return this.candidateService.updateExperience({ items }).pipe(tap((res) => this.candidateProfile.set(res.data)));
  }

  saveCandidateSkills(skills: string[]) {
    return this.candidateService.updateSkills({ skills }).pipe(tap((res) => this.candidateProfile.set(res.data)));
  }

  saveCandidateProjects(items: ProjectItemRequest[]) {
    return this.candidateService.updateProjects({ items }).pipe(tap((res) => this.candidateProfile.set(res.data)));
  }

  saveCandidateCertifications(items: CertificationItemRequest[]) {
    return this.candidateService
      .updateCertifications({ items })
      .pipe(tap((res) => this.candidateProfile.set(res.data)));
  }

  saveCandidateLanguages(languages: string[]) {
    return this.candidateService.updateLanguages({ languages }).pipe(tap((res) => this.candidateProfile.set(res.data)));
  }

  saveCandidateJobPreferences(request: JobPreferencesRequest) {
    return this.candidateService.updateJobPreferences(request).pipe(tap((res) => this.candidateProfile.set(res.data)));
  }

  saveCandidatePortfolio(request: PortfolioRequest) {
    return this.candidateService.updatePortfolio(request).pipe(
      tap((res) => {
        this.candidateProfile.set(res.data);
        this.showToast('Profile wizard completed — dashboard updated');
        this.closeModal();
      })
    );
  }

  loadCandidateDashboard() {
    this.candidateService.getDashboard().subscribe({
      next: (res) => this.candidateDashboard.set(res.data),
      error: () => this.showToast('Could not load your dashboard.'),
    });
  }

  loadSavedJobIds() {
    this.candidateService.listSavedJobs().subscribe({
      next: (res) => this.savedJobIds.set(new Set(res.data.map((s) => s.jobId))),
      error: () => {
        /* Non-fatal: browse-jobs just renders with nothing pre-marked as saved. */
      },
    });
  }

  toggleSaveJob(jobId: string, currentlySaved: boolean) {
    const call: Observable<unknown> = currentlySaved
      ? this.candidateService.unsaveJob(jobId)
      : this.candidateService.saveJob(jobId);
    call.subscribe({
      next: () => {
        this.savedJobIds.update((ids) => {
          const next = new Set(ids);
          currentlySaved ? next.delete(jobId) : next.add(jobId);
          return next;
        });
        this.showToast(currentlySaved ? 'Job removed from saved list' : 'Job saved to your list');
      },
      error: () => this.showToast('Could not update saved jobs.'),
    });
  }

  // Candidates actions
  selectCandidate(candidate: Candidate) {
    this.selectedCandidate.set(candidate);
  }

  clearSelectedCandidate() {
    this.selectedCandidate.set(null);
  }

  updateRecruiterNote(candId: string, note: string) {
    this.candidates.update(list => list.map(c => {
      if (c.id === candId) {
        const updated = { ...c, recruiterNote: note };
        if (this.selectedCandidate()?.id === candId) {
          this.selectedCandidate.set(updated);
        }
        return updated;
      }
      return c;
    }));
    this.showToast('Note saved');
  }

  rejectCandidate(candId: string) {
    this.candidates.update(list => list.map(c => {
      if (c.id === candId) {
        const updated = { ...c, stage: 'Consider' }; // fallback/archive stage
        if (this.selectedCandidate()?.id === candId) {
          this.selectedCandidate.set(updated);
        }
        return updated;
      }
      return c;
    }));
    this.showToast('Candidate moved to Consider list');
  }

  advanceCandidate(candId: string, nextStage: string) {
    this.candidates.update(list => list.map(c => {
      if (c.id === candId) {
        const updated = { ...c, stage: nextStage };
        if (this.selectedCandidate()?.id === candId) {
          this.selectedCandidate.set(updated);
        }
        return updated;
      }
      return c;
    }));
    this.showToast(`Candidate moved to ${nextStage}`);
  }

  // ---- Interview Management module actions (interview-management-service) ----

  loadInterviews(status?: RecruiterInterviewStatus) {
    this.interviewManagementService.list(status).subscribe({
      next: (res) => this.interviews.set(res.data.content),
      error: () => this.showToast('Could not load interviews.'),
    });
  }

  scheduleRecruiterInterview(request: ScheduleInterviewRequest) {
    this.interviewManagementService.schedule(request).subscribe({
      next: () => {
        this.showToast('Interview scheduled -- invite sent');
        this.loadInterviews();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not schedule this interview.'),
    });
  }

  rescheduleInterview(id: string, request: RescheduleInterviewRequest) {
    this.interviewManagementService.reschedule(id, request).subscribe({
      next: () => {
        this.showToast('Interview rescheduled');
        this.loadInterviews();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not reschedule this interview.'),
    });
  }

  cancelInterview(id: string, request?: CancelInterviewRequest) {
    this.interviewManagementService.cancel(id, request).subscribe({
      next: () => {
        this.showToast('Interview cancelled');
        this.loadInterviews();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not cancel this interview.'),
    });
  }

  loadInterviewDetail(id: string) {
    this.interviewManagementService.getDetail(id).subscribe({
      next: (res) => this.interviewDetail.set(res.data),
      error: () => this.showToast('Could not load this interview.'),
    });
  }

  clearInterviewDetail() {
    this.interviewDetail.set(null);
  }

  loadInterviewActivity(id: string) {
    this.interviewManagementService.getActivity(id).subscribe({
      next: (res) => this.interviewActivityLog.set(res.data),
      error: () => this.showToast('Could not load the interview activity log.'),
    });
  }

  submitInterviewFeedback(id: string, request: SubmitFeedbackRequest) {
    this.interviewManagementService.submitFeedback(id, request).subscribe({
      next: () => {
        this.showToast('Feedback submitted');
        this.loadInterviewFeedback(id);
        this.loadInterviews();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not submit feedback.'),
    });
  }

  loadInterviewFeedback(id: string) {
    this.interviewManagementService.getFeedback(id).subscribe({
      next: (res) => this.interviewFeedbackList.set(res.data),
      error: () => this.showToast('Could not load feedback for this interview.'),
    });
  }

  recordInterviewDecision(id: string, request: RecruiterDecisionRequest) {
    this.interviewManagementService.decide(id, request).subscribe({
      next: () => this.showToast('Decision recorded'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not record this decision.'),
    });
  }

  loadApplicationTimeline(applicationId: string) {
    this.interviewManagementService.getTimeline(applicationId).subscribe({
      next: (res) => this.applicationTimeline.set(res.data),
      error: () => this.showToast('Could not load the hiring pipeline timeline.'),
    });
  }

  /** InterviewListItemResponse doesn't carry the meeting link (only the detail endpoint does), so "Join" fetches detail first. */
  joinInterview(id: string) {
    this.interviewManagementService.getDetail(id).subscribe({
      next: (res) => {
        if (res.data.meetingLink) {
          window.open(res.data.meetingLink, '_blank');
        } else {
          this.showToast('No meeting link is available for this interview yet.');
        }
      },
      error: () => this.showToast('Could not open this interview.'),
    });
  }

  // ---- Candidate-facing: My Interviews (interview-management-service) ----

  loadMyInterviews(upcomingOnly = false) {
    this.myInterviewsLoading.set(true);
    this.interviewManagementService.listMyInterviews(upcomingOnly).subscribe({
      next: (res) => {
        this.myInterviews.set(res.data);
        this.myInterviewsLoading.set(false);
      },
      error: () => {
        this.showToast('Could not load your interviews.');
        this.myInterviewsLoading.set(false);
      },
    });
  }

  loadMyInterviewDetail(id: string) {
    this.interviewManagementService.getMyInterviewDetail(id).subscribe({
      next: (res) => this.myInterviewDetail.set(res.data),
      error: () => this.showToast('Could not load this interview.'),
    });
  }

  clearMyInterviewDetail() {
    this.myInterviewDetail.set(null);
  }

  requestMyInterviewReschedule(id: string, request?: RequestRescheduleRequest) {
    this.interviewManagementService.requestReschedule(id, request).subscribe({
      next: () => this.showToast('Reschedule request sent'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not send the reschedule request.'),
    });
  }

  /** interview-management-service has no meeting link on the list item, only on getMyInterviewDetail -- so "Join" fetches detail first, same pattern as the recruiter's joinInterview(). */
  joinMyInterview(id: string) {
    this.interviewManagementService.getMyInterviewDetail(id).subscribe({
      next: (res) => {
        if (res.data.meetingLink) {
          window.open(res.data.meetingLink, '_blank');
        } else {
          this.showToast('No meeting link is available for this interview yet.');
        }
      },
      error: () => this.showToast('Could not open this interview.'),
    });
  }

  // ---- Notification module actions (notification-service) ----

  loadNotifications(status?: NotificationStatus) {
    this.notificationService.list(status).subscribe({
      next: (res) => this.candidateNotifications.set(res.data.content),
      error: () => this.showToast('Could not load notifications.'),
    });
  }

  loadUnreadNotificationCount() {
    this.notificationService.unreadCount().subscribe({
      next: (res) => this.unreadNotificationCount.set(res.data),
      error: () => {},
    });
  }

  markNotificationRead(id: string) {
    this.notificationService.markRead(id).subscribe({
      next: () => {
        this.loadNotifications();
        this.loadUnreadNotificationCount();
      },
      error: () => this.showToast('Could not mark this notification as read.'),
    });
  }

  markAllNotificationsRead() {
    this.notificationService.markAllRead().subscribe({
      next: () => {
        this.showToast('All notifications marked as read');
        this.loadNotifications();
        this.loadUnreadNotificationCount();
      },
      error: () => this.showToast('Could not mark all notifications as read.'),
    });
  }

  archiveNotification(id: string) {
    this.notificationService.archive(id).subscribe({
      next: () => {
        this.showToast('Notification archived');
        this.loadNotifications();
      },
      error: () => this.showToast('Could not archive this notification.'),
    });
  }

  deleteNotification(id: string) {
    this.notificationService.delete(id).subscribe({
      next: () => {
        this.showToast('Notification deleted');
        this.loadNotifications();
      },
      error: () => this.showToast('Could not delete this notification.'),
    });
  }

  loadNotificationPreferences() {
    this.notificationService.getPreferences().subscribe({
      next: (res) => this.notificationPreferences.set(res.data),
      error: () => this.showToast('Could not load your notification preferences.'),
    });
  }

  updateNotificationPreferences(preferences: { category: PreferenceCategory; enabled: boolean }[]) {
    this.notificationService.updatePreferences({ preferences }).subscribe({
      next: (res) => {
        this.notificationPreferences.set(res.data);
        this.showToast('Notification preferences updated');
      },
      error: () => this.showToast('Could not update your notification preferences.'),
    });
  }

  loadEmailTemplates() {
    this.notificationService.listTemplates().subscribe({
      next: (res) => this.emailTemplates.set(res.data),
      error: () => this.showToast('Could not load email templates.'),
    });
  }

  updateEmailTemplate(id: string, request: UpdateTemplateRequest) {
    this.notificationService.updateTemplate(id, request).subscribe({
      next: () => {
        this.showToast('Template changes saved successfully');
        this.loadEmailTemplates();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not save this template.'),
    });
  }

  loadEmailHistory() {
    this.notificationService.listEmailHistory().subscribe({
      next: (res) => this.emailHistory.set(res.data.content),
      error: () => this.showToast('Could not load email history.'),
    });
  }

  loadScheduledEmails() {
    this.notificationService.listEmailQueue(EmailStatus.PENDING).subscribe({
      next: (res) => this.scheduledNotifications.set(res.data.content),
      error: () => this.showToast('Could not load scheduled emails.'),
    });
  }

  loadFailedEmails() {
    this.notificationService.listEmailQueue(EmailStatus.FAILED).subscribe({
      next: (res) => this.failedNotifications.set(res.data.content),
      error: () => this.showToast('Could not load failed emails.'),
    });
  }

  retryEmail(id: string) {
    this.notificationService.retryEmail(id).subscribe({
      next: () => {
        this.showToast('Retrying email delivery…');
        this.loadFailedEmails();
        this.loadEmailHistory();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not retry this email.'),
    });
  }

  cancelScheduledEmail(id: string) {
    this.notificationService.cancelScheduled(id).subscribe({
      next: () => {
        this.showToast('Scheduled notification cancelled');
        this.loadScheduledEmails();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not cancel this scheduled email.'),
    });
  }

  // ---- Analytics module actions (analytics-service) ----

  loadAnalyticsDashboard() {
    this.analyticsService.getRecruiterDashboard().subscribe({
      next: (res) => this.analyticsDashboard.set(res.data),
      error: () => this.showToast('Could not load analytics.'),
    });
  }

  loadAnalyticsFunnel() {
    this.analyticsService.getFunnel().subscribe({
      next: (res) => this.analyticsFunnel.set(res.data),
      error: () => this.showToast('Could not load the hiring funnel.'),
    });
  }

  exportAnalyticsReport() {
    this.analyticsService.exportReport(ReportFormat.CSV, ReportPeriod.MONTHLY).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = 'analytics-report.csv';
        a.click();
        window.URL.revokeObjectURL(url);
        this.showToast('Report exported');
      },
      error: () => this.showToast('Could not export the report.'),
    });
  }

  // ---- Offer Management module actions (offer-management-service) ----

  loadOffers(status?: OfferStatus) {
    this.offersLoading.set(true);
    this.offerManagementService.list(status).subscribe({
      next: (res) => {
        this.offersList.set(res.data.content);
        this.offersLoading.set(false);
      },
      error: () => {
        this.showToast('Could not load offers.');
        this.offersLoading.set(false);
      },
    });
  }

  loadOfferStats() {
    this.offerManagementService.stats().subscribe({
      next: (res) => this.offerStats.set(res.data),
      error: () => {},
    });
  }

  loadOfferDetail(offerId: string) {
    this.offerManagementService.getDetail(offerId).subscribe({
      next: (res) => this.offerDetail.set(res.data),
      error: () => this.showToast('Could not load this offer.'),
    });
  }

  clearOfferDetail() {
    this.offerDetail.set(null);
  }

  createOffer(request: CreateOrUpdateOfferRequest) {
    this.offerManagementService.create(request).subscribe({
      next: (res) => {
        this.showToast(res.message ?? 'Offer saved');
        this.closeModal();
        this.loadOffers();
        this.loadOfferStats();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not create this offer.'),
    });
  }

  deleteDraftOffer(offerId: string) {
    this.offerManagementService.deleteDraft(offerId).subscribe({
      next: () => {
        this.showToast('Draft offer deleted');
        this.loadOffers();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not delete this draft.'),
    });
  }

  approveOffer(offerId: string, approve: boolean, notes?: string) {
    this.offerManagementService.approve(offerId, { approve, notes }).subscribe({
      next: () => {
        this.showToast(approve ? 'Offer approved and sent to candidate' : 'Offer approval rejected');
        this.clearOfferDetail();
        this.loadOffers();
        this.loadOfferStats();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not record this decision.'),
    });
  }

  sendOffer(offerId: string) {
    this.offerManagementService.send(offerId).subscribe({
      next: () => {
        this.showToast('Offer sent to candidate');
        this.clearOfferDetail();
        this.loadOffers();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not send this offer.'),
    });
  }

  withdrawOffer(offerId: string, reason?: string) {
    this.offerManagementService.withdraw(offerId, { reason }).subscribe({
      next: () => {
        this.showToast('Offer withdrawn');
        this.clearOfferDetail();
        this.loadOffers();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not withdraw this offer.'),
    });
  }

  downloadOfferLetter(offerId: string) {
    this.offerManagementService.generateDocument(offerId).subscribe({
      next: (blob) => this.triggerBlobDownload(blob, 'offer-letter.pdf'),
      error: () => this.showToast('Could not generate the offer letter.'),
    });
  }

  // ---- Candidate-facing: My Offers (offer-management-service) ----

  loadMyOffers() {
    this.myOffersLoading.set(true);
    this.offerManagementService.listMyOffers().subscribe({
      next: (res) => {
        this.myOffers.set(res.data);
        this.myOffersLoading.set(false);
      },
      error: () => {
        this.showToast('Could not load your offers.');
        this.myOffersLoading.set(false);
      },
    });
  }

  downloadMyOfferLetter(offerId: string) {
    this.offerManagementService.downloadMyOffer(offerId).subscribe({
      next: (blob) => this.triggerBlobDownload(blob, 'offer-letter.pdf'),
      error: () => this.showToast('Could not download your offer letter.'),
    });
  }

  acceptMyOffer(offerId: string) {
    this.offerManagementService.acceptMyOffer(offerId).subscribe({
      next: (res) => {
        this.myOffers.update((list) => list.map((o) => (o.id === offerId ? res.data : o)));
        this.showToast('Offer accepted');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not accept this offer.'),
    });
  }

  rejectMyOffer(offerId: string, request?: CandidateDeclineRequest) {
    this.offerManagementService.rejectMyOffer(offerId, request).subscribe({
      next: (res) => {
        this.myOffers.update((list) => list.map((o) => (o.id === offerId ? res.data : o)));
        this.showToast('Offer declined');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not decline this offer.'),
    });
  }

  requestMyOfferNegotiation(offerId: string, request?: CandidateNegotiationRequest) {
    this.offerManagementService.requestNegotiation(offerId, request).subscribe({
      next: () => this.showToast('Negotiation request sent'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not send the negotiation request.'),
    });
  }

  // Chat Actions
  sendRecruiterChatMessage(text: string) {
    const userMsg: ChatMessage = { id: `rm-${Date.now()}-u`, sender: 'user', text };
    this.recruiterChatHistory.update(list => [...list, userMsg]);

    setTimeout(() => {
      let aiResponse: ChatMessage;
      if (text.toLowerCase().includes('java')) {
        aiResponse = {
          id: `rm-${Date.now()}-ai`,
          sender: 'ai',
          text: 'Found 8 candidates matching all four skills, sorted by match score:',
          aiResults: [
            { name: 'Arjun Verma', score: '94%', skills: 'Java, Kafka, Redis' },
            { name: 'Neha Kapoor', score: '89%', skills: 'Java, AWS' },
            { name: 'Karthik Iyer', score: '86%', skills: 'Kafka, Docker' }
          ]
        };
      } else {
        aiResponse = {
          id: `rm-${Date.now()}-ai`,
          sender: 'ai',
          text: `Here is the current hiring pipeline stats: We have ${this.jobs().length} open requirements and ${this.candidates().length} candidates currently active.`
        };
      }
      this.recruiterChatHistory.update(list => [...list, aiResponse]);
    }, 1000);
  }

  sendCandidateChatMessage(text: string) {
    const userMsg: ChatMessage = { id: `cc-${Date.now()}-u`, sender: 'user', text };
    this.candidateChatHistory.update(list => [...list, userMsg]);

    setTimeout(() => {
      const aiResponse: ChatMessage = {
        id: `cc-${Date.now()}-ai`,
        sender: 'ai',
        text: 'Practice explaining your projects out loud in under 2 minutes, keep answers structured (situation → action → result), and expect 1-2 live coding questions on data structures.'
      };
      this.candidateChatHistory.update(list => [...list, aiResponse]);
    }, 1000);
  }

  // Helper mock data restorer
  resetData() {
    this.candidates.set([
      {
        id: 'cand-1',
        name: 'Arjun Verma',
        email: 'arjun.verma@email.com',
        phone: '+91 98765 43210',
        currentCompany: 'Infosys',
        matchScore: 94,
        stage: 'AI Shortlisted',
        sourcedFrom: 'LinkedIn',
        appliedDate: '2d ago',
        noticePeriod: '15 days',
        expectedSalary: '₹24 LPA',
        recruiterNote: 'Strong Java backend skills. Completed assessment test with 94% match. Notice period is short.'
      },
      {
        id: 'cand-2',
        name: 'Priya Kulkarni',
        email: 'priya.k@email.com',
        phone: '+91 99887 76655',
        currentCompany: 'Wipro',
        matchScore: 88,
        stage: 'Interview Pending',
        sourcedFrom: 'Careers page',
        appliedDate: '3d ago',
        noticePeriod: '60 days',
        expectedSalary: '₹20 LPA',
        recruiterNote: 'Solid fundamental logic. Scheduled for AI Interview. High communication score.'
      },
      {
        id: 'cand-3',
        name: 'Rohan Mehta',
        email: 'rohan.m@email.com',
        phone: '+91 98888 77777',
        currentCompany: 'Capgemini',
        matchScore: 91,
        stage: 'Technical Round',
        sourcedFrom: 'Referral',
        appliedDate: '5d ago',
        noticePeriod: '30 days',
        expectedSalary: '₹28 LPA',
        recruiterNote: 'Referred by Vikram Shah. Good cloud scaling logic. Completed System Design round.'
      },
      {
        id: 'cand-4',
        name: 'Sneha Iyer',
        email: 'sneha.iyer@email.com',
        phone: '+91 97777 66666',
        currentCompany: 'Cognizant',
        matchScore: 79,
        stage: 'HR Round',
        sourcedFrom: 'LinkedIn',
        appliedDate: '6d ago',
        noticePeriod: '90 days',
        expectedSalary: '₹14 LPA',
        recruiterNote: 'Frontend React/Angular designer. Outgoing and conversational.'
      },
      {
        id: 'cand-5',
        name: 'Neha Kapoor',
        email: 'neha.k@email.com',
        phone: '+91 96666 55555',
        currentCompany: 'Accenture',
        matchScore: 89,
        stage: 'AI Shortlisted',
        sourcedFrom: 'Naukri',
        appliedDate: '1d ago',
        noticePeriod: '45 days',
        expectedSalary: '₹22 LPA',
        recruiterNote: 'Backend developer with AWS and Spring fundamentals.'
      },
      {
        id: 'cand-6',
        name: 'Karthik Iyer',
        email: 'karthik.i@email.com',
        phone: '+91 98xxxxx987',
        currentCompany: 'TCS',
        matchScore: 86,
        stage: 'Consider',
        sourcedFrom: 'CSV import',
        appliedDate: '4d ago',
        noticePeriod: '30 days',
        expectedSalary: '₹18 LPA',
        recruiterNote: 'Solid backend foundations. Notice period matches requirement.'
      }
    ]);

    this.recruiterChatHistory.set([
      { id: 'rm-1', sender: 'user', text: 'Show Java developers with Spring Boot, Kafka and Redis' },
      {
        id: 'rm-2',
        sender: 'ai',
        text: 'Found 8 candidates matching all four skills, sorted by match score:',
        aiResults: [
          { name: 'Arjun Verma', score: '94%', skills: 'Java, Kafka, Redis' },
          { name: 'Neha Kapoor', score: '89%', skills: 'Java, AWS' },
          { name: 'Karthik Iyer', score: '86%', skills: 'Kafka, Docker' }
        ]
      }
    ]);

    this.candidateChatHistory.set([
      {
        id: 'cc-1',
        sender: 'ai',
        text: "Hi Rahul — your resume score is 86%. Here's what would push it higher:\n\n• Add quantifiable impact: 'Reduced API latency by 30%' reads stronger than 'improved performance'.\n• Add missing skill: 'Kubernetes' appears in 70% of roles you view but isn't on your profile."
      },
      { id: 'cc-2', sender: 'user', text: 'How do I prepare for an AI interview?' },
      {
        id: 'cc-3',
        sender: 'ai',
        text: 'Practice explaining your projects out loud in under 2 minutes, keep answers structured (situation → action → result), and expect 1-2 live coding questions on data structures.'
      }
    ]);

  }

  // ---- Resume module actions (resume-service) ----

  loadCandidateResumes() {
    this.resumeService.listMyResumes().subscribe({
      next: (res) => this.candidateResumes.set(res.data),
      error: () => this.showToast('Could not load your resumes.'),
    });
  }

  uploadResumeFile(file: File, displayName?: string) {
    if (this.candidateResumes().length >= 3) {
      this.showToast('Resume limit reached (3/3). Delete a resume first.');
      return;
    }
    this.resumeService.upload(file, displayName).subscribe({
      next: (res) => {
        this.candidateResumes.update((list) => [...list, res.data]);
        this.showToast('Resume uploaded successfully');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not upload that resume.'),
    });
  }

  setDefaultResume(resumeId: string) {
    this.resumeService.setDefault(resumeId).subscribe({
      next: (res) => {
        this.candidateResumes.update((list) => list.map((r) => (r.id === res.data.id ? res.data : { ...r, defaultResume: false })));
        this.showToast('Default resume updated');
      },
      error: () => this.showToast('Could not update the default resume.'),
    });
  }

  renameResume(resumeId: string, newDisplayName: string) {
    this.resumeService.rename(resumeId, { displayName: newDisplayName }).subscribe({
      next: (res) => {
        this.candidateResumes.update((list) => list.map((r) => (r.id === resumeId ? res.data : r)));
        this.showToast('Resume renamed');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not rename that resume.'),
    });
  }

  deleteResume(resumeId: string) {
    this.resumeService.delete(resumeId).subscribe({
      next: () => {
        this.candidateResumes.update((list) => list.filter((r) => r.id !== resumeId));
        this.showToast('Resume deleted');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not delete that resume (it may be attached to an active application).'),
    });
  }

  downloadResume(resume: ResumeResponse) {
    this.resumeService.download(resume.id).subscribe({
      next: (blob) => this.triggerBlobDownload(blob, resume.originalFileName || resume.displayName),
      error: () => this.showToast('Could not download that resume.'),
    });
  }

  previewResume(resumeId: string): Observable<Blob> {
    return this.resumeService.preview(resumeId);
  }

  /** Recruiter access, scoped server-side to candidates who applied to the recruiter's own company. */
  recruiterDownloadResume(resumeId: string, fileName: string) {
    this.resumeService.recruiterDownload(resumeId).subscribe({
      next: (blob) => this.triggerBlobDownload(blob, fileName),
      error: () => this.showToast('Could not download that resume.'),
    });
  }

  private triggerBlobDownload(blob: Blob, fileName: string) {
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = fileName;
    link.click();
    URL.revokeObjectURL(url);
  }

  // ---- Application module actions (application-service) ----
  // Candidate-facing (apply/withdraw/track/offer response) --

  loadMyApplications() {
    this.applicationService.listMyApplications().subscribe({
      next: (res) => this.candidateApplications.set(res.data),
      error: () => this.showToast('Could not load your applications.'),
    });
  }

  /** Returns the Observable (rather than subscribing internally) so callers -- e.g. the
   * job-detail "Apply now" button -- can navigate only once the application actually succeeds. */
  applyToJob(jobId: string) {
    return this.applicationService.apply(jobId).pipe(
      tap((res) => {
        this.candidateApplications.update((list) => [res.data, ...list]);
        this.showToast('Application submitted successfully!');
      })
    );
  }

  withdrawApplication(id: string) {
    this.applicationService.withdraw(id).subscribe({
      next: () => {
        this.candidateApplications.update((list) => list.filter((a) => a.id !== id));
        this.showToast('Application withdrawn successfully');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not withdraw this application.'),
    });
  }

  respondToOffer(id: string, accept: boolean) {
    const call = accept ? this.applicationService.acceptOffer(id) : this.applicationService.rejectOffer(id);
    call.subscribe({
      next: (res) => {
        this.candidateApplications.update((list) => list.map((a) => (a.id === id ? res.data : a)));
        this.showToast(accept ? 'Offer accepted' : 'Offer declined');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not record your response.'),
    });
  }

  // Recruiter-facing (company pipeline search + status transitions + notes) --

  loadCompanyApplications(criteria: ApplicationSearchCriteria = {}) {
    const companyId = this.companyId;
    if (!companyId) return;
    this.applicationService.searchCompanyApplications(companyId, criteria).subscribe({
      next: (res) => this.companyApplications.set(res.data.content),
      error: () => this.showToast('Could not load the candidate pipeline.'),
    });
  }

  loadApplicationDetail(id: string) {
    this.applicationService.getDetail(id).subscribe({
      next: (res) => this.selectedApplication.set(res.data),
      error: () => this.showToast('Could not load this application.'),
    });
  }

  clearSelectedApplication() {
    this.selectedApplication.set(null);
  }

  changeApplicationStatus(id: string, toStatus: ApplicationStatus, reason?: string) {
    this.applicationService.changeStatus(id, { toStatus, reason }).subscribe({
      next: (res) => {
        this.companyApplications.update((list) => list.map((a) => (a.id === id ? res.data : a)));
        if (this.selectedApplication()?.id === id) this.selectedApplication.set(res.data);
        this.showToast('Status updated');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not update this application.'),
    });
  }

  addApplicationNote(id: string, note: string) {
    this.applicationService.addNote(id, { note }).subscribe({
      next: (res) => {
        this.selectedApplication.update((app) => (app ? { ...app, notes: [...(app.notes ?? []), res.data] } : app));
        this.showToast('Note saved');
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not save that note.'),
    });
  }

  // ---- Resume Parser module actions (resume-parser-service) ----

  loadParsingQueue(status?: ParsingStatus, search?: string) {
    this.resumeParserService.getQueue(status, search).subscribe({
      next: (res) => this.parsingQueue.set(res.data.content),
      error: () => this.showToast('Could not load the parsing queue.'),
    });
  }

  loadParsingQueueSummary() {
    this.resumeParserService.getQueueSummary().subscribe({
      next: (res) => this.parsingQueueSummary.set(res.data),
      error: () => this.showToast('Could not load parsing KPIs.'),
    });
  }

  loadParsedResumeDetail(resumeId: string) {
    this.resumeParserService.getParsedResume(resumeId).subscribe({
      next: (res) => this.parsedResumeDetail.set(res.data),
      error: () => this.showToast('Could not load this resume.'),
    });
  }

  clearParsedResumeDetail() {
    this.parsedResumeDetail.set(null);
    this.parsingLogs.set([]);
  }

  loadParsingLogs(resumeId: string) {
    this.resumeParserService.getParsingLogs(resumeId).subscribe({
      next: (res) => this.parsingLogs.set(res.data),
      error: () => this.showToast('Could not load parsing logs.'),
    });
  }

  retryParsing(resumeId: string) {
    this.resumeParserService.retryParsing(resumeId).subscribe({
      next: () => {
        this.showToast('Retry queued');
        this.loadParsingQueue();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not retry parsing.'),
    });
  }

  loadMatchRanking(jobId: string, search?: string) {
    this.resumeParserService.getRanking(jobId, search).subscribe({
      next: (res) => this.matchRanking.set(res.data.content),
      error: () => this.showToast('Could not load the candidate ranking.'),
    });
  }

  loadMatchSummary(jobId: string) {
    this.resumeParserService.getMatchSummary(jobId).subscribe({
      next: (res) => this.matchSummary.set(res.data),
      error: () => this.showToast('Could not load match KPIs.'),
    });
  }

  loadMatchAnalysisDetail(applicationId: string) {
    this.resumeParserService.getAnalysisByApplication(applicationId).subscribe({
      next: (res) => this.matchAnalysisDetail.set(res.data),
      error: () => this.showToast('Could not load this match analysis.'),
    });
  }

  clearMatchAnalysisDetail() {
    this.matchAnalysisDetail.set(null);
  }

  /** For the compare modal: fetches one candidate's analysis without touching the shared matchAnalysisDetail signal used by the drawer. */
  fetchMatchAnalysis(applicationId: string) {
    return this.resumeParserService.getAnalysisByApplication(applicationId).pipe(map((res) => res.data));
  }

  loadRecommendations(jobId?: string, departmentId?: string, minScore?: number) {
    this.resumeParserService.getRecommendations(jobId, departmentId, minScore).subscribe({
      next: (res) => this.recommendations.set(res.data),
      error: () => this.showToast('Could not load recommendations.'),
    });
  }

  // ---- AI Interview module actions (ai-interview-service) ----

  loadAiShortlisted(jobId: string) {
    this.aiInterviewService.getShortlisted(jobId).subscribe({
      next: (res) => this.aiShortlisted.set(res.data.content),
      error: () => this.showToast('Could not load shortlisted candidates.'),
    });
  }

  loadAiRejected(jobId: string) {
    this.aiInterviewService.getRejected(jobId).subscribe({
      next: (res) => this.aiRejected.set(res.data.content),
      error: () => this.showToast('Could not load rejected candidates.'),
    });
  }

  loadAiManualReview(jobId: string) {
    this.aiInterviewService.getManualReview(jobId).subscribe({
      next: (res) => this.aiManualReview.set(res.data.content),
      error: () => this.showToast('Could not load the manual review queue.'),
    });
  }

  loadAiShortlistSummary(jobId: string) {
    this.aiInterviewService.getShortlistSummary(jobId).subscribe({
      next: (res) => this.aiShortlistSummary.set(res.data),
      error: () => this.showToast('Could not load shortlisting KPIs.'),
    });
  }

  shortlistOne(applicationId: string, jobId: string) {
    this.aiInterviewService.shortlistOne(applicationId).subscribe({
      next: () => {
        this.aiManualReview.update((list) => list.filter((c) => c.applicationId !== applicationId));
        this.showToast('Candidate shortlisted');
        this.loadAiShortlistSummary(jobId);
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not shortlist that candidate.'),
    });
  }

  rejectOneManual(applicationId: string, jobId: string) {
    this.aiInterviewService.rejectOneManual(applicationId).subscribe({
      next: () => {
        this.aiManualReview.update((list) => list.filter((c) => c.applicationId !== applicationId));
        this.showToast('Candidate rejected');
        this.loadAiShortlistSummary(jobId);
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not reject that candidate.'),
    });
  }

  bulkShortlist(applicationIds: string[], jobId: string) {
    this.aiInterviewService.bulkShortlist({ applicationIds }).subscribe({
      next: () => {
        this.aiManualReview.update((list) => list.filter((c) => !applicationIds.includes(c.applicationId)));
        this.showToast(`${applicationIds.length} candidate(s) shortlisted`);
        this.loadAiShortlistSummary(jobId);
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not shortlist those candidates.'),
    });
  }

  bulkReject(applicationIds: string[], jobId: string) {
    this.aiInterviewService.bulkReject({ applicationIds }).subscribe({
      next: () => {
        this.aiManualReview.update((list) => list.filter((c) => !applicationIds.includes(c.applicationId)));
        this.showToast(`${applicationIds.length} candidate(s) rejected`);
        this.loadAiShortlistSummary(jobId);
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not reject those candidates.'),
    });
  }

  assignAiRecruiter(request: AssignRecruiterRequest) {
    this.aiInterviewService.assignRecruiter(request).subscribe({
      next: () => this.showToast('Recruiter assigned'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not assign a recruiter.'),
    });
  }

  loadAiInterviewQueue(jobId: string, status?: InterviewSessionStatus) {
    this.aiInterviewService.getQueue(jobId, status).subscribe({
      next: (res) => this.aiInterviewQueue.set(res.data.content),
      error: () => this.showToast('Could not load the interview queue.'),
    });
  }

  loadAiInterviewAnalysisSummary(jobId: string) {
    this.aiInterviewService.getAnalysisSummary(jobId).subscribe({
      next: (res) => this.aiInterviewAnalysisSummary.set(res.data),
      error: () => this.showToast('Could not load analysis KPIs.'),
    });
  }

  loadAiCompletedInterviews(jobId: string) {
    this.aiInterviewService.getCompleted(jobId).subscribe({
      next: (res) => this.aiCompletedInterviews.set(res.data.content),
      error: () => this.showToast('Could not load completed interviews.'),
    });
  }

  loadAiInterviewReport(applicationId: string) {
    this.aiInterviewService.getReport(applicationId).subscribe({
      next: (res) => this.aiInterviewReport.set(res.data),
      error: () => this.showToast('Could not load this interview report.'),
    });
  }

  clearAiInterviewReport() {
    this.aiInterviewReport.set(null);
  }

  startAiInterviewInvite(applicationId: string, jobId: string, candidateId: string) {
    this.aiInterviewService.start(applicationId, jobId, candidateId).subscribe({
      next: () => {
        this.showToast('Interview invitation sent');
        this.loadAiInterviewQueue(jobId);
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not send the interview invitation.'),
    });
  }

  sendAiInterviewReminder(applicationId: string) {
    this.aiInterviewService.sendReminder(applicationId).subscribe({
      next: () => this.showToast('Reminder sent'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not send a reminder.'),
    });
  }

  resendAiInterviewInvite(applicationId: string, jobId: string) {
    this.aiInterviewService.resendInvite(applicationId).subscribe({
      next: () => {
        this.showToast('Invitation resent');
        this.loadAiInterviewQueue(jobId);
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not resend the invitation.'),
    });
  }

  moveAiInterviewToCoding(applicationId: string) {
    this.aiInterviewService.moveToCoding(applicationId).subscribe({
      next: () => this.showToast('Candidate moved to Coding Assessment'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not move this candidate.'),
    });
  }

  rejectAfterAiInterview(applicationId: string) {
    this.aiInterviewService.reject(applicationId).subscribe({
      next: () => this.showToast('Candidate rejected'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not reject this candidate.'),
    });
  }

  flagAiInterviewForManualReview(applicationId: string) {
    this.aiInterviewService.manualReviewFlag(applicationId).subscribe({
      next: () => this.showToast('Flagged for manual review'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not flag this candidate.'),
    });
  }

  // ---- Candidate-facing AI interview flow (ai-interview-service) ----
  // start/answer/finish return raw Observables so the interview component retains
  // full control of the intro -> live -> completion state transitions, matching the
  // established pattern for the coding-assessment exam component.

  loadCandidateInterviewDetails(applicationId: string) {
    this.aiInterviewService.getCandidateInterviewDetails(applicationId).subscribe({
      next: (res) => this.candidateInterviewDetails.set(res.data),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not load this interview.'),
    });
  }

  startCandidateInterview(request: StartInterviewRequest) {
    return this.aiInterviewService.startCandidateInterview(request).pipe(
      tap((res) => this.candidateInterviewQuestion.set(res.data))
    );
  }

  submitCandidateAnswer(request: AnswerRequest) {
    return this.aiInterviewService.submitCandidateAnswer(request).pipe(
      tap((res) => this.candidateInterviewQuestion.set(res.data))
    );
  }

  finishCandidateInterviewEarly(applicationId: string) {
    return this.aiInterviewService.finishCandidateInterview(applicationId);
  }

  loadCandidateInterviewResult(applicationId: string) {
    this.aiInterviewService.getCandidateInterviewResult(applicationId).subscribe({
      next: (res) => this.candidateInterviewResult.set(res.data),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not load your interview result.'),
    });
  }

  clearCandidateInterviewState() {
    this.candidateInterviewDetails.set(null);
    this.candidateInterviewQuestion.set(null);
    this.candidateInterviewResult.set(null);
  }

  // ---- Coding Assessment module actions (coding-assessment-service) ----
  // Recruiter-facing --

  loadCodingAssessments(status?: AssessmentStatus) {
    this.codingAssessmentService.list(status).subscribe({
      next: (res) => this.codingAssessmentsList.set(res.data.content),
      error: () => this.showToast('Could not load assessments.'),
    });
  }

  /** Returns the Observable so the create-assessment modal can close only on success. */
  createCodingAssessment(request: CreateAssessmentRequest) {
    return this.codingAssessmentService.create(request).pipe(
      tap(() => {
        this.showToast(request.publishNow ? 'Assessment created and published' : 'Assessment created');
        this.loadCodingAssessments();
      })
    );
  }

  publishCodingAssessment(id: string) {
    this.codingAssessmentService.publish(id).subscribe({
      next: () => {
        this.showToast('Assessment published');
        this.loadCodingAssessments();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not publish this assessment.'),
    });
  }

  cloneCodingAssessment(id: string) {
    this.codingAssessmentService.clone(id).subscribe({
      next: () => {
        this.showToast('Assessment cloned');
        this.loadCodingAssessments();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not clone this assessment.'),
    });
  }

  archiveCodingAssessment(id: string) {
    this.codingAssessmentService.archive(id).subscribe({
      next: () => {
        this.showToast('Assessment archived');
        this.loadCodingAssessments();
      },
      error: (err) => this.showToast(err?.error?.message ?? 'Could not archive this assessment.'),
    });
  }

  loadCodingAssessmentQueue(assessmentId: string, status?: CandidateAssessmentStatus) {
    this.codingAssessmentService.getQueue(assessmentId, status).subscribe({
      next: (res) => this.codingAssessmentQueue.set(res.data.content),
      error: () => this.showToast('Could not load the assessment queue.'),
    });
  }

  sendCodingReminders(assessmentId: string) {
    this.codingAssessmentService.sendReminders(assessmentId).subscribe({
      next: () => this.showToast('Reminders sent'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not send reminders.'),
    });
  }

  remindCodingCandidate(assessmentId: string, applicationId: string) {
    this.codingAssessmentService.remindCandidate(assessmentId, applicationId).subscribe({
      next: () => this.showToast('Reminder sent'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not send a reminder.'),
    });
  }

  loadCodingResultsSummary(assessmentId: string) {
    this.codingAssessmentService.getResultsSummary(assessmentId).subscribe({
      next: (res) => this.codingResultsSummary.set(res.data),
      error: () => this.showToast('Could not load results KPIs.'),
    });
  }

  loadCodingLeaderboard(assessmentId: string) {
    this.codingAssessmentService.getLeaderboard(assessmentId).subscribe({
      next: (res) => this.codingLeaderboard.set(res.data),
      error: () => this.showToast('Could not load the leaderboard.'),
    });
  }

  loadCodingCompleted(assessmentId: string) {
    this.codingAssessmentService.getCompleted(assessmentId).subscribe({
      next: (res) => this.codingCompletedList.set(res.data.content),
      error: () => this.showToast('Could not load completed assessments.'),
    });
  }

  loadCodingSubmissionDrawer(assessmentId: string, applicationId: string) {
    this.codingAssessmentService.getSubmissionDrawer(assessmentId, applicationId).subscribe({
      next: (res) => this.codingSubmissionDrawer.set(res.data),
      error: () => this.showToast('Could not load this submission.'),
    });
  }

  clearCodingSubmissionDrawer() {
    this.codingSubmissionDrawer.set(null);
  }

  moveCodingCandidateToNextStage(assessmentId: string, applicationId: string) {
    this.codingAssessmentService.moveToNextStage(assessmentId, applicationId).subscribe({
      next: () => this.showToast('Candidate moved to next stage'),
      error: (err) => this.showToast(err?.error?.message ?? 'Could not move this candidate.'),
    });
  }

  loadCodingAnalytics(assessmentId: string) {
    this.codingAssessmentService.getAnalytics(assessmentId).subscribe({
      next: (res) => this.codingAnalytics.set(res.data),
      error: () => this.showToast('Could not load analytics.'),
    });
  }

  // Candidate-facing --

  loadCandidateAssessmentHistory() {
    this.codingAssessmentService.getHistory().subscribe({
      next: (res) => this.candidateAssessmentHistory.set(res.data.content),
      error: () => this.showToast('Could not load your assessment history.'),
    });
  }

  loadCandidateAssessmentIntro(id: string) {
    this.codingAssessmentService.getIntro(id).subscribe({
      next: (res) => this.candidateAssessmentIntro.set(res.data),
      error: () => this.showToast('Could not load this assessment.'),
    });
  }

  acceptAssessmentRules(id: string) {
    return this.codingAssessmentService.acceptRules(id);
  }

  startCodingAssessment(id: string, request?: StartAssessmentRequest) {
    return this.codingAssessmentService.start(id, request);
  }

  goToAssessmentQuestion(id: string, questionIndex: number) {
    return this.codingAssessmentService.goToQuestion(id, questionIndex);
  }

  runAssessmentCode(request: RunCodeRequest) {
    return this.codingAssessmentService.runCode(request);
  }

  submitAssessmentCode(request: SubmitCodeRequest) {
    return this.codingAssessmentService.submitCode(request);
  }

  markAssessmentQuestionForReview(id: string, request: MarkForReviewRequest) {
    return this.codingAssessmentService.markForReview(id, request);
  }

  finishCodingAssessment(id: string, request?: FinishAssessmentRequest) {
    return this.codingAssessmentService.finish(id, request);
  }

  recordAssessmentAntiCheatEvent(id: string, request: AntiCheatEventRequest) {
    this.codingAssessmentService.recordAntiCheatEvent(id, request).subscribe({ error: () => {} });
  }

  getAssessmentResult(id: string) {
    return this.codingAssessmentService.getResult(id);
  }
}
