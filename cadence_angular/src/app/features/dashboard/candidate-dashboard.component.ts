import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { JobService } from '../../core/services/job.service';
import { CandidateJobSummaryResponse } from '../../core/models/job.model';

@Component({
  selector: 'app-candidate-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-dashboard">
      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>Welcome back, {{ firstName() }}</h1>
          <p>Here's what's happening with your job search today.</p>
        </div>
      </div>

      <!-- Row 2: Profile Completion + AI Resume Score -->
      <div class="row-2">
        <!-- Profile Completion Card -->
        <div class="card">
          <div class="card-head">
            <div>
              <h2>Profile completion</h2>
              <div class="card-sub">Complete profiles get 3x more recruiter views</div>
            </div>
            <a class="card-link" (click)="state.openModal('profile-wizard')">Complete profile</a>
          </div>
          <div class="profile-completion-body" style="display:flex; align-items:center; gap:18px;">
            <div class="profile-ring">
              <svg viewBox="0 0 64 64">
                <circle class="ring-bg" cx="32" cy="32" r="28"/>
                <circle class="ring-fg" cx="32" cy="32" r="28" stroke-dasharray="175.9" [attr.stroke-dashoffset]="profileRingOffset()"/>
              </svg>
              <div class="ring-label">{{ dashboard()?.profileCompletionPercent ?? 0 }}%</div>
            </div>
            <div style="flex:1;">
              <div class="chip-group">
                <span class="skill-pill" [class.done]="profile()?.fullName">{{ profile()?.fullName ? '✓ ' : '' }}Basic info</span>
                <span class="skill-pill" [class.done]="profile()?.resumeUrl">{{ profile()?.resumeUrl ? '✓ ' : '' }}Resume</span>
                <span class="skill-pill" [class.done]="profile()?.experience?.length">{{ profile()?.experience?.length ? '✓ ' : '' }}Experience</span>
                <span class="skill-pill" [class.done]="profile()?.certifications?.length">{{ profile()?.certifications?.length ? '✓ ' : '' }}Certifications</span>
                <span class="skill-pill" [class.done]="profile()?.portfolio?.githubUrl || profile()?.portfolio?.linkedinUrl">{{ (profile()?.portfolio?.githubUrl || profile()?.portfolio?.linkedinUrl) ? '✓ ' : '' }}Portfolio</span>
              </div>
            </div>
          </div>
        </div>

        <!-- AI Resume Score Card -->
        <div class="card">
          <div class="card-head">
            <div>
              <h2>AI resume score</h2>
              <div class="card-sub">Based on your last uploaded resume</div>
            </div>
            <a class="card-link" (click)="goToResumes()">Improve score</a>
          </div>
          <div class="profile-completion-body" style="display:flex; align-items:center; gap:18px;">
            <div class="profile-ring">
              <svg viewBox="0 0 64 64">
                <circle class="ring-bg" cx="32" cy="32" r="28"/>
                <circle class="ring-fg" cx="32" cy="32" r="28" stroke="#1F7A6C" stroke-dasharray="175.9" [attr.stroke-dashoffset]="resumeScoreRingOffset()"/>
              </svg>
              <div class="ring-label" style="color: var(--teal);">{{ dashboard()?.aiResumeScore ?? '—' }}{{ dashboard()?.aiResumeScore != null ? '%' : '' }}</div>
            </div>
            <div class="score-text" style="flex:1; font-size:12.5px; color:var(--ink-soft); line-height:1.4;">
              {{ dashboard()?.aiResumeScore != null ? 'Score generated from your uploaded resume by the AI matching engine.' : 'Upload a resume to get an AI-generated score.' }}
            </div>
          </div>
        </div>
      </div>

      <!-- KPI Row -->
      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-label">Active applications</div>
          <div class="kpi-value">{{ dashboard()?.activeApplicationsCount ?? 0 }}</div>
          <div class="kpi-trend"><span class="muted">&nbsp;</span></div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Saved jobs</div>
          <div class="kpi-value">{{ dashboard()?.savedJobsCount ?? 0 }}</div>
          <div class="kpi-trend"><span class="muted">&nbsp;</span></div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Profile views</div>
          <div class="kpi-value">{{ dashboard()?.profileViews ?? 0 }}</div>
          <div class="kpi-trend"><span class="muted">&nbsp;</span></div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Upcoming interviews</div>
          <div class="kpi-value">{{ dashboard()?.upcomingInterviewsCount ?? 0 }}</div>
          <div class="kpi-trend"><span class="muted">&nbsp;</span></div>
        </div>
      </div>

      <!-- Row 3: Three equal columns -->
      <div class="row-3">
        <!-- Recent applications -->
        <div class="card">
          <div class="card-head">
            <h2>Recent applications</h2>
            <a class="card-link" (click)="goToApplications()">View all</a>
          </div>
          <div class="list-card">
            <div class="interview-item" *ngFor="let app of dashboard()?.recentApplications ?? []">
              <div class="avatar">{{ (app.companyNameSnapshot || '??').slice(0, 2).toUpperCase() }}</div>
              <div class="meta">
                <div class="who">{{ app.jobTitleSnapshot }}</div>
                <div class="role">{{ app.companyNameSnapshot }}</div>
                <span class="badge stage">{{ app.status }}</span>
              </div>
              <div class="when">{{ app.appliedAt | date: 'MMM d' }}</div>
            </div>
            <div class="muted" style="font-size:12.5px;" *ngIf="!(dashboard()?.recentApplications?.length)">No applications yet.</div>
          </div>
        </div>

        <!-- No dedicated recommendation/matching endpoint exists, so this shows the most
             recently published open roles rather than a personalized ranking. -->
        <div class="card">
          <div class="card-head">
            <h2>Suggested jobs</h2>
            <a class="card-link" (click)="goToJobs()">Browse all</a>
          </div>
          <div class="list-card">
            <div class="interview-item" *ngFor="let job of suggestedJobs()" (click)="goToJobDetail(job.id)" style="cursor:pointer;">
              <div class="avatar">{{ (job.companyName || '??').slice(0, 2).toUpperCase() }}</div>
              <div class="meta">
                <div class="who">{{ job.title }}</div>
                <div class="role">{{ job.companyName || 'Unknown company' }} · {{ job.location || 'Not specified' }}</div>
              </div>
            </div>
            <p style="font-size:12.5px; color:var(--ink-soft); padding:8px 0;" *ngIf="!suggestedJobs().length">No open roles yet. <a class="card-link" (click)="goToJobs()">Browse all open roles</a> to get started.</p>
          </div>
        </div>

        <!-- DashboardResponse only exposes upcomingInterviewsCount (bound above in the
             KPI row), not the list itself -- a real list needs a candidate-facing endpoint
             on interview-management-service that doesn't exist yet, so this shows an
             honest empty state rather than fabricated interview details. -->
        <div class="card">
          <div class="card-head">
            <h2>Upcoming interviews</h2>
          </div>
          <div class="list-card">
            <p style="font-size:12.5px; color:var(--ink-soft); padding:8px 0;">No upcoming interviews yet. <button type="button" style="background:none;border:none;color:var(--indigo);font-weight:600;cursor:pointer;padding:0;font-family:inherit;font-size:inherit;" (click)="router.navigate(['/candidate/interviews'])">View My Interviews</button> once you've applied to a role.</p>
          </div>
        </div>
      </div>

      <!-- Quick Actions -->
      <div class="card" style="margin-top:0;">
        <div class="card-head"><h2>Quick actions</h2></div>
        <div class="quick-actions-row">
          <button class="btn-primary-sm" (click)="state.openModal('profile-wizard')">Complete profile</button>
          <button class="btn-ghost" (click)="goToJobs()">Browse jobs</button>
          <button class="btn-ghost" (click)="goToResumes()">Update resume</button>
          <button class="btn-ghost" (click)="goToAssessments()">Start coding assessment</button>
        </div>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateDashboardComponent implements OnInit {
  constructor(public state: AppStateService, public router: Router, private jobService: JobService) {}

  profile = computed(() => this.state.candidateProfile());
  dashboard = computed(() => this.state.candidateDashboard());
  suggestedJobs = signal<CandidateJobSummaryResponse[]>([]);

  firstName = computed(() => this.profile()?.fullName?.split(' ')[0] ?? this.state.currentUser()?.name?.split(' ')[0] ?? 'there');

  profileRingOffset = computed(() => (175.9 * (1 - (this.dashboard()?.profileCompletionPercent ?? 0) / 100)).toFixed(1));
  resumeScoreRingOffset = computed(() => (175.9 * (1 - (this.dashboard()?.aiResumeScore ?? 0) / 100)).toFixed(1));

  ngOnInit() {
    this.state.loadCandidateProfile();
    this.state.loadCandidateDashboard();
    this.jobService.browsePublicJobs({}, 0, 3).subscribe({
      next: (res) => this.suggestedJobs.set(res.data.content),
      error: () => {},
    });
  }

  goToJobs() {
    this.router.navigate(['/candidate/browse-jobs']);
  }

  goToJobDetail(id: string) {
    this.router.navigate(['/candidate/jobs', id]);
  }

  goToApplications() {
    this.router.navigate(['/candidate/applications']);
  }

  goToResumes() {
    this.router.navigate(['/candidate/resumes']);
  }

  goToAssessments() {
    this.router.navigate(['/candidate/coding-assessments']);
  }

  /**
   * The real AI interview screen now exists at /candidate/ai-interview/:applicationId
   * (see candidate-applications.component.ts's "Start AI interview" button for a fully
   * wired entry point). This dashboard's "Upcoming interviews" list is still static mock
   * data with no real applicationId to navigate with, so this button stays a toast stub
   * until that list is wired to a real data source.
   */
  startAiInterview() {
    this.state.showToast('AI Interview session launched!');
  }
}
