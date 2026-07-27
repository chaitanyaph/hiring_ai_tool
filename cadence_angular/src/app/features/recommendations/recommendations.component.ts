import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { ResumeMatchRankingItemResponse, ResumeMatchResponse } from '../../core/models/resume-parser.model';

@Component({
  selector: 'app-recommendations',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="recommendations-viewport">
      <div class="page-head">
        <div>
          <h1>AI recommendations</h1>
          <p>AI-curated top candidates across your open roles.</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-ghost" [disabled]="selectedIds().size < 2" (click)="openCompareModal()">
            Compare ({{ selectedIds().size }})
          </button>
        </div>
      </div>

      <div class="filter-row">
        <select class="filter-select" (change)="onJobFilter($any($event.target).value)">
          <option value="">All jobs</option>
          <option *ngFor="let job of state.jobs()" [value]="job.id">{{ job.title }}</option>
        </select>
        <select class="filter-select" (change)="onDeptFilter($any($event.target).value)">
          <option value="">All departments</option>
          <option *ngFor="let dept of state.departments()" [value]="dept.id">{{ dept.departmentName }}</option>
        </select>
        <select class="filter-select" (change)="onMinScoreFilter($any($event.target).value)">
          <option value="">Min. score: Any</option>
          <option value="80">80%+</option>
          <option value="90">90%+</option>
        </select>
      </div>

      <div class="rec-card-grid" *ngIf="state.recommendations().length > 0; else emptyRecs">
        <div class="rec-card-lg" *ngFor="let item of state.recommendations()" [class.selected]="selectedIds().has(item.applicationId)">
          <input type="checkbox" class="row-checkbox rcl-select" [checked]="selectedIds().has(item.applicationId)" (change)="toggleSelect(item, $event)">
          <div class="rec-card-lg-top">
            <div class="avatar">{{ item.fullName ? getInitials(item.fullName) : '—' }}</div>
            <div>
              <div class="rcl-name">{{ item.fullName ?? 'Analysis pending' }}</div>
              <div class="rcl-role">{{ item.overallMatchScore ?? '—' }}{{ item.overallMatchScore != null ? '% match' : '' }}</div>
            </div>
          </div>
          <div class="rec-card-lg-actions">
            <button (click)="openDrawer(item.applicationId)">Details</button>
            <button class="primary" (click)="state.showToast('Added to shortlist')">Shortlist</button>
          </div>
        </div>
      </div>
      <ng-template #emptyRecs>
        <div class="empty-state"><p>No recommendations yet -- analysis hasn't completed for any application in this filter.</p></div>
      </ng-template>

      <!-- Recommendation details drawer overlay -->
      <div class="drawer-overlay" [ngClass]="{ 'show': activeApplicationId() }" (click)="closeDrawer()">
        <div class="drawer" (click)="$event.stopPropagation()">
          <ng-container *ngIf="state.matchAnalysisDetail() as analysis">
            <div class="drawer-head">
              <div>
                <h3 style="font-family:'Fraunces',serif; font-size:17px; font-weight:600; margin:0 0 3px;">
                  {{ analysis.fullName ?? 'Candidate' }}
                </h3>
                <p style="font-size:11.5px; color:var(--ink-soft); margin:0;">Why we recommend this candidate</p>
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
                  AI recommendation score &mdash; same match engine as Resume Matching, re-ranked across all your open roles.
                </div>
              </section>

              <section style="margin-bottom:20px;">
                <div class="drawer-section-title" style="font-size:11.5px; text-transform:uppercase; letter-spacing:0.04em; font-weight:600; color:var(--ink-faint); margin-bottom:8px;">Skill match</div>
                <div style="font-size:11px; color:var(--ink-soft); margin-bottom:6px;">Matched skills</div>
                <div class="chip-group" style="margin-bottom:12px;">
                  <span class="skill-pill" style="background:var(--teal-tint); color:var(--teal); font-size:11px; font-weight:500; padding:4px 8px; border-radius:6px; margin-right:6px;" *ngFor="let s of analysis.matchedSkills">
                    ✓ {{ s.skillName }}
                  </span>
                </div>
                <div style="font-size:11px; color:var(--ink-soft); margin-bottom:6px;">Missing skills</div>
                <div class="chip-group">
                  <span class="skill-pill" style="background:var(--danger-tint); color:var(--danger); font-size:11px; font-weight:500; padding:4px 8px; border-radius:6px; margin-right:6px;" *ngFor="let s of analysis.missingSkills">
                    &times; {{ s.skillName }}
                  </span>
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
              <button class="btn-ghost" (click)="state.showToast('Recommendation dismissed')">Dismiss</button>
              <div class="foot-right">
                <button class="btn-ghost" (click)="state.showToast('Interview scheduled')">Schedule interview</button>
                <button class="btn-primary-sm" (click)="state.showToast('Added to shortlist'); closeDrawer();">Shortlist</button>
              </div>
            </div>
          </ng-container>
        </div>
      </div>

      <!-- Candidate Comparison Modal -->
      <div class="modal-overlay" [ngClass]="{ 'show': showCompareModal() }">
        <div class="modal" style="max-width:800px; width:90%;">
          <div class="modal-head">
            <h3>Candidate comparison</h3>
            <button class="close-btn" (click)="showCompareModal.set(false)">&times;</button>
          </div>
          <div class="modal-body" style="overflow-x:auto;">
            <table class="table" style="min-width:600px;">
              <thead>
                <tr>
                  <th>Metric</th>
                  <th *ngFor="let item of selectedItems()">
                    <div style="font-weight:600;">{{ item.fullName ?? '—' }}</div>
                    <span class="badge" [ngClass]="(item.overallMatchScore ?? 0) >= 85 ? 'match-high' : 'match-mid'" style="margin-top:4px; display:inline-block;">
                      {{ item.overallMatchScore ?? '—' }}{{ item.overallMatchScore != null ? '% match' : '' }}
                    </span>
                  </th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td><b>Experience match</b></td>
                  <td *ngFor="let item of selectedItems()" class="muted">{{ compareCache().get(item.applicationId)?.experienceMatchLabel ?? '—' }}</td>
                </tr>
                <tr>
                  <td><b>Education match</b></td>
                  <td *ngFor="let item of selectedItems()" class="muted">{{ compareCache().get(item.applicationId)?.educationMatchLabel ?? '—' }}</td>
                </tr>
                <tr>
                  <td><b>Matched skills</b></td>
                  <td *ngFor="let item of selectedItems()">
                    <div class="chip-group">
                      <span class="skill-pill" style="background:var(--teal-tint); color:var(--teal);" *ngFor="let s of compareCache().get(item.applicationId)?.matchedSkills">
                        {{ s.skillName }}
                      </span>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td><b>Missing skills</b></td>
                  <td *ngFor="let item of selectedItems()">
                    <div class="chip-group">
                      <span class="skill-pill" style="background:var(--danger-tint); color:var(--danger);" *ngFor="let s of compareCache().get(item.applicationId)?.missingSkills">
                        {{ s.skillName }}
                      </span>
                    </div>
                  </td>
                </tr>
                <tr>
                  <td><b>Core Strength</b></td>
                  <td *ngFor="let item of selectedItems()" class="muted" style="font-size:11.5px; line-height:1.4;">
                    {{ compareCache().get(item.applicationId)?.strengths?.[0]?.description ?? '—' }}
                  </td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .recommendations-viewport {
      display: flex;
      flex-direction: column;
      gap: 20px;
      font-family: $font-sans;
    }

    .rec-card-grid {
      display: grid;
      grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
      gap: 14px;
    }

    .rec-card-lg {
      background: var(--paper-card);
      border: 1px solid var(--line);
      border-radius: 14px;
      padding: 16px;
      position: relative;
      display: flex;
      flex-direction: column;
      justify-content: space-between;
      @include transition-base;

      &.selected {
        border-color: var(--indigo);
        box-shadow: 0 0 0 1px var(--indigo);
      }

      .rcl-select {
        position: absolute;
        top: 14px;
        right: 14px;
      }

      .rec-card-lg-top {
        display: flex;
        gap: 12px;
        align-items: center;
        margin-bottom: 10px;
        padding-right: 24px;
      }

      .avatar {
        width: 32px;
        height: 32px;
        border-radius: 50%;
        background: var(--indigo-tint);
        color: var(--indigo);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 11px;
        font-weight: 700;
        flex-shrink: 0;
      }

      .rcl-name {
        font-family: $font-serif;
        font-size: 14.5px;
        font-weight: 600;
        color: var(--ink);
      }

      .rcl-role {
        font-size: 11.5px;
        color: var(--ink-soft);
        margin-top: 1px;
      }

      .rec-card-lg-actions {
        display: flex;
        gap: 8px;
        padding-top: 12px;
        border-top: 1px solid var(--line-soft);
        margin-top: auto;

        button {
          font-size: 11.5px;
          font-weight: 500;
          padding: 7px 11px;
          border-radius: 7px;
          border: 1px solid var(--line);
          background: var(--paper-card);
          color: var(--ink-soft);
          cursor: pointer;
          flex: 1;
          text-align: center;
          font-family: $font-sans;
          @include transition-base;

          &:hover:not(.primary) {
            border-color: var(--indigo);
            color: var(--indigo);
          }

          &.primary {
            background: var(--indigo);
            border-color: var(--indigo);
            color: #fff;

            &:hover {
              background: var(--indigo-deep);
              border-color: var(--indigo-deep);
            }
          }
        }
      }
    }
  `]
})
export class RecommendationsComponent implements OnInit {
  activeApplicationId = signal<string | null>(null);
  showCompareModal = signal<boolean>(false);
  selectedIds = signal<Set<string>>(new Set());
  compareCache = signal<Map<string, ResumeMatchResponse>>(new Map());

  jobFilter: string | undefined;
  deptFilter: string | undefined;
  minScoreFilter: number | undefined;

  constructor(public state: AppStateService) {}

  ngOnInit() {
    if (!this.state.jobs().length) this.state.loadJobs();
    if (!this.state.departments().length) this.state.loadDepartments();
    this.state.loadRecommendations();
  }

  private reload() {
    this.state.loadRecommendations(this.jobFilter, this.deptFilter, this.minScoreFilter);
  }

  onJobFilter(jobId: string) {
    this.jobFilter = jobId || undefined;
    this.reload();
  }

  onDeptFilter(deptId: string) {
    this.deptFilter = deptId || undefined;
    this.reload();
  }

  onMinScoreFilter(minScore: string) {
    this.minScoreFilter = minScore ? Number(minScore) : undefined;
    this.reload();
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  toggleSelect(item: ResumeMatchRankingItemResponse, event: Event) {
    const checked = (event.target as HTMLInputElement).checked;
    this.selectedIds.update((ids) => {
      const next = new Set(ids);
      checked ? next.add(item.applicationId) : next.delete(item.applicationId);
      return next;
    });
  }

  selectedItems = () => this.state.recommendations().filter((r) => this.selectedIds().has(r.applicationId));

  openDrawer(applicationId: string) {
    this.activeApplicationId.set(applicationId);
    this.state.loadMatchAnalysisDetail(applicationId);
  }

  closeDrawer() {
    this.activeApplicationId.set(null);
    this.state.clearMatchAnalysisDetail();
  }

  openCompareModal() {
    this.showCompareModal.set(true);
    for (const item of this.selectedItems()) {
      if (!this.compareCache().has(item.applicationId)) {
        this.state.fetchMatchAnalysis(item.applicationId).subscribe((analysis) => {
          this.compareCache.update((cache) => new Map(cache).set(item.applicationId, analysis));
        });
      }
    }
  }
}
