import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { ShortlistItemResponse } from '../../core/models/ai-interview.model';
import { SkeletonComponent } from '../../shared/components/skeleton.component';

@Component({
  selector: 'app-shortlisting',
  standalone: true,
  imports: [CommonModule, SkeletonComponent],
  template: `
    <div class="shortlist-viewport">
      <div class="page-head">
        <div>
          <h1>AI shortlisting</h1>
          <p>Candidates automatically sorted by match score, with a queue for human judgment calls{{ jobTitle() ? ' -- ' + jobTitle() : '' }}.</p>
        </div>
      </div>

      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-label">Shortlisted</div>
          <div class="kpi-value">{{ state.aiShortlistSummary()?.shortlistedCount ?? 0 }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Rejected (AI)</div>
          <div class="kpi-value">{{ state.aiShortlistSummary()?.rejectedCount ?? 0 }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Manual review</div>
          <div class="kpi-value">{{ state.aiShortlistSummary()?.manualReviewCount ?? 0 }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Auto-shortlist rate</div>
          <div class="kpi-value">{{ (state.aiShortlistSummary()?.autoShortlistRatePercent ?? 0) | number: '1.0-0' }}%</div>
        </div>
      </div>

      <div class="row-2">
      <div class="card" *ngIf="!state.aiShortlistLoading(); else shortlistSkeleton">
        <div class="filter-row">
          <div class="filter-tabs">
            <button [class.active]="activeTab() === 'shortlisted'" (click)="activeTab.set('shortlisted')">Shortlisted {{ state.aiShortlisted().length }}</button>
            <button [class.active]="activeTab() === 'rejected'" (click)="activeTab.set('rejected')">Rejected {{ state.aiRejected().length }}</button>
            <button [class.active]="activeTab() === 'review'" (click)="activeTab.set('review')">Manual review {{ state.aiManualReview().length }}</button>
          </div>
        </div>

        <!-- SHORTLISTED TAB -->
        <div *ngIf="activeTab() === 'shortlisted'">
          <table class="table">
            <thead>
              <tr><th>Candidate</th><th>Job</th><th>Match score</th><th>Decided</th><th></th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let cand of state.aiShortlisted()">
                <td>
                  <div class="cand-cell">
                    <div class="avatar">{{ getInitials(cand.fullName) }}</div>
                    <div><div class="name">{{ cand.fullName }}</div><div class="email">{{ cand.email }}</div></div>
                  </div>
                </td>
                <td class="muted">{{ cand.jobTitle }}</td>
                <td><span class="badge match-high">{{ cand.overallMatchScore ?? '—' }}{{ cand.overallMatchScore != null ? '%' : '' }}</span></td>
                <td class="muted">{{ cand.decidedAt | date: 'MMM d, y' }}</td>
                <td>
                  <span class="row-link" (click)="state.showToast('Opening candidate profile…')">View</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div class="empty-state" *ngIf="!state.aiShortlisted().length"><p>No shortlisted candidates yet.</p></div>
        </div>

        <!-- REJECTED TAB -->
        <div *ngIf="activeTab() === 'rejected'">
          <table class="table">
            <thead>
              <tr><th>Candidate</th><th>Job</th><th>Match score</th><th>Reason</th><th>Rejected</th></tr>
            </thead>
            <tbody>
              <tr *ngFor="let cand of state.aiRejected()">
                <td>
                  <div class="cand-cell">
                    <div class="avatar">{{ getInitials(cand.fullName) }}</div>
                    <div><div class="name">{{ cand.fullName }}</div><div class="email">{{ cand.email }}</div></div>
                  </div>
                </td>
                <td class="muted">{{ cand.jobTitle }}</td>
                <td><span class="badge match-low">{{ cand.overallMatchScore ?? '—' }}{{ cand.overallMatchScore != null ? '%' : '' }}</span></td>
                <td class="muted">{{ cand.reason ?? '—' }}</td>
                <td class="muted">{{ cand.decidedAt | date: 'MMM d, y' }}</td>
              </tr>
            </tbody>
          </table>
          <div class="empty-state" *ngIf="!state.aiRejected().length"><p>No AI-rejected candidates yet.</p></div>
        </div>

        <!-- MANUAL REVIEW TAB -->
        <div *ngIf="activeTab() === 'review'">
          <div class="bulk-bar" *ngIf="selectedIds().size > 0">
            <span class="bb-count">{{ selectedIds().size }} selected</span>
            <div class="bb-actions">
              <button class="primary" (click)="bulkShortlist()">Shortlist selected</button>
              <button class="danger-o" (click)="bulkReject()">Reject selected</button>
            </div>
          </div>

          <table class="table">
            <thead>
              <tr>
                <th style="width:34px;"><input type="checkbox" class="row-checkbox" [checked]="isAllSelected()" (change)="toggleSelectAll($event)"></th>
                <th>Candidate</th><th>Job</th><th>Match score</th><th>Why it's borderline</th><th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let cand of state.aiManualReview()">
                <td><input type="checkbox" class="row-checkbox" [checked]="selectedIds().has(cand.applicationId)" (change)="toggleSelect(cand, $event)"></td>
                <td>
                  <div class="cand-cell">
                    <div class="avatar">{{ getInitials(cand.fullName) }}</div>
                    <div><div class="name">{{ cand.fullName }}</div><div class="email">{{ cand.email }}</div></div>
                  </div>
                </td>
                <td class="muted">{{ cand.jobTitle }}</td>
                <td><span class="badge match-mid">{{ cand.overallMatchScore ?? '—' }}{{ cand.overallMatchScore != null ? '%' : '' }}</span></td>
                <td class="muted">{{ cand.reason ?? '—' }}</td>
                <td>
                  <span class="row-link" (click)="shortlistOne(cand)">Shortlist</span>
                  <span class="row-link" style="margin-left: 8px;" (click)="rejectOne(cand)">Reject</span>
                </td>
              </tr>
            </tbody>
          </table>
          <div class="empty-state" *ngIf="!state.aiManualReview().length"><p>Nothing needs manual review right now.</p></div>
        </div>
      </div>
      <ng-template #shortlistSkeleton>
        <div class="card">
          <div class="skeleton-rows">
            <div class="skeleton-row" *ngFor="let _ of [1,2,3,4,5]">
              <app-skeleton width="200px" height="14px" />
              <app-skeleton width="120px" height="14px" />
              <app-skeleton width="80px" height="14px" />
              <app-skeleton width="70px" height="14px" />
            </div>
          </div>
        </div>
      </ng-template>

      <!-- Right analytics column -->
      <div class="card">
        <div class="card-head"><h2>Shortlisting analytics</h2></div>
        <div class="donut-row">
          <svg width="92" height="92" viewBox="0 0 100 100">
            <circle cx="50" cy="50" r="40" fill="none" stroke="#EFEBDF" stroke-width="14"/>
            <circle cx="50" cy="50" r="40" fill="none" stroke="#1F7A6C" stroke-width="14" [attr.stroke-dasharray]="shortlistedArc() + ' ' + 251.3" transform="rotate(-90 50 50)"/>
            <circle cx="50" cy="50" r="40" fill="none" stroke="#C79A2E" stroke-width="14" [attr.stroke-dasharray]="manualReviewArc() + ' ' + 251.3" [attr.stroke-dashoffset]="-shortlistedArc()" transform="rotate(-90 50 50)"/>
            <circle cx="50" cy="50" r="40" fill="none" stroke="#B3412C" stroke-width="14" opacity="0.55" [attr.stroke-dasharray]="rejectedArc() + ' ' + 251.3" [attr.stroke-dashoffset]="-(shortlistedArc() + manualReviewArc())" transform="rotate(-90 50 50)"/>
          </svg>
          <div class="legend">
            <div class="legend-item"><span class="legend-dot" style="background:#1F7A6C;"></span>Shortlisted<b>{{ pct(state.aiShortlistSummary()?.shortlistedCount) }}%</b></div>
            <div class="legend-item"><span class="legend-dot" style="background:#C79A2E;"></span>Manual review<b>{{ pct(state.aiShortlistSummary()?.manualReviewCount) }}%</b></div>
            <div class="legend-item"><span class="legend-dot" style="background:#B3412C; opacity:.55;"></span>Rejected<b>{{ pct(state.aiShortlistSummary()?.rejectedCount) }}%</b></div>
          </div>
        </div>

        <!--
          "Shortlist rate by job" is left out here rather than shown with fabricated
          numbers: ShortlistSummaryResponse is scoped to one job (autoShortlistRatePercent
          above), and there's no endpoint that returns a per-job breakdown across every
          open job in one call -- would need N calls (one per job) to build honestly.
        -->
      </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .shortlist-viewport {
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

    .row-2 {
      display: grid;
      grid-template-columns: 2.2fr 1fr;
      gap: 16px;
      align-items: start;

      @include respond-to('mobile') {
        grid-template-columns: 1fr;
      }
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

    .bulk-bar {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 12px;
      background: var(--indigo-deep);
      color: var(--paper);
      border-radius: 11px;
      padding: 11px 16px;
      margin-bottom: 14px;

      .bb-count { font-size: 12.5px; font-weight: 600; }
      .bb-actions { display: flex; gap: 8px; }
      .bb-actions button {
        font-size: 12px;
        font-weight: 500;
        padding: 7px 13px;
        border-radius: 7px;
        cursor: pointer;
        border: 1px solid rgba(250, 249, 244, 0.28);
        background: transparent;
        color: var(--paper);
        font-family: $font-sans;
      }
      .bb-actions button.primary { background: var(--gold); border-color: var(--gold); color: var(--ink); }
      .bb-actions button.danger-o { border-color: rgba(240, 161, 154, 0.5); color: #F0A19A; }
    }
  `]
})
export class ShortlistingComponent implements OnInit {
  activeTab = signal<string>('shortlisted');
  selectedIds = signal<Set<string>>(new Set());
  jobId = signal<string | null>(null);

  constructor(public state: AppStateService) {}

  ngOnInit() {
    if (!this.state.jobs().length) this.state.loadJobs();
    // No job selector exists in this screen's original design -- defaults to the
    // first open job, same convention as Resume Matching before its selector.
    const jobId = this.state.jobs()[0]?.id ?? null;
    this.jobId.set(jobId);
    if (jobId) {
      this.state.loadAiShortlisted(jobId);
      this.state.loadAiRejected(jobId);
      this.state.loadAiManualReview(jobId);
      this.state.loadAiShortlistSummary(jobId);
    }
  }

  jobTitle(): string {
    return this.state.jobs().find((j) => j.id === this.jobId())?.title ?? '';
  }

  private totalDecided(): number {
    const s = this.state.aiShortlistSummary();
    return s ? s.shortlistedCount + s.rejectedCount + s.manualReviewCount : 0;
  }

  pct(count: number | undefined): number {
    const total = this.totalDecided();
    return total && count ? Math.round((count / total) * 100) : 0;
  }

  private arcFor(count: number | undefined): number {
    const total = this.totalDecided();
    return total && count ? (count / total) * 251.3 : 0;
  }

  shortlistedArc(): number {
    return this.arcFor(this.state.aiShortlistSummary()?.shortlistedCount);
  }

  manualReviewArc(): number {
    return this.arcFor(this.state.aiShortlistSummary()?.manualReviewCount);
  }

  rejectedArc(): number {
    return this.arcFor(this.state.aiShortlistSummary()?.rejectedCount);
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  toggleSelect(cand: ShortlistItemResponse, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedIds.update((ids) => {
      const next = new Set(ids);
      checked ? next.add(cand.applicationId) : next.delete(cand.applicationId);
      return next;
    });
  }

  toggleSelectAll(event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedIds.set(checked ? new Set(this.state.aiManualReview().map((c) => c.applicationId)) : new Set());
  }

  isAllSelected(): boolean {
    return this.state.aiManualReview().length > 0 && this.selectedIds().size === this.state.aiManualReview().length;
  }

  shortlistOne(cand: ShortlistItemResponse) {
    if (this.jobId()) this.state.shortlistOne(cand.applicationId, this.jobId()!);
  }

  rejectOne(cand: ShortlistItemResponse) {
    if (this.jobId()) this.state.rejectOneManual(cand.applicationId, this.jobId()!);
  }

  bulkShortlist() {
    if (this.jobId()) {
      this.state.bulkShortlist(Array.from(this.selectedIds()), this.jobId()!);
      this.selectedIds.set(new Set());
    }
  }

  bulkReject() {
    if (this.jobId()) {
      this.state.bulkReject(Array.from(this.selectedIds()), this.jobId()!);
      this.selectedIds.set(new Set());
    }
  }
}
