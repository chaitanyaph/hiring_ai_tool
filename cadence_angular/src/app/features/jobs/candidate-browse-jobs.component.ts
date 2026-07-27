import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';

interface BrowseJobItem {
  id: string;
  title: string;
  company: string;
  location: string;
  matchScore: number;
  skills: string[];
  workType: string;
  empType: string;
  salary: string;
  postedDate: string;
  saved: boolean;
}

@Component({
  selector: 'app-candidate-browse-jobs',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-jobs">
      <div class="page-head">
        <div>
          <h1>Browse jobs</h1>
          <p>AI-matched openings based on your profile and preferences.</p>
        </div>
      </div>
      
      <div class="card">
        <div class="filter-row">
          <div class="filter-tabs" id="cjobs-filter-tabs">
            <button
              [class.active]="activeTab() === 'all'"
              (click)="activeTab.set('all')">
              All 24
            </button>
            <button
              [class.active]="activeTab() === 'saved'"
              (click)="activeTab.set('saved')">
              Saved 11
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
            <option value="Remote">Remote</option>
            <option value="Hybrid">Hybrid</option>
            <option value="On-site">On-site</option>
          </select>
        </div>

        <div id="cjobs-list" *ngIf="filteredJobs().length > 0; else emptyState">
          <div class="job-card" *ngFor="let job of filteredJobs()">
            <div class="job-card-top">
              <div>
                <h3 class="job-card-title">{{ job.title }}</h3>
                <p class="job-card-sub">{{ job.company }} · {{ job.location }}</p>
              </div>
              <span class="badge" [ngClass]="job.matchScore >= 80 ? 'match-high' : 'match-mid'">
                {{ job.matchScore }}% match
              </span>
            </div>
            <div class="chip-group" style="margin:10px 0 0;">
              <span class="skill-pill" *ngFor="let skill of job.skills">{{ skill }}</span>
            </div>
            <div class="job-card-meta">
              <span>{{ job.workType }}</span>
              <span>{{ job.empType }}</span>
              <span>{{ job.salary }}</span>
              <span>Posted {{ job.postedDate }}</span>
            </div>
            <div style="display:flex; gap:8px; margin-top:12px;">
              <button class="btn-primary-sm" (click)="viewJob(job)">View job</button>
              <button class="btn-ghost" (click)="toggleSave(job)">
                {{ job.saved ? 'Saved ✓' : 'Save' }}
              </button>
            </div>
          </div>
        </div>

        <ng-template #emptyState>
          <div class="empty-state" id="cjobs-empty">
            <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8" fill="none"/></svg>
            <p>No jobs match your filters</p>
            <button (click)="clearFilters()">Clear filters</button>
          </div>
        </ng-template>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateBrowseJobsComponent {
  searchQuery = signal<string>('');
  activeTab = signal<string>('all');
  selectedWorkType = signal<string>('');

  jobs = signal<BrowseJobItem[]>([
    {
      id: 'job-1',
      title: 'Senior Backend Engineer',
      company: 'Acme Corp',
      location: 'Pune, India',
      matchScore: 91,
      skills: ['Java', 'Spring Boot', 'Kafka', 'AWS'],
      workType: 'Remote',
      empType: 'Full-time',
      salary: '₹18-28 LPA',
      postedDate: '2 days ago',
      saved: true
    },
    {
      id: 'job-2',
      title: 'Platform Engineer',
      company: 'Nimbus Labs',
      location: 'Bengaluru, India',
      matchScore: 84,
      skills: ['Spring Boot', 'Docker', 'Redis'],
      workType: 'Hybrid',
      empType: 'Full-time',
      salary: '₹15-24 LPA',
      postedDate: '5 days ago',
      saved: false
    },
    {
      id: 'job-3',
      title: 'DevOps Engineer',
      company: 'Orbit Systems',
      location: 'Hyderabad, India',
      matchScore: 68,
      skills: ['Kubernetes', 'AWS', 'Terraform'],
      workType: 'On-site',
      empType: 'Full-time',
      salary: '₹12-20 LPA',
      postedDate: '1 week ago',
      saved: false
    }
  ]);

  constructor(public state: AppStateService, private router: Router) {
    this.state.loadSavedJobIds();
  }

  filteredJobs = computed(() => {
    const q = this.searchQuery().toLowerCase();
    const tab = this.activeTab();
    const workType = this.selectedWorkType();

    return this.jobs().filter(job => {
      const matchesSearch = job.title.toLowerCase().includes(q) || job.company.toLowerCase().includes(q) || job.skills.some(s => s.toLowerCase().includes(q));
      const matchesTab = tab === 'all' || job.saved;
      const matchesWorkType = !workType || job.workType === workType;
      return matchesSearch && matchesTab && matchesWorkType;
    });
  });

  viewJob(job: BrowseJobItem) {
    this.router.navigate(['/candidate/jobs', job.id]);
  }

  toggleSave(job: BrowseJobItem) {
    // This screen's jobs are still local mock data (job-service has no candidate-safe
    // browse endpoint -- see Module 3 gap notes), so job.id isn't a real job-service
    // UUID. state.toggleSaveJob() (wired to the real SavedJobController) is called
    // here for when that upstream gap is resolved, but the visible saved/unsaved
    // state below still comes from the local list, not the backend response.
    this.state.toggleSaveJob(job.id, job.saved);
    this.jobs.update(list => list.map(j => j.id === job.id ? { ...j, saved: !j.saved } : j));
  }

  clearFilters() {
    this.searchQuery.set('');
    this.activeTab.set('all');
    this.selectedWorkType.set('');
  }
}
