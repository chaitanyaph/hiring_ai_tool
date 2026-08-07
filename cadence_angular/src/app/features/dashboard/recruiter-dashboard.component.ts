import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { Candidate } from '../../core/models/models';
import { ApplicationResponse, ApplicationStage, ApplicationStatus } from '../../core/models/application.model';
import { compareToCurrentStage } from '../../core/utils/application.mapper';
import { SkeletonComponent } from '../../shared/components/skeleton.component';

@Component({
  selector: 'app-recruiter-dashboard',
  standalone: true,
  imports: [CommonModule, SkeletonComponent],
  template: `
    <div class="dashboard-wrap">
      <!-- Page Heading -->
      <div class="page-head">
        <div>
          <h1>Good morning{{ state.currentUser()?.name ? ', ' + state.currentUser()?.name!.split(' ')[0] : '' }}</h1>
          <p>Here's how hiring is trending across {{ state.company()?.companyName || 'your workspace' }} today.</p>
        </div>
      </div>

      <ng-container *ngIf="!state.companyApplicationsLoading(); else kpiSkeleton">
        <!-- KPI Row -->
        <div class="kpi-row">
          <div class="kpi-card">
            <div class="kpi-label">Total applications</div>
            <div class="kpi-value">{{ kpis().totalApplications }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Open positions</div>
            <div class="kpi-value">{{ kpis().openPositions }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Candidates in pipeline</div>
            <div class="kpi-value">{{ kpis().inPipeline }}</div>
          </div>
          <div class="kpi-card">
            <div class="kpi-label">Avg. time to hire</div>
            <div class="kpi-value">{{ kpis().avgTimeToHireDays !== null ? kpis().avgTimeToHireDays + 'd' : 'N/A' }}</div>
            <div class="kpi-trend" *ngIf="kpis().avgTimeToHireDays === null"><span class="muted">No hires yet</span></div>
          </div>
        </div>
      </ng-container>
      <ng-template #kpiSkeleton>
        <div class="kpi-row">
          <div class="kpi-card" *ngFor="let _ of [1,2,3,4]">
            <app-skeleton width="70%" height="12px" />
            <div style="height:8px;"></div>
            <app-skeleton width="45%" height="24px" />
          </div>
        </div>
      </ng-template>

      <!-- Interview funnel card -->
      <div class="card funnel-card" style="margin-bottom: 20px;">
        <div class="card-head">
          <div>
            <h2>Interview funnel</h2>
            <div class="card-sub">Live pipeline across all open roles</div>
          </div>
        </div>
        <div class="funnel" *ngIf="!state.companyApplicationsLoading(); else funnelSkeleton">
          <div class="funnel-row">
            <div class="stage-name">Applicants</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" [style.width.%]="funnel().applicants.pct">{{ funnel().applicants.count }}</div>
            </div>
            <div class="conv">&mdash;</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">AI Shortlisted <small>match score &ge; 80</small></div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" [style.width.%]="funnel().aiShortlisted.pct">{{ funnel().aiShortlisted.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().aiShortlisted.pct }}%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">AI Rejected</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" style="background:var(--danger);" [style.width.%]="funnel().aiRejected.pct">{{ funnel().aiRejected.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().aiRejected.pct }}%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Interview pending</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" [style.width.%]="funnel().interviewPending.pct">{{ funnel().interviewPending.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().interviewPending.pct }}%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Coding assessment pending</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" [style.width.%]="funnel().codingPending.pct">{{ funnel().codingPending.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().codingPending.pct }}%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Coding assessment passed</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" [style.width.%]="funnel().codingPassed.pct">{{ funnel().codingPassed.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().codingPassed.pct }}%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Coding assessment failed</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" style="background:var(--danger);" [style.width.%]="funnel().codingFailed.pct">{{ funnel().codingFailed.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().codingFailed.pct }}%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Technical round</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" [style.width.%]="funnel().technical.pct">{{ funnel().technical.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().technical.pct }}%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">HR round</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" [style.width.%]="funnel().hr.pct">{{ funnel().hr.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().hr.pct }}%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Offer released</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" [style.width.%]="funnel().offer.pct">{{ funnel().offer.count }}</div>
            </div>
            <div class="conv"><b>{{ funnel().offer.pct }}%</b> of applicants</div>
          </div>
        </div>
        <ng-template #funnelSkeleton>
          <div class="funnel">
            <div class="funnel-row" *ngFor="let _ of [1,2,3,4,5,6,7,8,9]">
              <app-skeleton width="140px" height="12px" />
              <app-skeleton width="100%" height="20px" />
              <app-skeleton width="60px" height="12px" />
            </div>
          </div>
        </ng-template>
      </div>

      <!-- Row 2: Charts and breakdowns -->
      <div class="row-2" style="margin-bottom: 20px;">
        <!-- Left: Hiring analytics -->
        <div class="card">
          <div class="card-head">
            <div>
              <h2>Hiring analytics</h2>
              <div class="card-sub">Applications received over time</div>
            </div>
            <div class="period-toggle">
              <button [class.active]="activePeriod() === '7d'" (click)="activePeriod.set('7d')">7D</button>
              <button [class.active]="activePeriod() === '30d'" (click)="activePeriod.set('30d')">30D</button>
              <button [class.active]="activePeriod() === '90d'" (click)="activePeriod.set('90d')">90D</button>
            </div>
          </div>
          <div class="chart-wrap">
            <svg viewBox="0 0 560 180" preserveAspectRatio="none" style="display:block; width:100%; height:180px;">
              <defs>
                <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#372F84" stop-opacity="0.22"/>
                  <stop offset="100%" stop-color="#372F84" stop-opacity="0"/>
                </linearGradient>
              </defs>
              <line x1="0" y1="140" x2="560" y2="140" stroke="#E7E3D6" stroke-width="1"/>
              <line x1="0" y1="90" x2="560" y2="90" stroke="#E7E3D6" stroke-width="1"/>
              <line x1="0" y1="40" x2="560" y2="40" stroke="#E7E3D6" stroke-width="1"/>
              <path [attr.d]="chartData().area" fill="url(#areaFill)"/>
              <path [attr.d]="chartData().line" fill="none" stroke="#372F84" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
              <g>
                <circle *ngFor="let p of filteredPoints()" [attr.cx]="p[0]" [attr.cy]="p[1]" r="3.2" fill="#372F84"/>
              </g>
            </svg>
          </div>
        </div>

        <!-- Right: Resume screening + Source Analysis -->
        <div class="card">
          <div class="split-card-section">
            <div class="card-head" style="margin-bottom:12px;">
              <h2>Resume screening</h2>
            </div>
            <div class="donut-row" *ngIf="resumeScreening().total > 0; else noResumeScreening">
              <svg width="92" height="92" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" fill="none" stroke="#EFEBDF" stroke-width="14"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#1F7A6C" stroke-width="14" [attr.stroke-dasharray]="resumeScreening().shortlistedDash + ' 251.3'" transform="rotate(-90 50 50)"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#C79A2E" stroke-width="14" [attr.stroke-dasharray]="resumeScreening().manualReviewDash + ' 251.3'" [attr.stroke-dashoffset]="-resumeScreening().shortlistedDash" transform="rotate(-90 50 50)"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#B3412C" stroke-width="14" opacity="0.55" [attr.stroke-dasharray]="resumeScreening().rejectedDash + ' 251.3'" [attr.stroke-dashoffset]="-(resumeScreening().shortlistedDash + resumeScreening().manualReviewDash)" transform="rotate(-90 50 50)"/>
              </svg>
              <div class="legend">
                <div class="legend-item"><span class="legend-dot" style="background:#1F7A6C;"></span>Shortlisted<b>{{ resumeScreening().shortlistedPct }}%</b></div>
                <div class="legend-item"><span class="legend-dot" style="background:#C79A2E;"></span>Manual review<b>{{ resumeScreening().manualReviewPct }}%</b></div>
                <div class="legend-item"><span class="legend-dot" style="background:#B3412C; opacity:.55;"></span>Rejected<b>{{ resumeScreening().rejectedPct }}%</b></div>
              </div>
            </div>
            <ng-template #noResumeScreening>
              <p class="muted" style="font-size:13px;">No resume screening decisions yet.</p>
            </ng-template>
          </div>
          <div>
            <div class="card-head" style="margin-bottom:12px;">
              <h2>Source analysis</h2>
            </div>
            <div class="source-list">
              <div class="source-item"><span>LinkedIn</span><div class="source-track"><div class="source-fill" style="width:34%;"></div></div><span>34%</span></div>
              <div class="source-item"><span>Careers page</span><div class="source-track"><div class="source-fill" style="width:28%; background:#C79A2E;"></div></div><span>28%</span></div>
              <div class="source-item"><span>Referral</span><div class="source-track"><div class="source-fill" style="width:19%; background:#1F7A6C;"></div></div><span>19%</span></div>
              <div class="source-item"><span>Naukri</span><div class="source-track"><div class="source-fill" style="width:12%;"></div></div><span>12%</span></div>
              <div class="source-item"><span>CSV import</span><div class="source-track"><div class="source-fill" style="width:7%;"></div></div><span>7%</span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Row 3: Stack lists -->
      <div class="row-3">
        <!-- Upcoming interviews -->
        <div class="card">
          <div class="card-head">
            <h2>Upcoming interviews</h2>
            <a class="card-link" (click)="router.navigate(['/recruiter/interviews'])">Calendar</a>
          </div>
          <div class="list-card">
            <div class="interview-item">
              <div class="avatar">PK</div>
              <div class="meta">
                <div class="who">Priya Kulkarni</div>
                <div class="role">Backend Engineer</div>
                <span class="tag ai">AI Interview</span>
              </div>
              <div class="when">Today<br>3:30 PM</div>
            </div>
            <div class="interview-item">
              <div class="avatar">RM</div>
              <div class="meta">
                <div class="who">Rohan Mehta</div>
                <div class="role">Senior DevOps Engineer</div>
                <span class="tag tech">Technical round</span>
              </div>
              <div class="when">Today<br>5:00 PM</div>
            </div>
            <div class="interview-item">
              <div class="avatar">SI</div>
              <div class="meta">
                <div class="who">Sneha Iyer</div>
                <div class="role">Frontend Engineer</div>
                <span class="tag hr">HR round</span>
              </div>
              <div class="when">Tomorrow<br>11:00 AM</div>
            </div>
          </div>
        </div>

        <!-- AI Recommendations -->
        <div class="card">
          <div class="card-head">
            <h2>AI recommendations</h2>
            <a class="card-link" (click)="router.navigate(['/recruiter/ai-assistant'])">See all</a>
          </div>
          <div class="list-card">
            <div class="rec-item">
              <div class="rec-top">
                <span class="who">Arjun Verma</span>
                <span class="match-badge">94% match</span>
              </div>
              <div class="rec-skills">
                <span class="skill-pill">Java</span>
                <span class="skill-pill">Spring Boot</span>
                <span class="skill-pill">Kafka</span>
                <span class="skill-pill">Redis</span>
              </div>
              <div class="rec-actions">
                <button class="view" style="font-family:'Inter',sans-serif;" (click)="viewProfile('cand-1')">View profile</button>
                <button class="shortlist" style="font-family:'Inter',sans-serif;" (click)="state.showToast('Added to shortlist')">Shortlist</button>
              </div>
            </div>
            <div class="rec-item">
              <div class="rec-top">
                <span class="who">Neha Kapoor</span>
                <span class="match-badge">89% match</span>
              </div>
              <div class="rec-skills">
                <span class="skill-pill">Java</span>
                <span class="skill-pill">Microservices</span>
                <span class="skill-pill">AWS</span>
              </div>
              <div class="rec-actions">
                <button class="view" style="font-family:'Inter',sans-serif;" (click)="viewProfile('cand-5')">View profile</button>
                <button class="shortlist" style="font-family:'Inter',sans-serif;" (click)="state.showToast('Added to shortlist')">Shortlist</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Activity -->
        <div class="card">
          <div class="card-head">
            <h2>Recent activity</h2>
          </div>
          <div class="list-card">
            <div class="activity-item">
              <div class="activity-dot"></div>
              <div>
                <div class="atext">AI Matching shortlisted <b>14 candidates</b> for Backend Engineer</div>
                <div class="atime">12 min ago</div>
              </div>
            </div>
            <div class="activity-item">
              <div class="activity-dot" style="background:#1F7A6C;"></div>
              <div>
                <div class="atext">Interview completed — Priya K. scored <b>8.7 / 10</b></div>
                <div class="atime">40 min ago</div>
              </div>
            </div>
            <div class="activity-item">
              <div class="activity-dot" style="background:#372F84;"></div>
              <div>
                <div class="atext">Offer generated for <b>Rohan Mehta</b></div>
                <div class="atime">2h ago</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .dashboard-wrap {
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0;
      font-family: $font-sans;
      color: var(--ink);
    }

    .muted {
      color: var(--ink-soft);
    }
  `]
})
export class RecruiterDashboardComponent {
  activePeriod = signal<'7d' | '30d' | '90d'>('7d');

  private datasets = {
    '7d': [40, 58, 49, 72, 66, 90, 81],
    '30d': [30, 42, 38, 55, 48, 63, 58, 70, 64, 80, 74, 88, 82, 95, 90, 60, 52, 47, 63, 58, 70, 66, 80, 75, 90, 85, 96, 92, 100, 94],
    '90d': [20, 28, 25, 34, 30, 40, 36, 45, 42, 50, 47, 55, 52, 60, 57, 64, 60, 68, 65, 72, 69, 76, 72, 80, 77, 84, 80, 88, 85, 92, 88, 95, 91, 98, 94, 100, 96, 90, 85, 80, 75, 70, 66, 62, 58, 55, 52, 50, 48, 46, 44, 42, 40, 44, 48, 52, 56, 60, 64, 68, 72, 76, 80, 84, 88, 92, 96, 100, 94, 88, 82, 76, 70, 64, 58, 52, 46, 40, 44, 48, 52, 56, 60, 64, 68, 72, 76, 80, 84, 88]
  };

  chartData = computed(() => {
    const values = this.datasets[this.activePeriod()];
    const w = 560, h = 180, pad = 10;
    const max = Math.max(...values), min = Math.min(...values);
    const stepX = w / (values.length - 1);
    
    const points: [number, number][] = values.map((v, i) => {
      const x = i * stepX;
      const y = h - pad - ((v - min) / (max - min || 1)) * (h - pad * 2);
      return [x, y];
    });

    let line = `M ${points[0][0]} ${points[0][1]}`;
    for (let i = 1; i < points.length; i++) {
      line += ` L ${points[i][0]} ${points[i][1]}`;
    }
    
    const area = `${line} L ${points[points.length - 1][0]} ${h} L 0 ${h} Z`;
    
    return { line, area, points };
  });

  filteredPoints = computed(() => {
    const points = this.chartData().points;
    const showEvery = points.length > 20 ? Math.round(points.length / 10) : 1;
    return points.filter((p, i) => i % showEvery === 0 || i === points.length - 1);
  });

  /** currentStage freezes at whatever stage an application was in when it hit a terminal
   * status (REJECTED/WITHDRAWN never have a "default stage" of their own -- see
   * ApplicationStatus.java's transitionStatus()) -- so currentStage doubles as "how far
   * this application got," including for rejected ones, which is what every count below relies on. */
  private reachedStage(app: ApplicationResponse, stage: ApplicationStage): boolean {
    return compareToCurrentStage(app, stage) <= 0;
  }

  private rejectedAtStage(app: ApplicationResponse, stage: ApplicationStage): boolean {
    return app.currentStatus === ApplicationStatus.REJECTED && app.currentStage === stage;
  }

  kpis = computed(() => {
    const apps = this.state.companyApplications();
    const totalApplications = apps.length;
    const openPositions = this.state.jobs().filter(j => j.status === 'published').length;
    const terminal = new Set([ApplicationStatus.REJECTED, ApplicationStatus.WITHDRAWN, ApplicationStatus.HIRED, ApplicationStatus.OFFER_DECLINED]);
    const inPipeline = apps.filter(a => !terminal.has(a.currentStatus)).length;

    const hired = apps.filter(a => a.currentStatus === ApplicationStatus.HIRED);
    let avgTimeToHireDays: number | null = null;
    if (hired.length > 0) {
      const totalDays = hired.reduce((sum, a) => {
        const days = (new Date(a.lastStatusChangedAt).getTime() - new Date(a.appliedAt).getTime()) / 86400000;
        return sum + Math.max(0, days);
      }, 0);
      avgTimeToHireDays = Math.round(totalDays / hired.length);
    }

    return { totalApplications, openPositions, inPipeline, avgTimeToHireDays };
  });

  funnel = computed(() => {
    const apps = this.state.companyApplications();
    const total = apps.length || 1;
    const row = (count: number) => ({ count, pct: Math.round((count / total) * 100) });

    const aiShortlisted = apps.filter(a =>
      a.currentStatus === ApplicationStatus.SHORTLISTED || this.reachedStage(a, ApplicationStage.AI_INTERVIEW)).length;
    const aiRejected = apps.filter(a => this.rejectedAtStage(a, ApplicationStage.AI_RESUME_SCREENING)).length;
    const interviewPending = apps.filter(a => a.currentStatus === ApplicationStatus.AI_INTERVIEW_PENDING).length;
    const codingPending = apps.filter(a => a.currentStatus === ApplicationStatus.CODING_ASSESSMENT_PENDING).length;
    const codingPassed = apps.filter(a => this.reachedStage(a, ApplicationStage.TECHNICAL_INTERVIEW)).length;
    const codingFailed = apps.filter(a => this.rejectedAtStage(a, ApplicationStage.CODING_ASSESSMENT)).length;
    const technical = apps.filter(a => this.reachedStage(a, ApplicationStage.TECHNICAL_INTERVIEW)).length;
    const hr = apps.filter(a => this.reachedStage(a, ApplicationStage.HR_INTERVIEW)).length;
    const offer = apps.filter(a => this.reachedStage(a, ApplicationStage.OFFER)).length;

    return {
      applicants: row(apps.length),
      aiShortlisted: row(aiShortlisted),
      aiRejected: row(aiRejected),
      interviewPending: row(interviewPending),
      codingPending: row(codingPending),
      codingPassed: row(codingPassed),
      codingFailed: row(codingFailed),
      technical: row(technical),
      hr: row(hr),
      offer: row(offer),
    };
  });

  /** Only counts applications that have actually finished (or been rejected out of) resume
   * screening -- still-in-progress ones (APPLIED/RESUME_PARSING/AI_MATCHING) are excluded so
   * the breakdown reflects decisions made, not the raw applicant pool. */
  resumeScreening = computed(() => {
    const apps = this.state.companyApplications();
    const decided = apps.filter(a =>
      a.currentStatus === ApplicationStatus.SHORTLISTED ||
      a.currentStatus === ApplicationStatus.MANUAL_REVIEW ||
      this.rejectedAtStage(a, ApplicationStage.AI_RESUME_SCREENING) ||
      this.reachedStage(a, ApplicationStage.AI_INTERVIEW));
    const total = decided.length;
    if (total === 0) {
      return { total: 0, shortlistedPct: 0, manualReviewPct: 0, rejectedPct: 0, shortlistedDash: 0, manualReviewDash: 0, rejectedDash: 0 };
    }
    const shortlisted = decided.filter(a => a.currentStatus === ApplicationStatus.SHORTLISTED || this.reachedStage(a, ApplicationStage.AI_INTERVIEW)).length;
    const manualReview = decided.filter(a => a.currentStatus === ApplicationStatus.MANUAL_REVIEW).length;
    const rejected = total - shortlisted - manualReview;

    const circumference = 251.3;
    const shortlistedPct = Math.round((shortlisted / total) * 100);
    const manualReviewPct = Math.round((manualReview / total) * 100);
    const rejectedPct = 100 - shortlistedPct - manualReviewPct;

    return {
      total,
      shortlistedPct, manualReviewPct, rejectedPct,
      shortlistedDash: Math.round((shortlisted / total) * circumference * 10) / 10,
      manualReviewDash: Math.round((manualReview / total) * circumference * 10) / 10,
      rejectedDash: Math.round((rejected / total) * circumference * 10) / 10,
    };
  });

  constructor(public state: AppStateService, public router: Router) {
    // The KPI row, funnel, and resume-screening breakdown all read
    // state.companyApplications() -- load it here so the dashboard shows real,
    // current numbers even when it's the first page opened after login, rather
    // than depending on some other route having already populated it.
    this.state.loadCompanyApplications();
  }

  viewProfile(candId: string) {
    const cand = this.state.candidates().find(c => c.id === candId);
    if (cand) {
      this.state.selectCandidate(cand);
      this.router.navigate(['/recruiter/candidates', candId]);
    }
  }
}
