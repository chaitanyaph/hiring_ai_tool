import { Component, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';

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

        <!-- Suggested jobs: left static/mock -- no recommendation endpoint exists on any
             service yet, and job-service has no candidate-safe browse endpoint either
             (see Module 3 gap notes), so there's no real data source to bind this to. -->
        <div class="card">
          <div class="card-head">
            <h2>Suggested jobs</h2>
            <a class="card-link" (click)="goToJobs()">Browse all</a>
          </div>
          <div class="list-card">
            <div class="rec-item">
              <div class="rec-top">
                <span class="who">Senior Backend Engineer</span>
                <span class="match-badge">91% match</span>
              </div>
              <div class="rec-skills">
                <span class="skill-pill">Java</span>
                <span class="skill-pill">Kafka</span>
                <span class="skill-pill">AWS</span>
              </div>
              <div class="rec-actions">
                <button class="view" (click)="goToJobDetail('job-1')">View job</button>
                <button class="shortlist" (click)="state.showToast('Job saved to your list')">Save</button>
              </div>
            </div>
            <div class="rec-item">
              <div class="rec-top">
                <span class="who">Platform Engineer</span>
                <span class="match-badge">84% match</span>
              </div>
              <div class="rec-skills">
                <span class="skill-pill">Spring Boot</span>
                <span class="skill-pill">Docker</span>
              </div>
              <div class="rec-actions">
                <button class="view" (click)="goToJobDetail('job-2')">View job</button>
                <button class="shortlist" (click)="state.showToast('Job saved to your list')">Save</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Upcoming interviews: left static/mock -- DashboardResponse only exposes
             upcomingInterviewsCount (bound above in the KPI row), not the list itself;
             a real list needs interview-management-service (Module 10). -->
        <div class="card">
          <div class="card-head">
            <h2>Upcoming interviews</h2>
          </div>
          <div class="list-card">
            <div class="interview-item">
              <div class="avatar">HR</div>
              <div class="meta">
                <div class="who">HR round · Backend Engineer</div>
                <div class="role">Acme Corp</div>
                <span class="tag hr">Video call</span>
              </div>
              <div class="when">Tomorrow<br>11:00 AM</div>
            </div>
            <div class="interview-item">
              <div class="avatar">AI</div>
              <div class="meta">
                <div class="who">AI Interview · Frontend Engineer</div>
                <div class="role">Nimbus Labs</div>
                <span class="tag ai">Due in 3 days</span>
              </div>
              <button class="btn-primary-sm" style="padding:6px 12px; font-size:11px; flex-shrink:0;" (click)="startAiInterview()">Start</button>
            </div>
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
  constructor(public state: AppStateService, public router: Router) {}

  profile = computed(() => this.state.candidateProfile());
  dashboard = computed(() => this.state.candidateDashboard());

  firstName = computed(() => this.profile()?.fullName?.split(' ')[0] ?? this.state.currentUser()?.name?.split(' ')[0] ?? 'there');

  profileRingOffset = computed(() => (175.9 * (1 - (this.dashboard()?.profileCompletionPercent ?? 0) / 100)).toFixed(1));
  resumeScoreRingOffset = computed(() => (175.9 * (1 - (this.dashboard()?.aiResumeScore ?? 0) / 100)).toFixed(1));

  ngOnInit() {
    this.state.loadCandidateProfile();
    this.state.loadCandidateDashboard();
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
