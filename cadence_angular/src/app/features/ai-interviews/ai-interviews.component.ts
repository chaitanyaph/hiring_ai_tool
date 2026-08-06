import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { InterviewCompletedItemResponse, InterviewQueueItemResponse, InterviewSessionStatus } from '../../core/models/ai-interview.model';

const STATUS_FILTER_MAP: Record<string, InterviewSessionStatus | undefined> = {
  all: undefined,
  'not-started': InterviewSessionStatus.NOT_STARTED,
  'in-progress': InterviewSessionStatus.IN_PROGRESS,
  completed: InterviewSessionStatus.COMPLETED,
  expired: InterviewSessionStatus.EXPIRED,
};

@Component({
  selector: 'app-ai-interviews',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="ai-interviews-viewport">
      <div class="page-head">
        <div>
          <h1>AI interviews</h1>
          <p>Track who still needs an AI interview, and review completed evaluations.</p>
        </div>
      </div>

      <div class="settings-layout">
        <div class="subnav">
          <button [class.active]="activeTab() === 'queue'" (click)="selectTab('queue')">Interview queue</button>
          <button [class.active]="activeTab() === 'analysis'" (click)="selectTab('analysis')">Analysis dashboard</button>
        </div>

        <div>
          <!-- INTERVIEW QUEUE -->
          <div class="settings-pane active" *ngIf="activeTab() === 'queue'">
            <div class="card">
              <div class="filter-row">
                <div class="filter-tabs">
                  <button [class.active]="queueFilter() === 'all'" (click)="setQueueFilter('all')">All {{ state.aiInterviewQueue().length }}</button>
                  <button [class.active]="queueFilter() === 'not-started'" (click)="setQueueFilter('not-started')">Not started</button>
                  <button [class.active]="queueFilter() === 'in-progress'" (click)="setQueueFilter('in-progress')">In progress</button>
                  <button [class.active]="queueFilter() === 'completed'" (click)="setQueueFilter('completed')">Completed</button>
                  <button [class.active]="queueFilter() === 'expired'" (click)="setQueueFilter('expired')">Expired</button>
                </div>
              </div>

              <table class="table">
                <thead>
                  <tr><th>Candidate</th><th>Job</th><th>Status</th><th>Due / completed</th><th></th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of state.aiInterviewQueue()">
                    <td>
                      <div class="cand-cell">
                        <div class="avatar">{{ getInitials(item.fullName) }}</div>
                        <div><div class="name">{{ item.fullName }}</div><div class="email">{{ item.email }}</div></div>
                      </div>
                    </td>
                    <td class="muted">{{ item.jobTitle }}</td>
                    <td>
                      <span class="badge" [ngClass]="getBadgeClass(item.status)">{{ item.status }}</span>
                    </td>
                    <td class="muted">{{ dueOrCompleted(item) | date: 'MMM d, y' }}</td>
                    <td>
                      <span class="row-link" *ngIf="item.status === 'COMPLETED'" (click)="openDrawer(item.applicationId)">View report</span>
                      <span class="row-link" *ngIf="item.status === 'NOT_STARTED'" (click)="sendReminder(item)">Send reminder</span>
                      <span class="row-link" *ngIf="item.status === 'EXPIRED'" (click)="resendInvite(item)">Resend invite</span>
                      <span class="muted" *ngIf="item.status === 'IN_PROGRESS'">&mdash;</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div class="empty-state" *ngIf="!state.aiInterviewQueue().length"><p>No interviews in this filter.</p></div>
            </div>
          </div>

          <!-- ANALYSIS DASHBOARD -->
          <div class="settings-pane active" *ngIf="activeTab() === 'analysis'">
            <div class="kpi-row" style="margin-bottom:20px;">
              <div class="kpi-card">
                <div class="kpi-label">Interviews completed</div>
                <div class="kpi-value">{{ state.aiInterviewAnalysisSummary()?.completedCount ?? 0 }}</div>
                <div class="kpi-trend"><span class="muted">{{ state.aiInterviewAnalysisSummary()?.completedThisWeekCount ?? 0 }} this week</span></div>
              </div>
              <div class="kpi-card">
                <div class="kpi-label">Avg. overall rating</div>
                <div class="kpi-value">{{ state.aiInterviewAnalysisSummary()?.avgOverallScore ?? '—' }}</div>
              </div>
              <div class="kpi-card">
                <div class="kpi-label">Avg. communication score</div>
                <div class="kpi-value">{{ state.aiInterviewAnalysisSummary()?.avgCommunicationScore ?? '—' }}</div>
              </div>
              <div class="kpi-card">
                <div class="kpi-label">Flagged for review</div>
                <div class="kpi-value">{{ state.aiInterviewAnalysisSummary()?.flaggedForReviewCount ?? 0 }}</div>
              </div>
            </div>

            <div class="card">
              <div class="card-head"><h2>Completed interviews</h2></div>
              <table class="table">
                <thead>
                  <tr><th>Candidate</th><th>Job</th><th>Overall rating</th><th>Recommendation</th><th>Completed</th><th></th></tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of state.aiCompletedInterviews()">
                    <td>
                      <div class="cand-cell">
                        <div class="avatar">{{ getInitials(item.fullName) }}</div>
                        <div>{{ item.fullName }}</div>
                      </div>
                    </td>
                    <td class="muted">{{ item.jobTitle }}</td>
                    <td>
                      <span class="badge" [ngClass]="(item.overallScore ?? 0) >= 75 ? 'match-high' : 'match-mid'">
                        {{ item.overallScore ?? '—' }}
                      </span>
                    </td>
                    <td>
                      <span class="badge" [ngClass]="item.hiringRecommendation === 'PROCEED' ? 'published' : (item.hiringRecommendation === 'HOLD' ? 'draft' : 'failed')">
                        {{ item.hiringRecommendation }}
                      </span>
                    </td>
                    <td class="muted">{{ item.completedAt | date: 'MMM d, y' }}</td>
                    <td>
                      <span class="row-link" (click)="openDrawer(item.applicationId)">View report</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div class="empty-state" *ngIf="!state.aiCompletedInterviews().length"><p>No completed interviews yet.</p></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Interview Report Drawer Overlay -->
      <div class="drawer-overlay" [ngClass]="{ 'show': activeApplicationId() }" (click)="closeDrawer()">
        <div class="drawer" (click)="$event.stopPropagation()">
          <div class="drawer-head">
            <h3>AI interview evaluation</h3>
            <button class="close-btn" (click)="closeDrawer()">&times;</button>
          </div>

          <div class="drawer-body" *ngIf="state.aiInterviewReport() as report">
            <div class="card-head" style="margin-bottom:16px;">
              <div>
                <h4 style="font-family:'Fraunces',serif; font-size:17px; margin:0 0 4px;">{{ report.fullName }}</h4>
                <div style="font-size:12px; color:var(--ink-soft);">{{ report.jobTitle }}</div>
              </div>
              <span class="badge" [ngClass]="(report.overallScore ?? 0) >= 75 ? 'match-high' : 'match-mid'">
                {{ report.overallScore ?? '—' }} overall
              </span>
            </div>

            <h5 class="section-label">Ratings breakdown</h5>
            <div class="match-compare-grid" style="margin-bottom:20px;">
              <div class="mc-card"><div class="mc-label">Communication</div><div class="mc-value">{{ report.communicationScore ?? '—' }}</div></div>
              <div class="mc-card"><div class="mc-label">Confidence</div><div class="mc-value">{{ report.confidenceScore ?? '—' }}</div></div>
              <div class="mc-card"><div class="mc-label">Technical accuracy</div><div class="mc-value">{{ report.technicalAccuracyScore ?? '—' }}</div></div>
            </div>

            <h5 class="section-label">Behavioral metrics</h5>
            <div style="display:flex; flex-direction:column; gap:6px; margin-bottom:20px; font-size:12.5px;">
              <div style="display:flex; justify-content:space-between; border-bottom:1px solid var(--line-soft); padding:6px 0;">
                <span class="muted">Eye contact / engagement</span><span style="font-weight:600; font-style:italic; color:var(--ink-faint);" title="No audio/video capture pipeline exists on this platform -- not measurable.">Not available</span>
              </div>
              <div style="display:flex; justify-content:space-between; border-bottom:1px solid var(--line-soft); padding:6px 0;">
                <span class="muted">Speaking pace</span><span style="font-weight:600; font-style:italic; color:var(--ink-faint);" title="No audio/video capture pipeline exists on this platform -- not measurable.">Not available</span>
              </div>
              <div style="display:flex; justify-content:space-between; border-bottom:1px solid var(--line-soft); padding:6px 0;">
                <span class="muted">Filler words</span><span style="font-weight:600;">{{ report.fillerWordCount ?? '—' }}</span>
              </div>
              <div style="display:flex; justify-content:space-between; border-bottom:1px solid var(--line-soft); padding:6px 0;">
                <span class="muted">Avg. response latency</span><span style="font-weight:600;">{{ report.avgResponseLatencySeconds != null ? report.avgResponseLatencySeconds + 's' : '—' }}</span>
              </div>
            </div>

            <h5 class="section-label">Hiring recommendation</h5>
            <div class="tl-card" style="margin-bottom:20px;" [style.border-color]="report.hiringRecommendation === 'PROCEED' ? 'var(--teal)' : 'var(--gold)'" [style.background]="report.hiringRecommendation === 'PROCEED' ? 'var(--teal-tint)' : 'var(--gold-tint)'">
              <div style="font-weight:600;" [style.color]="report.hiringRecommendation === 'PROCEED' ? 'var(--teal)' : '#8A6A1F'">
                {{ report.hiringRecommendation }}
              </div>
              <div style="font-size:11.5px; color:var(--ink-soft); margin-top:2px;">{{ report.recruiterSummary ?? report.interviewSummary }}</div>
            </div>

            <h5 class="section-label" *ngIf="report.strengths?.length || report.weaknesses?.length">Strengths &amp; weaknesses</h5>
            <div style="display:flex; flex-direction:column; gap:6px; margin-bottom:20px; font-size:12px;">
              <div *ngFor="let s of report.strengths" style="display:flex; gap:6px;"><span style="color:var(--teal);">✓</span>{{ s }}</div>
              <div *ngFor="let w of report.weaknesses" style="display:flex; gap:6px;"><span style="color:var(--danger);">&times;</span>{{ w }}</div>
            </div>

            <h5 class="section-label">Interview transcript snippet</h5>
            <div class="parsing-log show" style="max-height:220px;">
              <div *ngFor="let turn of report.transcript" style="margin-bottom:8px;">
                <b [style.color]="turn.speaker === 'AI' ? 'var(--teal)' : 'var(--indigo)'">
                  {{ turn.speaker === 'AI' ? 'AI Assistant' : 'Candidate' }}:
                </b>
                <span style="font-size:12px; color:var(--ink-soft);">{{ turn.text }}</span>
              </div>
            </div>

            <div class="foot-right" style="display:flex; gap:8px; margin-top:20px;">
              <button class="btn-ghost" (click)="moveToCoding(report.applicationId)">Move to coding</button>
              <button class="btn-ghost" (click)="flagForReview(report.applicationId)">Manual review</button>
              <button class="btn-primary-sm" style="background:var(--danger); border-color:var(--danger);" (click)="rejectCandidate(report.applicationId)">Reject</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .ai-interviews-viewport {
      display: flex;
      flex-direction: column;
      gap: 20px;
      font-family: $font-sans;
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
      .kpi-trend { font-size: 12px; color: var(--ink-soft); display: flex; align-items: center; gap: 4px; }
      .kpi-trend.up { color: var(--teal); }
      svg { width: 14px; height: 14px; }
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
      grid-template-columns: repeat(3, 1fr);
      gap: 8px;
    }

    .mc-card {
      background: var(--paper);
      border: 1px solid var(--line-soft);
      border-radius: 9px;
      padding: 10px 12px;
      text-align: center;

      .mc-label { font-size: 10px; color: var(--ink-soft); }
      .mc-value { font-size: 13.5px; font-weight: 600; color: var(--indigo); margin-top: 3px; }
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
export class AiInterviewsComponent implements OnInit {
  activeTab = signal<string>('queue');
  queueFilter = signal<string>('all');
  activeApplicationId = signal<string | null>(null);
  jobId = signal<string | null>(null);

  constructor(public state: AppStateService) {}

  ngOnInit() {
    if (!this.state.jobs().length) this.state.loadJobs();
    // No job selector exists in this screen's original design -- defaults to the
    // first open job, same convention as Resume Matching/AI Shortlisting.
    const jobId = this.state.jobs()[0]?.id ?? null;
    this.jobId.set(jobId);
    if (jobId) {
      this.state.loadAiInterviewQueue(jobId);
      this.state.loadAiInterviewAnalysisSummary(jobId);
      this.state.loadAiCompletedInterviews(jobId);
    }
  }

  selectTab(tab: string) {
    this.activeTab.set(tab);
  }

  setQueueFilter(filter: string) {
    this.queueFilter.set(filter);
    if (this.jobId()) this.state.loadAiInterviewQueue(this.jobId()!, STATUS_FILTER_MAP[filter]);
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  getBadgeClass(status: InterviewSessionStatus): string {
    if (status === 'COMPLETED') return 'published';
    if (status === 'IN_PROGRESS') return 'processing';
    if (status === 'NOT_STARTED') return 'draft';
    return 'failed';
  }

  dueOrCompleted(item: InterviewQueueItemResponse): string | undefined {
    return item.completedAt ?? item.expiresAt ?? item.startedAt ?? item.invitedAt;
  }

  sendReminder(item: InterviewQueueItemResponse) {
    this.state.sendAiInterviewReminder(item.applicationId);
  }

  resendInvite(item: InterviewQueueItemResponse) {
    if (this.jobId()) this.state.resendAiInterviewInvite(item.applicationId, this.jobId()!);
  }

  openDrawer(applicationId: string) {
    this.activeApplicationId.set(applicationId);
    this.state.loadAiInterviewReport(applicationId);
  }

  closeDrawer() {
    this.activeApplicationId.set(null);
    this.state.clearAiInterviewReport();
  }

  moveToCoding(applicationId: string) {
    this.state.moveAiInterviewToCoding(applicationId);
    this.closeDrawer();
  }

  flagForReview(applicationId: string) {
    this.state.flagAiInterviewForManualReview(applicationId);
    this.closeDrawer();
  }

  rejectCandidate(applicationId: string) {
    this.state.rejectAfterAiInterview(applicationId);
    this.closeDrawer();
  }
}
