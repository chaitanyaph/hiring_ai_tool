import { Routes } from '@angular/router';
import { RecruiterLayoutComponent } from './core/layouts/recruiter-layout.component';
import { CandidateLayoutComponent } from './core/layouts/candidate-layout.component';
import { authGuard } from './core/guards/auth.guard';
import { recruiterGuard, candidateGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },
  { path: 'login', loadComponent: () => import('./features/auth/login.component').then(m => m.LoginComponent) },
  { path: 'mfa', loadComponent: () => import('./features/auth/mfa.component').then(m => m.MfaComponent) },
  { path: 'register-company', loadComponent: () => import('./features/auth/register-company.component').then(m => m.RegisterCompanyComponent) },
  { path: 'register-candidate', loadComponent: () => import('./features/auth/register-candidate.component').then(m => m.RegisterCandidateComponent) },
  { path: 'forgot-password', loadComponent: () => import('./features/auth/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'reset-password', loadComponent: () => import('./features/auth/reset-password.component').then(m => m.ResetPasswordComponent) },
  { path: 'verify-email', loadComponent: () => import('./features/auth/verify-email.component').then(m => m.VerifyEmailComponent) },
  { path: 'oauth2/callback', loadComponent: () => import('./features/auth/oauth2-callback.component').then(m => m.Oauth2CallbackComponent) },

  {
    path: 'recruiter',
    component: RecruiterLayoutComponent,
    canActivate: [authGuard, recruiterGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/recruiter-dashboard.component').then(m => m.RecruiterDashboardComponent) },
      { path: 'jobs', loadComponent: () => import('./features/jobs/jobs-list.component').then(m => m.JobsListComponent) },
      { path: 'candidates', loadComponent: () => import('./features/candidates/candidates-list.component').then(m => m.CandidatesListComponent) },
      { path: 'candidates/:id', loadComponent: () => import('./features/candidates/candidate-detail.component').then(m => m.CandidateDetailComponent) },
      { path: 'interviews', loadComponent: () => import('./features/interviews/interviews.component').then(m => m.InterviewsComponent) },
      { path: 'assessments', loadComponent: () => import('./features/assessments/assessments.component').then(m => m.AssessmentsComponent) },
      { path: 'analytics', loadComponent: () => import('./features/analytics/analytics.component').then(m => m.AnalyticsComponent) },
      { path: 'ai-assistant', loadComponent: () => import('./features/ai-assistant/ai-assistant.component').then(m => m.AiAssistantComponent) },
      { path: 'parsing', loadComponent: () => import('./features/parsing/parsing.component').then(m => m.ParsingComponent) },
      { path: 'matching', loadComponent: () => import('./features/matching/matching.component').then(m => m.MatchingComponent) },
      { path: 'shortlisting', loadComponent: () => import('./features/shortlisting/shortlisting.component').then(m => m.ShortlistingComponent) },
      { path: 'recommendations', loadComponent: () => import('./features/recommendations/recommendations.component').then(m => m.RecommendationsComponent) },
      { path: 'ai-interviews', loadComponent: () => import('./features/ai-interviews/ai-interviews.component').then(m => m.AiInterviewsComponent) },
      { path: 'coding-assessments', loadComponent: () => import('./features/coding-assessments/coding-assessments.component').then(m => m.CodingAssessmentsComponent) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/notifications.component').then(m => m.NotificationsComponent) },
      { path: 'offers', loadComponent: () => import('./features/offers/offers.component').then(m => m.OffersComponent) },
      { path: 'settings', loadComponent: () => import('./features/settings/settings.component').then(m => m.SettingsComponent) }
    ]
  },

  {
    path: 'candidate',
    component: CandidateLayoutComponent,
    canActivate: [authGuard, candidateGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },
      { path: 'dashboard', loadComponent: () => import('./features/dashboard/candidate-dashboard.component').then(m => m.CandidateDashboardComponent) },
      { path: 'profile', loadComponent: () => import('./features/profile/candidate-profile.component').then(m => m.CandidateProfileComponent) },
      { path: 'resumes', loadComponent: () => import('./features/resumes/candidate-resumes.component').then(m => m.CandidateResumesComponent) },
      { path: 'browse-jobs', loadComponent: () => import('./features/jobs/candidate-browse-jobs.component').then(m => m.CandidateBrowseJobsComponent) },
      { path: 'jobs/:id', loadComponent: () => import('./features/jobs/candidate-job-detail.component').then(m => m.CandidateJobDetailComponent) },
      { path: 'applications', loadComponent: () => import('./features/candidates/candidate-applications.component').then(m => m.CandidateApplicationsComponent) },
      { path: 'applications/:id', loadComponent: () => import('./features/candidates/candidate-application-detail.component').then(m => m.CandidateApplicationDetailComponent) },
      { path: 'interviews', loadComponent: () => import('./features/interviews/candidate-my-interviews.component').then(m => m.CandidateMyInterviewsComponent) },
      { path: 'ai-coach', loadComponent: () => import('./features/ai-assistant/ai-coach.component').then(m => m.AiCoachComponent) },
      { path: 'coding-assessments', loadComponent: () => import('./features/assessments/candidate-coding-assessments.component').then(m => m.CandidateCodingAssessmentsComponent) },
      { path: 'offers', loadComponent: () => import('./features/offers/candidate-offers.component').then(m => m.CandidateOffersComponent) },
      { path: 'notifications', loadComponent: () => import('./features/notifications/candidate-notifications.component').then(m => m.CandidateNotificationsComponent) },
      { path: 'settings', loadComponent: () => import('./features/settings/candidate-settings.component').then(m => m.CandidateSettingsComponent) }
    ]
  },
  {
    path: 'candidate/coding-assessments/:id',
    canActivate: [authGuard, candidateGuard],
    loadComponent: () => import('./features/assessments/candidate-assessment-detail.component').then(m => m.CandidateAssessmentDetailComponent)
  },
  {
    path: 'candidate/coding-assessments/:id/exam',
    canActivate: [authGuard, candidateGuard],
    loadComponent: () => import('./features/assessments/candidate-assessment-exam.component').then(m => m.CandidateAssessmentExamComponent)
  },
  {
    path: 'candidate/coding-assessments/:id/result',
    canActivate: [authGuard, candidateGuard],
    loadComponent: () => import('./features/assessments/candidate-assessment-result.component').then(m => m.CandidateAssessmentResultComponent)
  },
  {
    path: 'candidate/ai-interview/:applicationId',
    canActivate: [authGuard, candidateGuard],
    loadComponent: () => import('./features/ai-interview/candidate-ai-interview.component').then(m => m.CandidateAiInterviewComponent)
  },
  { path: '**', redirectTo: 'login' }
];
