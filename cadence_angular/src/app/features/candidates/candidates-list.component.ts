import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { ApplicationResponse } from '../../core/models/application.model';
import { applicationStageLabel } from '../../core/utils/application.mapper';

@Component({
  selector: 'app-candidates-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="candidates-viewport">

      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>Candidates</h1>
          <p>{{ state.companyApplications().length }} in pipeline</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-ghost" (click)="state.openModal('candidate')">Import resumes</button>
          <button class="btn-primary-sm" (click)="state.showToast('Opening candidate comparison…')">
            <svg viewBox="0 0 24 24" width="14" height="14" style="stroke:var(--paper-card); fill:none; stroke-width:2.2; margin-right:6px;"><path d="M7 3v18M17 3v18M3 8h4M3 16h4M17 8h4M17 16h4"/></svg>
            Compare
          </button>
        </div>
      </div>

      <!-- Card Container -->
      <div class="card">
        <!-- Filter Row -->
        <div class="filter-row" style="margin-bottom: 16px;">
          <!-- Search -->
          <div class="search-inline" style="max-width:220px;">
            <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8" fill="none"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8"/></svg>
            <input
              type="text"
              placeholder="Search candidates…"
              #searchVal
              (input)="searchQuery.set(searchVal.value)"
            >
          </div>

          <!-- Stage filter -->
          <select class="filter-select" (change)="selectedStage.set($any($event.target).value)">
            <option *ngFor="let st of stageOptions" [value]="st">{{ st }}</option>
          </select>

          <!-- Job filter -->
          <select class="filter-select" (change)="selectedJob.set($any($event.target).value)">
            <option *ngFor="let j of jobOptions()" [value]="j">{{ j }}</option>
          </select>
        </div>

        <table class="table" *ngIf="filteredApplications().length > 0; else emptyState">
          <thead>
            <tr>
              <th>Candidate</th>
              <th>Applied for</th>
              <th>Match score</th>
              <th>Stage</th>
              <th>Applied</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let app of filteredApplications()">
              <!-- Candidate cell -->
              <td>
                <div class="cand-cell">
                  <div class="avatar">{{ getInitials(app.candidateNameSnapshot) }}</div>
                  <div>
                    <div class="name">{{ app.candidateNameSnapshot }}</div>
                    <div class="email">{{ app.candidateEmailSnapshot }}</div>
                  </div>
                </div>
              </td>

              <!-- Applied for -->
              <td class="muted">{{ app.jobTitleSnapshot }}</td>

              <!-- Match Score -->
              <td>
                <span class="badge" [ngClass]="(app.overallScore ?? 0) >= 88 ? 'match-high' : 'match-mid'" *ngIf="app.overallScore != null; else noScore">
                  {{ app.overallScore }}%
                </span>
                <ng-template #noScore><span class="muted">—</span></ng-template>
              </td>

              <!-- Stage -->
              <td>
                <span class="badge stage">{{ stageLabel(app) }}</span>
              </td>

              <!-- Applied -->
              <td class="muted">{{ app.appliedAt | date: 'MMM d, y' }}</td>

              <!-- Actions -->
              <td>
                <span class="row-link" (click)="viewProfile(app)">View</span>
              </td>
            </tr>
          </tbody>
        </table>

        <ng-template #emptyState>
          <div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8"/>
            </svg>
            <p>No candidates match your filters.</p>
            <button class="btn-text" (click)="clearFilters()">Clear filters</button>
          </div>
        </ng-template>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .candidates-viewport {
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 18px;
      font-family: $font-sans;
    }

    .muted {
      color: var(--ink-soft);
    }
  `]
})
export class CandidatesListComponent implements OnInit {
  searchQuery = signal<string>('');
  selectedStage = signal<string>('All stages');
  selectedJob = signal<string>('All jobs');

  stageOptions = ['All stages', 'Resume screening', 'AI Interview', 'Coding assessment', 'Technical Round', 'HR round', 'Offer'];

  constructor(public state: AppStateService, private router: Router) {}

  ngOnInit() {
    this.state.loadCompanyApplications();
  }

  /** Derived from whatever jobs are actually present in the loaded pipeline, rather than a fixed illustrative list. */
  jobOptions = computed(() => ['All jobs', ...new Set(this.state.companyApplications().map((a) => a.jobTitleSnapshot))]);

  filteredApplications = computed(() => {
    const q = this.searchQuery().toLowerCase();
    const stage = this.selectedStage();
    const job = this.selectedJob();
    return this.state.companyApplications().filter((app) => {
      const matchesSearch = app.candidateNameSnapshot.toLowerCase().includes(q) || app.candidateEmailSnapshot.toLowerCase().includes(q);
      const matchesStage = stage === 'All stages' || this.stageLabel(app) === stage;
      const matchesJob = job === 'All jobs' || app.jobTitleSnapshot === job;
      return matchesSearch && matchesStage && matchesJob;
    });
  });

  stageLabel(app: ApplicationResponse): string {
    return applicationStageLabel(app.currentStage);
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  viewProfile(app: ApplicationResponse) {
    this.state.selectedApplication.set(app);
    this.router.navigate(['/recruiter/candidates', app.id]);
  }

  clearFilters() {
    this.searchQuery.set('');
    this.selectedStage.set('All stages');
    this.selectedJob.set('All jobs');
  }
}
