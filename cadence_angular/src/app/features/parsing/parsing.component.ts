import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { ParsingQueueItemResponse, ParsingStatus } from '../../core/models/resume-parser.model';

/** Bucketed for the 4 filter tabs the existing UI already has -- EXTRACTING_TEXT/PARSING_FIELDS both read as "processing". */
function bucketOf(status: ParsingStatus): 'queued' | 'processing' | 'parsed' | 'failed' {
  if (status === ParsingStatus.QUEUED) return 'queued';
  if (status === ParsingStatus.PARSED) return 'parsed';
  if (status === ParsingStatus.FAILED) return 'failed';
  return 'processing';
}

@Component({
  selector: 'app-parsing',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="parsing-viewport">
      <div class="page-head">
        <div>
          <h1>Resume parsing</h1>
          <p>Automatic AI extraction from every uploaded resume.</p>
        </div>
      </div>

      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-label">Queued</div>
          <div class="kpi-value">{{ state.parsingQueueSummary()?.queuedCount ?? 0 }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Processing</div>
          <div class="kpi-value">{{ state.parsingQueueSummary()?.processingCount ?? 0 }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Parsed today</div>
          <div class="kpi-value">{{ state.parsingQueueSummary()?.parsedTodayCount ?? 0 }}</div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Failed</div>
          <div class="kpi-value">{{ state.parsingQueueSummary()?.failedCount ?? 0 }}</div>
        </div>
      </div>

      <div class="card">
        <div class="filter-row">
          <div class="filter-tabs">
            <button [class.active]="activeFilter() === 'all'" (click)="setFilter('all')">All {{ state.parsingQueue().length }}</button>
            <button [class.active]="activeFilter() === 'queued'" (click)="setFilter('queued')">Queued</button>
            <button [class.active]="activeFilter() === 'processing'" (click)="setFilter('processing')">Processing</button>
            <button [class.active]="activeFilter() === 'parsed'" (click)="setFilter('parsed')">Parsed</button>
            <button [class.active]="activeFilter() === 'failed'" (click)="setFilter('failed')">Failed</button>
          </div>
          <div class="search-inline">
            <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4"/></svg>
            <input placeholder="Search candidate…" #searchVal (input)="searchQuery.set(searchVal.value)">
          </div>
        </div>

        <table class="table">
          <thead>
            <tr>
              <th>Candidate</th>
              <th>Resume ID</th>
              <th>Status</th>
              <th>Progress</th>
              <th>Submitted</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of filteredItems()">
              <td>
                <div class="cand-cell">
                  <div class="avatar">{{ item.fullName ? getInitials(item.fullName) : '—' }}</div>
                  <div>
                    <div class="name">{{ item.fullName ?? 'Not parsed yet' }}</div>
                    <div class="email">{{ item.email ?? '' }}</div>
                  </div>
                </div>
              </td>
              <td class="muted" style="font-family:monospace; font-size:11px;">{{ item.resumeId.slice(0, 8) }}…</td>
              <td>
                <span class="badge" [ngClass]="getBadgeClass(item.status)">
                  {{ item.status }}
                </span>
              </td>
              <td>
                <div class="mini-progress-track" *ngIf="bucket(item) === 'processing'">
                  <div class="mini-progress-fill" [style.width.%]="item.progressPercent ?? 50"></div>
                </div>
                <span class="muted" *ngIf="bucket(item) !== 'processing'">&mdash;</span>
              </td>
              <td class="muted">{{ item.submittedAt | date: 'MMM d, h:mm a' }}</td>
              <td>
                <span class="row-link" (click)="openDrawer(item)">View</span>
                <span class="row-link" *ngIf="item.status === 'FAILED'" style="margin-left: 8px;" (click)="retry(item)">Retry</span>
              </td>
            </tr>
          </tbody>
        </table>

        <div class="empty-state" *ngIf="filteredItems().length === 0">
          <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4"/></svg>
          <p>No resumes match your filters.</p>
          <button (click)="clearFilters()">Clear filters</button>
        </div>
      </div>

      <!-- Detail/Logs Drawer Overlay -->
      <div class="drawer-overlay" [ngClass]="{ 'show': activeResumeId() }" (click)="closeDrawer()">
        <div class="drawer" (click)="$event.stopPropagation()">
          <div class="drawer-head">
            <div>
              <h3>{{ state.parsedResumeDetail()?.fullName ?? 'Parsing detail' }}</h3>
              <p><span class="badge" [ngClass]="getBadgeClass(state.parsedResumeDetail()?.status)">{{ state.parsedResumeDetail()?.status }}</span></p>
            </div>
            <button class="modal-close" aria-label="Close panel" (click)="closeDrawer()">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>

          <div class="drawer-body" *ngIf="state.parsedResumeDetail() as detail">
            <section>
              <div class="drawer-section-title">Processing progress</div>
              <div class="status-stepper">
                <div class="status-step" [class.done]="stepDone(detail.status, 1)" [class.current]="stepCurrent(detail.status, 1)">
                  <div class="ss-dot"><svg *ngIf="stepDone(detail.status, 1)" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5" stroke="#fff" stroke-width="2.6" fill="none"/></svg></div>
                  <div class="ss-label">Uploaded</div>
                </div>
                <div class="status-step" [class.done]="stepDone(detail.status, 2)" [class.current]="stepCurrent(detail.status, 2)">
                  <div class="ss-dot"><svg *ngIf="stepDone(detail.status, 2)" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5" stroke="#fff" stroke-width="2.6" fill="none"/></svg></div>
                  <div class="ss-label">Extracting text</div>
                </div>
                <div class="status-step" [class.done]="stepDone(detail.status, 3)" [class.current]="stepCurrent(detail.status, 3)" [class.failed]="detail.status === 'FAILED'">
                  <div class="ss-dot">
                    <svg *ngIf="stepDone(detail.status, 3)" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5" stroke="#fff" stroke-width="2.6" fill="none"/></svg>
                    <svg *ngIf="detail.status === 'FAILED'" viewBox="0 0 24 24"><path d="M18 6L6 18M6 6l12 12" stroke="#fff" stroke-width="2.8" fill="none"/></svg>
                  </div>
                  <div class="ss-label">Parsing fields</div>
                </div>
                <div class="status-step" [class.done]="stepDone(detail.status, 4)">
                  <div class="ss-dot"><svg *ngIf="stepDone(detail.status, 4)" viewBox="0 0 24 24"><path d="M20 6L9 17l-5-5" stroke="#fff" stroke-width="2.6" fill="none"/></svg></div>
                  <div class="ss-label">Complete</div>
                </div>
              </div>
            </section>

            <div *ngIf="detail.status !== 'FAILED'">
              <section *ngIf="detail.professionalSummary">
                <div class="drawer-section-title">Resume summary</div>
                <p style="font-size:12.5px; color:var(--ink-soft); line-height:1.6;">{{ detail.professionalSummary }}</p>
              </section>
              <section *ngIf="detail.skills?.length">
                <div class="drawer-section-title">Extracted skills</div>
                <div class="chip-group">
                  <span class="skill-pill" *ngFor="let s of detail.skills">{{ s.skillName }}</span>
                </div>
              </section>
              <section *ngIf="detail.experience?.length">
                <div class="drawer-section-title">Experience</div>
                <div class="extracted-item" *ngFor="let exp of detail.experience">
                  <div class="ei-title">{{ exp.designation }}</div>
                  <div class="ei-sub">{{ exp.companyName }} · {{ exp.startDate ?? '—' }} – {{ exp.current ? 'Present' : (exp.endDate ?? '—') }}</div>
                </div>
              </section>
              <section *ngIf="detail.education?.length">
                <div class="drawer-section-title">Education</div>
                <div class="extracted-item" *ngFor="let edu of detail.education">
                  <div class="ei-title">{{ edu.degree }}{{ edu.fieldOfStudy ? ', ' + edu.fieldOfStudy : '' }}</div>
                  <div class="ei-sub">{{ edu.institutionName }} · {{ edu.startDate ?? '—' }} – {{ edu.endDate ?? '—' }}</div>
                </div>
              </section>
              <section *ngIf="detail.projects?.length">
                <div class="drawer-section-title">Projects</div>
                <div class="extracted-item" *ngFor="let proj of detail.projects">
                  <div class="ei-title">{{ proj.projectName }}</div>
                  <div class="ei-sub">{{ proj.description }}</div>
                </div>
              </section>
              <section *ngIf="detail.certifications?.length">
                <div class="drawer-section-title">Certifications</div>
                <div class="extracted-item" *ngFor="let cert of detail.certifications">
                  <div class="ei-title">{{ cert.certificationName }}</div>
                  <div class="ei-sub">{{ cert.issuingOrganization }}{{ cert.issuedDate ? ' · ' + cert.issuedDate : '' }}</div>
                </div>
              </section>
            </div>

            <div *ngIf="detail.status === 'FAILED'">
              <section>
                <div class="demo-hint" style="border-color:var(--danger); color:var(--danger); font-size:12.5px; padding:12px; border-radius:8px; background:rgba(179, 65, 44, 0.05); border:1px solid rgba(179,65,44,0.18);">
                  <b>Parsing failed.</b> {{ detail.failureReason ?? 'Unknown reason.' }}
                </div>
              </section>
            </div>

            <section>
              <div class="drawer-section-title" style="display:flex; justify-content:space-between; align-items:center;">
                Parsing logs
                <a class="card-link" (click)="showLogs.set(!showLogs())">Show / hide</a>
              </div>
              <div class="parsing-log" [ngClass]="{ 'show': showLogs() }" style="display:block;">
                <div *ngFor="let log of state.parsingLogs()">{{ log.createdAt | date: 'HH:mm:ss' }} [{{ log.logLevel }}] {{ log.message }}</div>
                <div *ngIf="!state.parsingLogs().length" class="muted">No logs yet.</div>
              </div>
            </section>
          </div>

          <div class="drawer-foot">
            <span></span>
            <div class="foot-right" *ngIf="state.parsedResumeDetail()?.status !== 'FAILED'">
              <button class="btn-ghost" (click)="closeDrawer()">Close</button>
            </div>
            <div class="foot-right" *ngIf="state.parsedResumeDetail()?.status === 'FAILED'">
              <button class="btn-ghost" (click)="closeDrawer()">Close</button>
              <button class="btn-primary-sm" (click)="retryFromDrawer()">Retry parsing</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .parsing-viewport {
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
    }

    .mini-progress-track {
      width: 100px;
      height: 6px;
      background: var(--line-soft);
      border-radius: 3px;
      overflow: hidden;
    }

    .mini-progress-fill {
      height: 100%;
      background: var(--indigo);
      border-radius: 3px;
    }
  `]
})
export class ParsingComponent implements OnInit {
  activeFilter = signal<string>('all');
  searchQuery = signal<string>('');
  activeResumeId = signal<string | null>(null);
  showLogs = signal<boolean>(true);

  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadParsingQueue();
    this.state.loadParsingQueueSummary();
  }

  setFilter(val: string) {
    this.activeFilter.set(val);
  }

  clearFilters() {
    this.activeFilter.set('all');
    this.searchQuery.set('');
  }

  bucket(item: ParsingQueueItemResponse) {
    return bucketOf(item.status);
  }

  filteredItems = computed(() => {
    const f = this.activeFilter();
    const q = this.searchQuery().toLowerCase();
    return this.state.parsingQueue().filter((item) => {
      const matchesFilter = f === 'all' || bucketOf(item.status) === f;
      const matchesSearch = !q || (item.fullName ?? '').toLowerCase().includes(q) || (item.email ?? '').toLowerCase().includes(q);
      return matchesFilter && matchesSearch;
    });
  });

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  getBadgeClass(status?: ParsingStatus): string {
    if (status === ParsingStatus.PARSED) return 'published';
    if (status === ParsingStatus.FAILED) return 'failed';
    if (status === ParsingStatus.QUEUED) return 'draft';
    return 'processing';
  }

  openDrawer(item: ParsingQueueItemResponse) {
    this.activeResumeId.set(item.resumeId);
    this.state.loadParsedResumeDetail(item.resumeId);
    this.state.loadParsingLogs(item.resumeId);
  }

  closeDrawer() {
    this.activeResumeId.set(null);
    this.state.clearParsedResumeDetail();
  }

  retry(item: ParsingQueueItemResponse) {
    this.state.retryParsing(item.resumeId);
  }

  retryFromDrawer() {
    const id = this.activeResumeId();
    if (id) {
      this.state.retryParsing(id);
      this.closeDrawer();
    }
  }

  /** Maps a status to its 1-based step number on the 4-step stepper (FAILED reads as stuck at step 3, same as the backend's own doc comment). */
  private stepNumber(status: ParsingStatus): number {
    switch (status) {
      case ParsingStatus.QUEUED: return 1;
      case ParsingStatus.EXTRACTING_TEXT: return 2;
      case ParsingStatus.PARSING_FIELDS: return 3;
      case ParsingStatus.PARSED: return 4;
      case ParsingStatus.FAILED: return 3;
    }
  }

  stepDone(status: ParsingStatus | undefined, step: number): boolean {
    if (!status) return false;
    if (status === ParsingStatus.FAILED) return step < 3;
    return this.stepNumber(status) > step || (this.stepNumber(status) === step && status === ParsingStatus.PARSED);
  }

  stepCurrent(status: ParsingStatus | undefined, step: number): boolean {
    if (!status || status === ParsingStatus.FAILED) return false;
    return this.stepNumber(status) === step && status !== ParsingStatus.PARSED;
  }
}
