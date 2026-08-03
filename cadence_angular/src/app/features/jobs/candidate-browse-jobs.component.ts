import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { JobService } from '../../core/services/job.service';
import { CandidateJobSummaryResponse } from '../../core/models/job.model';

@Component({
  selector: 'app-candidate-browse-jobs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-jobs">
      <div class="page-head">
        <div>
          <h1>Browse jobs</h1>
          <p>Published openings across every company on the platform.</p>
        </div>
      </div>

      <div class="card">
        <div class="filter-row">
          <div class="filter-tabs" id="cjobs-filter-tabs">
            <button
              [class.active]="activeTab() === 'all'"
              (click)="activeTab.set('all')">
              All {{ allJobs().length }}
            </button>
            <button
              [class.active]="activeTab() === 'saved'"
              (click)="activeTab.set('saved')">
              Saved {{ state.savedJobIds().size }}
            </button>
          </div>
          <div class="search-inline">
            <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8" fill="none"/></svg>
            <input
              id="cjobs-search"
              placeholder="Search jobs, skills, companies…"
              #searchVal
              (input)="searchQuery.set(searchVal.value)"
            >
          </div>
          <select class="filter-select" id="cjobs-worktype" (change)="selectedWorkType.set($any($event.target).value)">
            <option value="">Work type</option>
            <option value="REMOTE">Remote</option>
            <option value="HYBRID">Hybrid</option>
            <option value="ON_SITE">On-site</option>
          </select>
        </div>

        <div id="cjobs-list" *ngIf="loaded() && filteredJobs().length > 0; else emptyState">
          <div class="job-card" *ngFor="let job of filteredJobs()">
            <div class="job-card-top">
              <div>
                <h3 class="job-card-title">{{ job.title }}</h3>
                <p class="job-card-sub">{{ job.companyName || 'Unknown company' }} · {{ job.location || 'Location not specified' }}</p>
              </div>
            </div>
            <div class="chip-group" style="margin:10px 0 0;">
              <span class="skill-pill" *ngFor="let skill of job.skills">{{ skill }}</span>
            </div>
            <div class="job-card-meta">
              <span>{{ workTypeLabel(job.workType) }}</span>
              <span>{{ employmentTypeLabel(job.employmentType) }}</span>
              <span>{{ salaryLabel(job) }}</span>
              <span>Posted {{ postedLabel(job.publishedAt) }}</span>
            </div>
            <div style="display:flex; gap:8px; margin-top:12px;">
              <button class="btn-primary-sm" (click)="viewJob(job)">View job</button>
              <button class="btn-ghost" (click)="toggleSave(job)">
                {{ isSaved(job.id) ? 'Saved ✓' : 'Save' }}
              </button>
            </div>
          </div>
        </div>

        <ng-template #emptyState>
          <div class="empty-state" id="cjobs-empty">
            <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8" fill="none"/></svg>
            <p>{{ loaded() ? 'No jobs match your filters' : 'Loading jobs…' }}</p>
            <button *ngIf="loaded()" (click)="clearFilters()">Clear filters</button>
          </div>
        </ng-template>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateBrowseJobsComponent implements OnInit {
  searchQuery = signal<string>('');
  activeTab = signal<string>('all');
  selectedWorkType = signal<string>('');
  loaded = signal(false);

  allJobs = signal<CandidateJobSummaryResponse[]>([]);

  constructor(public state: AppStateService, private router: Router, private jobService: JobService) {
    this.state.loadSavedJobIds();
  }

  ngOnInit() {
    this.jobService.browsePublicJobs().subscribe({
      next: (res) => {
        this.allJobs.set(res.data.content);
        this.loaded.set(true);
      },
      error: () => this.loaded.set(true),
    });
  }

  filteredJobs = computed(() => {
    const q = this.searchQuery().toLowerCase();
    const tab = this.activeTab();
    const workType = this.selectedWorkType();
    const savedIds = this.state.savedJobIds();

    return this.allJobs().filter(job => {
      const matchesSearch = !q
        || job.title.toLowerCase().includes(q)
        || (job.companyName ?? '').toLowerCase().includes(q)
        || job.skills.some(s => s.toLowerCase().includes(q));
      const matchesTab = tab === 'all' || savedIds.has(job.id);
      const matchesWorkType = !workType || job.workType === workType;
      return matchesSearch && matchesTab && matchesWorkType;
    });
  });

  isSaved(jobId: string): boolean {
    return this.state.savedJobIds().has(jobId);
  }

  workTypeLabel(workType: string | null): string {
    switch (workType) {
      case 'REMOTE': return 'Remote';
      case 'HYBRID': return 'Hybrid';
      case 'ON_SITE': return 'On-site';
      default: return 'Not specified';
    }
  }

  employmentTypeLabel(employmentType: string | null): string {
    switch (employmentType) {
      case 'FULL_TIME': return 'Full-time';
      case 'PART_TIME': return 'Part-time';
      case 'CONTRACT': return 'Contract';
      case 'INTERNSHIP': return 'Internship';
      default: return 'Not specified';
    }
  }

  salaryLabel(job: CandidateJobSummaryResponse): string {
    if (job.minSalary == null && job.maxSalary == null) {
      return 'Salary not disclosed';
    }
    const currency = job.salaryCurrency || 'INR';
    const fmt = (n: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n);
    if (job.minSalary != null && job.maxSalary != null) {
      return `${fmt(job.minSalary)} - ${fmt(job.maxSalary)}`;
    }
    return fmt(job.minSalary ?? job.maxSalary!);
  }

  postedLabel(publishedAt: string | null): string {
    if (!publishedAt) {
      return 'recently';
    }
    const days = Math.floor((Date.now() - new Date(publishedAt).getTime()) / (1000 * 60 * 60 * 24));
    if (days <= 0) return 'today';
    if (days === 1) return '1 day ago';
    if (days < 7) return `${days} days ago`;
    const weeks = Math.floor(days / 7);
    return weeks === 1 ? '1 week ago' : `${weeks} weeks ago`;
  }

  viewJob(job: CandidateJobSummaryResponse) {
    this.router.navigate(['/candidate/jobs', job.id]);
  }

  toggleSave(job: CandidateJobSummaryResponse) {
    this.state.toggleSaveJob(job.id, this.isSaved(job.id));
  }

  clearFilters() {
    this.searchQuery.set('');
    this.activeTab.set('all');
    this.selectedWorkType.set('');
  }
}
