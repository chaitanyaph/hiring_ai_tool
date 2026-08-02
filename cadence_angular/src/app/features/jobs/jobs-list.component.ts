import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';

@Component({
  selector: 'app-jobs-list',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="jobs-viewport">

      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>Jobs</h1>
          <p>{{ allCount() }} job{{ allCount() === 1 ? '' : 's' }} across {{ state.departments().length }} department{{ state.departments().length === 1 ? '' : 's' }}</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-ghost" (click)="state.showToast('Opening templates…')">Templates</button>
          <button class="btn-primary-sm" (click)="state.openModal('job')">
            <svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg>
            New job
          </button>
        </div>
      </div>

      <!-- Main Layout Card (containing both filter row and table) -->
      <div class="card">
        <!-- Filter Row -->
        <div class="filter-row">
          <!-- Filter Tabs -->
          <div class="filter-tabs">
            <button
              [class.active]="activeTab() === 'All'"
              (click)="activeTab.set('All')">
              All {{ allCount() }}
            </button>
            <button
              [class.active]="activeTab() === 'Published'"
              (click)="activeTab.set('Published')">
              Published {{ publishedCount() }}
            </button>
            <button
              [class.active]="activeTab() === 'Draft'"
              (click)="activeTab.set('Draft')">
              Draft {{ draftCount() }}
            </button>
            <button
              [class.active]="activeTab() === 'Archived'"
              (click)="activeTab.set('Archived')">
              Archived {{ archivedCount() }}
            </button>
          </div>

          <!-- Search Inline -->
          <div class="search-inline">
            <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8" fill="none"/></svg>
            <input
              placeholder="Search jobs…"
              #searchVal
              [value]="searchQuery()"
              (input)="searchQuery.set(searchVal.value)"
            >
          </div>

          <!-- Department filter -->
          <select class="filter-select" (change)="selectedDept.set($any($event.target).value)">
            <option value="All departments">All departments</option>
            <option *ngFor="let d of state.departments()" [value]="d.departmentName">{{ d.departmentName }}</option>
          </select>
        </div>

        <!-- Jobs Table -->
        <table class="table" *ngIf="filteredJobs().length > 0; else emptyState">
          <thead>
            <tr>
              <th>Job title</th>
              <th>Location</th>
              <th>Type</th>
              <th>Openings</th>
              <th>Applicants</th>
              <th>Status</th>
              <th>Posted</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let job of filteredJobs()">
              <!-- Title & Dept -->
              <td>
                <b style="font-weight:600; color:var(--ink); cursor:pointer;" (click)="state.showToast('Viewing job details…')">
                  {{ job.title }}
                </b>
                <div class="muted" style="font-size:11.5px; margin-top:2px;">
                  {{ job.department }}
                </div>
              </td>

              <!-- Location -->
              <td class="muted">{{ job.location }}</td>

              <!-- Type -->
              <td class="muted">{{ job.empType }}</td>

              <!-- Openings -->
              <td>{{ job.openings }}</td>

              <!-- Applicants & Stage Badge -->
              <td>
                <span>{{ job.applicants }}</span>
                <span class="badge stage" style="margin-left: 6px;">Screening</span>
              </td>

              <!-- Status badge -->
              <td>
                <span class="badge" [ngClass]="job.status">
                  {{ job.status === 'published' ? 'Published' : (job.status === 'draft' ? 'Draft' : 'Archived') }}
                </span>
              </td>

              <!-- Posted -->
              <td class="muted">{{ job.posted }}</td>

              <!-- Actions -->
              <td style="text-align:right; white-space:nowrap; vertical-align:middle;">
                <span class="row-link" (click)="editJob(job.id)">Edit</span>
                <span *ngIf="job.status === 'published'" class="row-link" style="margin-left:12px;" (click)="archiveJob(job.id)">Archive</span>
                <span *ngIf="job.status === 'draft'" class="row-link" style="margin-left:12px;" (click)="publishJob(job.id)">Publish</span>
                <span *ngIf="job.status === 'archived'" class="row-link" style="margin-left:12px;" (click)="restoreJob(job.id)">Restore</span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Empty state -->
        <ng-template #emptyState>
          <div class="empty-state">
            <svg viewBox="0 0 24 24" fill="none">
              <circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8"/>
              <path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
            </svg>
            <p>No jobs match your filters.</p>
            <button (click)="clearFilters()">Clear filters</button>
          </div>
        </ng-template>
      </div>

    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .jobs-viewport {
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 18px;
      font-family: $font-sans;
    }
  `]
})
export class JobsListComponent {
  searchQuery  = signal<string>('');
  activeTab    = signal<string>('All');
  selectedDept = signal<string>('All departments');

  constructor(public state: AppStateService) {}

  allCount = computed(() => this.state.jobs().length);
  publishedCount = computed(() => this.state.jobs().filter(j => j.status === 'published').length);
  draftCount = computed(() => this.state.jobs().filter(j => j.status === 'draft').length);
  archivedCount = computed(() => this.state.jobs().filter(j => j.status === 'archived').length);

  filteredJobs = computed(() => {
    const q   = this.searchQuery().toLowerCase();
    const tab = this.activeTab();
    const dept = this.selectedDept();

    return this.state.jobs().filter(job => {
      const matchesSearch = job.title.toLowerCase().includes(q) || job.department.toLowerCase().includes(q);
      const matchesTab    = tab === 'All' || job.status.toLowerCase() === tab.toLowerCase();
      const matchesDept   = dept === 'All departments' || job.department === dept;
      return matchesSearch && matchesTab && matchesDept;
    });
  });

  editJob(jobId: string) {
    this.state.openModal('job');
  }

  archiveJob(jobId: string) {
    if (confirm('Archive this job? Candidates already in the pipeline keep their status.')) {
      this.state.archiveJob(jobId);
    }
  }

  publishJob(jobId: string) {
    this.state.publishJob(jobId);
  }

  restoreJob(jobId: string) {
    this.state.restoreJob(jobId);
  }

  clearFilters() {
    this.searchQuery.set('');
    this.activeTab.set('All');
    this.selectedDept.set('All departments');
  }
}
