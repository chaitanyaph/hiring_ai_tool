import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { ResumeMatchRankingItemResponse } from '../../core/models/resume-parser.model';

@Component({
  selector: 'app-matching',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="matching-viewport">
      <div class="page-head">
        <div>
          <h1>Resume matching</h1>
          <p>AI match scores between candidates and job requirements.</p>
        </div>
        <div class="page-head-actions">
          <select class="filter-select" [value]="selectedJobId()" (change)="onJobChange($any($event.target).value)">
            <option *ngFor="let job of state.jobs()" [value]="job.id">{{ job.title }}</option>
          </select>
        </div>
      </div>

      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-label">Candidates matched</div>
          <div class="kpi-value">{{ state.matchSummary()?.analyzedCount ?? 0 }}</div>
          <div class="kpi-trend"><span class="muted">for {{ selectedJobTitle() }}</span></div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Avg. match score</div>
          <div class="kpi-value">{{ state.matchSummary()?.averageMatchScore != null ? (state.matchSummary()!.averageMatchScore | number: '1.0-0') + '%' : '—' }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Top score</div>
          <div class="kpi-value">{{ state.matchSummary()?.topMatchScore ?? '—' }}{{ state.matchSummary()?.topMatchScore != null ? '%' : '' }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Below 70% threshold</div>
          <div class="kpi-value">{{ state.matchSummary()?.belowThresholdCount ?? 0 }}</div>
          <div class="kpi-trend"><span class="muted">auto-excluded from shortlist</span></div>
        </div>
      </div>

      <div class="card">
        <div class="card-head">
          <h2>Candidate ranking</h2>
          <span class="badge stage">{{ selectedJobTitle() }}</span>
        </div>

        <table class="table" *ngIf="state.matchRanking().length > 0; else emptyRanking">
          <thead>
            <tr>
              <th>#</th>
              <th>Candidate</th>
              <th>Match score</th>
              <th>Experience</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let cand of state.matchRanking(); let i = index">
              <td class="muted">{{ i + 1 }}</td>
              <td>
                <div class="cand-cell">
                  <div class="avatar">{{ cand.fullName ? getInitials(cand.fullName) : '—' }}</div>
                  <div>
                    <div class="name">{{ cand.fullName ?? 'Analysis pending' }}</div>
                    <div class="email">{{ cand.email ?? '' }}</div>
                  </div>
                </div>
              </td>
              <td>
                <span class="badge" [ngClass]="cand.overallMatchScore == null ? '' : (cand.overallMatchScore >= 85 ? 'match-high' : (cand.overallMatchScore >= 70 ? 'match-mid' : 'match-low'))" *ngIf="cand.overallMatchScore != null; else noScore">
                  {{ cand.overallMatchScore }}%
                </span>
                <ng-template #noScore><span class="muted">{{ cand.status }}</span></ng-template>
              </td>
              <td>
                <span class="badge" [ngClass]="expClass(cand)">{{ cand.experienceMatchLabel ?? '—' }}</span>
              </td>
              <td>
                <span class="row-link" (click)="openDrawer(cand.applicationId)">View analysis</span>
              </td>
            </tr>
          </tbody>
        </table>
        <ng-template #emptyRanking>
          <div class="empty-state"><p>No candidates analyzed yet for this job.</p></div>
        </ng-template>
      </div>

      <!-- Match Analysis Drawer Overlay -->
      <div class="drawer-overlay" [ngClass]="{ 'show': activeApplicationId() }" (click)="closeDrawer()">
        <div class="drawer" (click)="$event.stopPropagation()">
          <ng-container *ngIf="state.matchAnalysisDetail() as analysis">
            <div class="drawer-head">
              <div>
                <h3 style="font-family:'Fraunces',serif; font-size:17.5px; font-weight:600; margin:0 0 3px;">
                  {{ analysis.fullName ?? 'Candidate' }}
                </h3>
                <p style="font-size:11.5px; color:var(--ink-soft); margin:0;">
                  {{ selectedJobTitle() }} &middot; Match analysis
                </p>
              </div>
              <button class="modal-close" aria-label="Close panel" (click)="closeDrawer()">
                <svg viewBox="0 0 24 24" width="14" height="14" style="stroke:currentColor; fill:none; stroke-width:2.2;"><path d="M6 6l12 12M18 6L6 18"/></svg>
              </button>
            </div>

            <div class="drawer-body">
              <section style="display:flex; align-items:center; gap:16px; margin-bottom:20px;">
                <div class="profile-ring">
                  <svg viewBox="0 0 64 64" width="48" height="48">
                    <circle class="ring-bg" cx="32" cy="32" r="28" fill="none" stroke="var(--line-soft)" stroke-width="5"/>
                    <circle class="ring-fg" cx="32" cy="32" r="28" fill="none" stroke="#1F7A6C" stroke-width="5"
                            [attr.stroke-dasharray]="175.9" [attr.stroke-dashoffset]="175.9 * (1 - (analysis.overallMatchScore ?? 0)/100)"/>
                  </svg>
                  <div class="ring-label" style="color:#1F7A6C; font-family:'IBM Plex Mono',monospace; font-weight:600; font-size:12px;">{{ analysis.overallMatchScore ?? '—' }}{{ analysis.overallMatchScore != null ? '%' : '' }}</div>
                </div>
                <div style="font-size:12px; color:var(--ink-soft); line-height:1.4;">
                  Overall match score &mdash; computed from skills, experience and education alignment against this job's requirements.
                </div>
              </section>

              <section style="margin-bottom:20px;">
                <div class="drawer-section-title" style="font-size:11.5px; text-transform:uppercase; letter-spacing:0.04em; font-weight:600; color:var(--ink-faint); margin-bottom:8px;">Skill match analysis</div>
                <div style="font-size:11px; color:var(--ink-soft); margin-bottom:6px;">Matched skills</div>
                <div class="chip-group" style="margin-bottom:12px;">
                  <span class="skill-pill" style="background:var(--teal-tint); color:var(--teal); font-size:11px; font-weight:500; padding:4px 8px; border-radius:6px; margin-right:6px;" *ngFor="let s of analysis.matchedSkills">
                    ✓ {{ s.skillName }}
                  </span>
                  <span class="muted" *ngIf="!analysis.matchedSkills.length">None yet.</span>
                </div>
                <div style="font-size:11px; color:var(--ink-soft); margin-bottom:6px;">Missing skills</div>
                <div class="chip-group">
                  <span class="skill-pill" style="background:var(--danger-tint); color:var(--danger); font-size:11px; font-weight:500; padding:4px 8px; border-radius:6px; margin-right:6px;" *ngFor="let s of analysis.missingSkills">
                    &times; {{ s.skillName }}
                  </span>
                  <span class="muted" *ngIf="!analysis.missingSkills.length">None.</span>
                </div>
              </section>

              <section style="margin-bottom:20px;">
                <div class="drawer-section-title" style="font-size:11.5px; text-transform:uppercase; letter-spacing:0.04em; font-weight:600; color:var(--ink-faint); margin-bottom:8px;">Experience &amp; education match</div>
                <div class="match-compare-grid" style="display:grid; grid-template-columns:1fr 1fr; gap:8px;">
                  <div class="mc-card" style="background:var(--paper); border:1px solid var(--line-soft); border-radius:8px; padding:10px;">
                    <div class="mc-label" style="font-size:10.5px; color:var(--ink-soft);">Experience match</div>
                    <div class="mc-value" style="font-size:13px; font-weight:600; color:var(--ink); margin-top:2px;">{{ analysis.experienceMatchLabel ?? '—' }}</div>
                  </div>
                  <div class="mc-card" style="background:var(--paper); border:1px solid var(--line-soft); border-radius:8px; padding:10px;">
                    <div class="mc-label" style="font-size:10.5px; color:var(--ink-soft);">Education match</div>
                    <div class="mc-value" style="font-size:13px; font-weight:600; color:var(--indigo); margin-top:2px;">{{ analysis.educationMatchLabel ?? '—' }}</div>
                  </div>
                </div>
              </section>

              <section style="margin-bottom:20px;">
                <div class="drawer-section-title" style="font-size:11.5px; text-transform:uppercase; letter-spacing:0.04em; font-weight:600; color:var(--ink-faint); margin-bottom:8px;">Strengths &amp; weaknesses</div>
                <div class="sw-list" style="display:flex; flex-direction:column; gap:6px;">
                  <div *ngFor="let s of analysis.strengths" style="font-size:12px; color:var(--ink-soft); line-height:1.4; display:flex; gap:6px; align-items:start;">
                    <span style="color:var(--teal);">✓</span> {{ s.description }}
                  </div>
                  <div *ngFor="let w of analysis.weaknesses" style="font-size:12px; color:var(--ink-soft); line-height:1.4; display:flex; gap:6px; align-items:start;">
                    <span style="color:var(--danger);">&times;</span> {{ w.description }}
                  </div>
                </div>
              </section>

              <section style="margin-bottom:20px;" *ngIf="analysis.aiRecommendation as rec">
                <div class="drawer-section-title" style="font-size:11.5px; text-transform:uppercase; letter-spacing:0.04em; font-weight:600; color:var(--ink-faint); margin-bottom:8px;">AI explanation</div>
                <div class="bubble-ai" style="margin:0; background:var(--indigo-tint); border:1px solid var(--line-soft); border-radius:8px; padding:10px 12px; font-size:12px; line-height:1.5; color:var(--ink-soft);">
                  {{ rec.overallAiSummary }}
                </div>
              </section>
            </div>

            <div class="drawer-foot">
              <button class="btn-ghost" (click)="state.showToast('Added to shortlist')">Shortlist</button>
              <div class="foot-right">
                <button class="btn-ghost" (click)="closeDrawer()">Close</button>
                <button class="btn-primary-sm" (click)="state.showToast('Opening full profile…')">View full profile</button>
              </div>
            </div>
          </ng-container>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .matching-viewport {
      display: flex;
      flex-direction: column;
      gap: 20px;
      font-family: $font-sans;
    }
  `]
})
export class MatchingComponent implements OnInit {
  selectedJobId = signal<string | null>(null);
  activeApplicationId = signal<string | null>(null);

  constructor(public state: AppStateService) {}

  ngOnInit() {
    if (!this.state.jobs().length) this.state.loadJobs();
    const first = this.state.jobs()[0]?.id ?? null;
    this.selectedJobId.set(first);
    if (first) this.loadForJob(first);
  }

  selectedJobTitle = computed(() => this.state.jobs().find((j) => j.id === this.selectedJobId())?.title ?? '—');

  onJobChange(jobId: string) {
    this.selectedJobId.set(jobId);
    this.loadForJob(jobId);
  }

  private loadForJob(jobId: string) {
    this.state.loadMatchRanking(jobId);
    this.state.loadMatchSummary(jobId);
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  expClass(cand: ResumeMatchRankingItemResponse): string {
    if (cand.experienceMatchLabel === 'STRONG') return 'published';
    if (cand.experienceMatchLabel === 'ADEQUATE') return 'stage';
    if (cand.experienceMatchLabel === 'BELOW_RANGE') return 'archived';
    return '';
  }

  openDrawer(applicationId: string) {
    this.activeApplicationId.set(applicationId);
    this.state.loadMatchAnalysisDetail(applicationId);
  }

  closeDrawer() {
    this.activeApplicationId.set(null);
    this.state.clearMatchAnalysisDetail();
  }
}
