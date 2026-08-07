import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { CandidateAssessmentStatus, LeaderboardItemResponse } from '../../core/models/coding-assessment.model';
import { SkeletonComponent } from '../../shared/components/skeleton.component';

const STATUS_FILTER_MAP: Record<string, CandidateAssessmentStatus | undefined> = {
  all: undefined,
  sent: CandidateAssessmentStatus.NOT_STARTED,
  'in-progress': CandidateAssessmentStatus.IN_PROGRESS,
  evaluated: CandidateAssessmentStatus.COMPLETED,
  failed: CandidateAssessmentStatus.EXPIRED,
};

@Component({
  selector: 'app-coding-assessments',
  standalone: true,
  imports: [CommonModule, SkeletonComponent],
  template: `
    <div class="coding-assessments-viewport">
      <div class="page-head">
        <div>
          <h1>Coding assessments</h1>
          <p>Track automated code compilation tests, test case passes, and execution times{{ assessmentName() ? ' -- ' + assessmentName() : '' }}.</p>
        </div>
      </div>

      <div class="settings-layout">
        <div class="subnav">
          <button [class.active]="activeTab() === 'queue'" (click)="activeTab.set('queue')">Assessment queue</button>
          <button [class.active]="activeTab() === 'analysis'" (click)="activeTab.set('analysis')">Analysis dashboard</button>
        </div>

        <div>
          <!-- ASSESSMENT QUEUE -->
          <div class="settings-pane active" *ngIf="activeTab() === 'queue'">
            <div class="card">
              <div class="filter-row">
                <div class="filter-tabs">
                  <button [class.active]="queueFilter() === 'all'" (click)="setQueueFilter('all')">All {{ state.codingAssessmentQueue().length }}</button>
                  <button [class.active]="queueFilter() === 'sent'" (click)="setQueueFilter('sent')">Sent</button>
                  <button [class.active]="queueFilter() === 'in-progress'" (click)="setQueueFilter('in-progress')">In progress</button>
                  <button [class.active]="queueFilter() === 'evaluated'" (click)="setQueueFilter('evaluated')">Evaluated</button>
                  <button [class.active]="queueFilter() === 'failed'" (click)="setQueueFilter('failed')">Failed</button>
                </div>
              </div>

              <div class="skeleton-rows" *ngIf="state.codingAssessmentQueueLoading()">
                <div class="skeleton-row" *ngFor="let _ of [1,2,3,4,5]">
                  <app-skeleton width="200px" height="14px" />
                  <app-skeleton width="120px" height="14px" />
                  <app-skeleton width="80px" height="14px" />
                  <app-skeleton width="90px" height="14px" />
                </div>
              </div>
              <table class="table" *ngIf="!state.codingAssessmentQueueLoading()">
                <thead>
                  <tr><th>Candidate</th><th>Job</th><th>Status</th><th>Sent / completed</th><th></th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of state.codingAssessmentQueue()">
                    <td>
                      <div class="cand-cell">
                        <div class="avatar">{{ getInitials(item.candidateName) }}</div>
                        <div><div class="name">{{ item.candidateName }}</div><div class="email">{{ item.candidateEmail }}</div></div>
                      </div>
                    </td>
                    <td class="muted">{{ item.jobTitle }}</td>
                    <td><span class="badge" [ngClass]="getBadgeClass(item.status)">{{ item.status }}</span></td>
                    <td class="muted">{{ item.dueOrCompletedAt | date: 'MMM d, y' }}</td>
                    <td>
                      <span class="row-link" *ngIf="item.status === 'COMPLETED'" (click)="openDrawer(item.applicationId)">View code</span>
                      <span class="row-link" *ngIf="item.status === 'NOT_STARTED'" (click)="remind(item.applicationId)">Send reminder</span>
                      <span class="row-link" *ngIf="item.status === 'EXPIRED'" (click)="remind(item.applicationId)">Resend invite</span>
                      <span class="muted" *ngIf="item.status === 'IN_PROGRESS'">&mdash;</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div class="empty-state" *ngIf="!state.codingAssessmentQueueLoading() && !state.codingAssessmentQueue().length"><p>No candidates in this filter.</p></div>
            </div>
          </div>

          <!-- ANALYSIS DASHBOARD -->
          <div class="settings-pane active" *ngIf="activeTab() === 'analysis'">
            <div class="kpi-row" style="margin-bottom:20px;">
              <div class="kpi-card">
                <div class="kpi-label">Assessments completed</div>
                <div class="kpi-value">{{ state.codingResultsSummary()?.completedCount ?? 0 }}</div>
              </div>
              <div class="kpi-card">
                <div class="kpi-label">Avg. code score</div>
                <div class="kpi-value">{{ state.codingResultsSummary()?.avgScore != null ? (state.codingResultsSummary()!.avgScore | number: '1.0-0') + '%' : '—' }}</div>
              </div>
              <div class="kpi-card">
                <div class="kpi-label">Most used language</div>
                <div class="kpi-value" style="font-size: 20px; font-weight: 600;">{{ state.codingAnalytics()?.mostUsedLanguage ?? '—' }}</div>
              </div>
              <div class="kpi-card">
                <div class="kpi-label">Completion rate</div>
                <div class="kpi-value">{{ state.codingAnalytics()?.completionRatePercent != null ? (state.codingAnalytics()!.completionRatePercent | number: '1.0-0') + '%' : '—' }}</div>
              </div>
            </div>

            <div class="card">
              <div class="card-head"><h2>Candidate ranking</h2></div>
              <table class="table">
                <thead>
                  <tr><th>#</th><th>Candidate</th><th>Job</th><th>Score</th><th>Time used</th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of state.codingLeaderboard()">
                    <td class="muted">{{ item.rank }}</td>
                    <td>
                      <div class="cand-cell">
                        <div class="avatar">{{ getInitials(item.candidateName) }}</div>
                        <div>{{ item.candidateName }}</div>
                      </div>
                    </td>
                    <td class="muted">{{ item.jobTitle }}</td>
                    <td>
                      <span class="badge" [ngClass]="(item.score ?? 0) >= 80 ? 'match-high' : 'match-mid'">{{ item.score ?? '—' }}{{ item.score != null ? '%' : '' }}</span>
                    </td>
                    <td class="muted">{{ formatTime(item.timeUsedSeconds) }}</td>
                  </tr>
                </tbody>
              </table>
              <div class="empty-state" *ngIf="!state.codingLeaderboard().length"><p>No completed submissions yet.</p></div>
            </div>

            <div class="card">
              <div class="card-head"><h2>Completed assessments</h2></div>
              <table class="table">
                <thead>
                  <tr><th>Candidate</th><th>Job</th><th>Score</th><th>Completed</th><th></th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of state.codingCompletedList()">
                    <td>
                      <div class="cand-cell">
                        <div class="avatar">{{ getInitials(item.candidateName) }}</div>
                        <div>{{ item.candidateName }}</div>
                      </div>
                    </td>
                    <td class="muted">{{ item.jobTitle }}</td>
                    <td>
                      <span class="badge" [ngClass]="(item.score ?? 0) >= 80 ? 'match-high' : 'match-mid'">{{ item.score ?? '—' }}{{ item.score != null ? '%' : '' }}</span>
                    </td>
                    <td class="muted">{{ item.completedAt | date: 'MMM d, y' }}</td>
                    <td>
                      <span class="row-link" (click)="openDrawer(item.applicationId)">View code</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div class="empty-state" *ngIf="!state.codingCompletedList().length"><p>Nothing completed yet.</p></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Coding Report Drawer Overlay -->
      <div class="drawer-overlay" [ngClass]="{ 'show': activeApplicationId() }" (click)="closeDrawer()">
        <div class="drawer" (click)="$event.stopPropagation()" style="max-width: 600px;">
          <div class="drawer-head">
            <h3>Coding Assessment Report</h3>
            <button class="close-btn" (click)="closeDrawer()">&times;</button>
          </div>

          <div class="drawer-body" *ngIf="state.codingSubmissionDrawer() as report">
            <div class="card-head" style="margin-bottom:16px;">
              <div>
                <h4 style="font-family:'Fraunces',serif; font-size:17px; margin:0 0 4px;">{{ report.candidateName }}</h4>
                <div style="font-size:12px; color:var(--ink-soft);">{{ report.jobTitle }}</div>
              </div>
              <span class="badge" [ngClass]="(report.scorePercent ?? 0) >= 80 ? 'match-high' : 'match-mid'">
                {{ report.scorePercent ?? '—' }}{{ report.scorePercent != null ? '% score' : '' }}
              </span>
            </div>

            <h5 class="section-label">Execution metrics</h5>
            <div class="match-compare-grid" style="margin-bottom:20px;">
              <div class="mc-card"><div class="mc-label">Test cases</div><div class="mc-value">{{ report.testCaseSummary ?? '—' }}</div></div>
              <div class="mc-card"><div class="mc-label">Time used</div><div class="mc-value">{{ report.timeUsedMinutes ?? '—' }} min</div></div>
              <div class="mc-card"><div class="mc-label">Plagiarism</div><div class="mc-value">{{ report.plagiarismBadge ?? '—' }}</div></div>
              <div class="mc-card"><div class="mc-label">AI review</div><div class="mc-value">{{ report.aiReviewBadge ?? '—' }}</div></div>
            </div>

            <h5 class="section-label" *ngIf="report.antiCheatSignals?.length">Anti-cheat signals</h5>
            <div style="display:flex; flex-direction:column; gap:6px; margin-bottom:20px; font-size:12.5px;" *ngIf="report.antiCheatSignals?.length">
              <div *ngFor="let s of report.antiCheatSignals" style="display:flex; justify-content:space-between; border-bottom:1px solid var(--line-soft); padding:6px 0;">
                <span class="muted">{{ s.label }}</span><span style="font-weight:600;">{{ s.value }}</span>
              </div>
            </div>

            <div *ngFor="let q of report.questions">
              <h5 class="section-label">{{ q.title }} ({{ q.difficulty }})</h5>
              <pre class="code-block"><code>{{ q.code }}</code></pre>
              <div style="display:flex; gap:6px; flex-wrap:wrap; margin-top:8px;">
                <span class="badge" *ngFor="let tc of q.testCases" [ngClass]="tc.passed ? 'published' : 'failed'">{{ tc.label }}</span>
              </div>
            </div>

            <div class="foot-right" style="margin-top:20px;">
              <button class="btn-primary-sm" (click)="moveToNextStage(report.applicationId)">Move to next stage</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .coding-assessments-viewport {
      display: flex;
      flex-direction: column;
      gap: 20px;
      font-family: $font-sans;
    }

    .skeleton-rows {
      display: flex;
      flex-direction: column;
      gap: 18px;
      padding: 16px 0;
    }
    .skeleton-row {
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
    }

    .kpi-row {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 16px;

      @include respond-to('tablet') { grid-template-columns: repeat(2, 1fr); }
      @include respond-to('mobile') { grid-template-columns: 1fr; }
    }

    .kpi-card {
      background: var(--paper-card);
      border: 1px solid var(--line);
      border-radius: 14px;
      padding: 18px 20px;
      display: flex;
      flex-direction: column;
      gap: 4px;

      .kpi-label { font-size: 12px; color: var(--ink-soft); }
      .kpi-value { font-family: $font-serif; font-size: 27px; font-weight: 560; }
    }

    .section-label {
      font-size: 11.5px;
      text-transform: uppercase;
      letter-spacing: 0.05em;
      color: var(--ink-faint);
      margin: 18px 0 8px;
      font-weight: 600;
    }

    .match-compare-grid {
      display: grid;
      grid-template-columns: repeat(4, 1fr);
      gap: 8px;
    }

    .mc-card {
      background: var(--paper);
      border: 1px solid var(--line-soft);
      border-radius: 9px;
      padding: 10px 12px;
      text-align: center;

      .mc-label { font-size: 10px; color: var(--ink-soft); }
      .mc-value { font-size: 12.5px; font-weight: 600; color: var(--indigo); margin-top: 3px; }
    }

    .code-block {
      background: var(--ink);
      color: #eaeaea;
      padding: 14px;
      border-radius: 9px;
      font-family: $font-mono;
      font-size: 12px;
      line-height: 1.5;
      overflow-x: auto;
      margin: 8px 0 0;
    }

    .drawer-overlay {
      position: fixed;
      inset: 0;
      background: rgba(28,27,41,0.22);
      z-index: 999;
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.2s ease;
      &.show { opacity: 1; pointer-events: auto; }
    }

    .drawer {
      position: fixed;
      right: 0;
      top: 0;
      bottom: 0;
      width: 100%;
      max-width: 480px;
      background: var(--paper-card);
      border-left: 1px solid var(--line);
      box-shadow: -10px 0 30px rgba(28,27,41,0.08);
      z-index: 1000;
      transform: translateX(100%);
      transition: transform 0.22s cubic-bezier(0.1, 0.8, 0.2, 1);
      display: flex;
      flex-direction: column;
      &.show { transform: translateX(0); }
    }

    .drawer-head {
      padding: 16px 20px;
      border-bottom: 1px solid var(--line-soft);
      display: flex;
      justify-content: space-between;
      align-items: center;
      h3 { font-family: $font-serif; font-size: 16px; font-weight: 560; color: var(--ink); margin: 0; }
      .close-btn { background: none; border: none; font-size: 22px; color: var(--ink-soft); cursor: pointer; }
    }

    .drawer-body { padding: 20px; flex: 1; overflow-y: auto; }
  `]
})
export class CodingAssessmentsComponent implements OnInit {
  activeTab = signal<string>('queue');
  queueFilter = signal<string>('all');
  activeApplicationId = signal<string | null>(null);
  assessmentId = signal<string | null>(null);

  constructor(public state: AppStateService) {}

  ngOnInit() {
    if (!this.state.codingAssessmentsList().length) this.state.loadCodingAssessments();
    // No assessment selector exists in this screen's original design -- defaults to
    // the first assessment, same convention used across Modules 7/8's job selectors.
    const id = this.state.codingAssessmentsList()[0]?.id ?? null;
    this.assessmentId.set(id);
    if (id) {
      this.state.loadCodingAssessmentQueue(id);
      this.state.loadCodingResultsSummary(id);
      this.state.loadCodingLeaderboard(id);
      this.state.loadCodingCompleted(id);
      this.state.loadCodingAnalytics(id);
    }
  }

  assessmentName(): string {
    return this.state.codingAssessmentsList().find((a) => a.id === this.assessmentId())?.name ?? '';
  }

  setQueueFilter(filter: string) {
    this.queueFilter.set(filter);
    if (this.assessmentId()) this.state.loadCodingAssessmentQueue(this.assessmentId()!, STATUS_FILTER_MAP[filter]);
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  getBadgeClass(status: CandidateAssessmentStatus): string {
    if (status === 'COMPLETED') return 'published';
    if (status === 'IN_PROGRESS') return 'processing';
    if (status === 'NOT_STARTED') return 'draft';
    return 'failed';
  }

  formatTime(seconds?: number): string {
    if (seconds == null) return '—';
    return `${Math.floor(seconds / 60)} min`;
  }

  remind(applicationId: string) {
    if (this.assessmentId()) this.state.remindCodingCandidate(this.assessmentId()!, applicationId);
  }

  openDrawer(applicationId: string) {
    this.activeApplicationId.set(applicationId);
    if (this.assessmentId()) this.state.loadCodingSubmissionDrawer(this.assessmentId()!, applicationId);
  }

  closeDrawer() {
    this.activeApplicationId.set(null);
    this.state.clearCodingSubmissionDrawer();
  }

  moveToNextStage(applicationId: string) {
    if (this.assessmentId()) this.state.moveCodingCandidateToNextStage(this.assessmentId()!, applicationId);
    this.closeDrawer();
  }
}
