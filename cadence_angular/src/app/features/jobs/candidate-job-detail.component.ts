import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { JobService } from '../../core/services/job.service';
import { CandidateJobDetailResponse } from '../../core/models/job.model';

@Component({
  selector: 'app-candidate-job-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-job-detail" *ngIf="job() as j">
      <button class="btn-text" style="padding-left:0; margin-bottom:14px;" (click)="goBack()">
        ← Back to jobs
      </button>
      <div class="row-2">
        <div class="card">
          <div class="card-head">
            <div>
              <h2 id="jd-title">{{ j.title }}</h2>
              <div class="card-sub" id="jd-company">
                {{ j.companyName || 'Unknown company' }} · {{ j.location || 'Location not specified' }} · Posted {{ postedLabel(j.publishedAt) }}
              </div>
            </div>
            <span class="badge" [ngClass]="statusBadgeClass()" id="jd-status">{{ statusLabel() }}</span>
          </div>
          <div class="chip-group" style="margin-bottom:16px;">
            <span class="skill-pill" *ngFor="let skill of j.requirements?.skills">{{ skill.skillName }}</span>
          </div>
          <div class="job-card-meta" style="margin-bottom:16px;">
            <span>{{ j.location || 'Not specified' }}</span>
            <span>{{ employmentTypeLabel(j.employmentType) }}</span>
            <span>{{ salaryLabel(j) }}</span>
            <span *ngIf="j.requirements?.minExperienceYears != null || j.requirements?.maxExperienceYears != null">
              {{ experienceLabel(j) }} experience
            </span>
          </div>

          <ng-container *ngIf="j.descriptionHtml">
            <h3 style="font-family:'Fraunces',serif; font-size:14px; margin-bottom:6px;">About the role</h3>
            <div style="font-size:13px; color:var(--ink-soft); line-height:1.6; margin-bottom:14px;" [innerHTML]="j.descriptionHtml"></div>
          </ng-container>

          <ng-container *ngIf="j.requirements?.responsibilities">
            <h3 style="font-family:'Fraunces',serif; font-size:14px; margin-bottom:6px;">Responsibilities</h3>
            <p style="font-size:13px; color:var(--ink-soft); line-height:1.8; margin-bottom:14px; white-space:pre-line;">{{ j.requirements?.responsibilities }}</p>
          </ng-container>

          <ng-container *ngIf="qualificationsText() as quals">
            <h3 style="font-family:'Fraunces',serif; font-size:14px; margin-bottom:6px;">Qualifications</h3>
            <p style="font-size:13px; color:var(--ink-soft); line-height:1.8; margin-bottom:14px; white-space:pre-line;">{{ quals }}</p>
          </ng-container>

          <ng-container *ngIf="j.requirements?.benefits?.length">
            <h3 style="font-family:'Fraunces',serif; font-size:14px; margin-bottom:6px;">Benefits</h3>
            <ul style="font-size:13px; color:var(--ink-soft); line-height:1.8; margin:0 0 14px 18px;">
              <li *ngFor="let benefit of j.requirements?.benefits">{{ benefit }}</li>
            </ul>
          </ng-container>

          <div style="font-size:12px; color:var(--ink-soft);" *ngIf="j.applicationDeadline">
            Application deadline: {{ j.applicationDeadline }}
          </div>
        </div>

        <div>
          <div class="card">
            <div class="card-head"><h2>Apply</h2></div>

            <p *ngIf="alreadyApplied()" style="font-size:12.5px; color:var(--ink-soft); margin-bottom:14px;">
              You've already applied to this job.
            </p>
            <p *ngIf="!alreadyApplied() && !isOpenForApplications()" style="font-size:12.5px; color:var(--ink-soft); margin-bottom:14px;">
              This job is no longer accepting applications.
            </p>

            <button
              class="btn-primary"
              style="width:100%; margin-bottom:8px;"
              [disabled]="alreadyApplied() || !isOpenForApplications() || applying()"
              (click)="apply()">
              {{ alreadyApplied() ? 'Applied ✓' : (applying() ? 'Submitting…' : 'Apply now') }}
            </button>
            <button class="btn-ghost" style="width:100%;" (click)="save()">Save for later</button>
          </div>
        </div>
      </div>
    </section>

    <section class="section csection active" *ngIf="!job() && !notFound()">
      <p>Loading job…</p>
    </section>

    <section class="section csection active" *ngIf="notFound()">
      <p>This job could not be found.</p>
      <button class="btn-text" style="padding-left:0;" (click)="goBack()">← Back to jobs</button>
    </section>
  `,
  styles: []
})
export class CandidateJobDetailComponent implements OnInit {
  job = signal<CandidateJobDetailResponse | null>(null);
  notFound = signal(false);
  applying = signal(false);

  alreadyApplied = computed(() => {
    const j = this.job();
    if (!j) return false;
    return this.state.candidateApplications().some(a => a.jobId === j.id);
  });

  constructor(
    public state: AppStateService,
    private route: ActivatedRoute,
    private router: Router,
    private jobService: JobService
  ) {}

  ngOnInit() {
    this.state.loadMyApplications();
    this.route.paramMap.subscribe(params => {
      const id = params.get('id');
      if (!id) {
        this.notFound.set(true);
        return;
      }
      this.jobService.getPublicJobDetail(id).subscribe({
        next: (res) => this.job.set(res.data),
        error: () => this.notFound.set(true),
      });
    });
  }

  isOpenForApplications(): boolean {
    return this.job()?.status === 'PUBLISHED';
  }

  statusLabel(): string {
    switch (this.job()?.status) {
      case 'PUBLISHED': return 'Open';
      case 'PAUSED': return 'Paused';
      case 'CLOSED': return 'Closed';
      case 'ARCHIVED': return 'Closed';
      case 'EXPIRED': return 'Expired';
      default: return '';
    }
  }

  statusBadgeClass(): string {
    return this.job()?.status === 'PUBLISHED' ? 'match-high' : 'match-mid';
  }

  employmentTypeLabel(employmentType: string | null | undefined): string {
    switch (employmentType) {
      case 'FULL_TIME': return 'Full-time';
      case 'PART_TIME': return 'Part-time';
      case 'CONTRACT': return 'Contract';
      case 'INTERNSHIP': return 'Internship';
      default: return 'Not specified';
    }
  }

  salaryLabel(j: CandidateJobDetailResponse): string {
    const req = j.requirements;
    if (!req || (req.minSalary == null && req.maxSalary == null)) {
      return 'Salary not disclosed';
    }
    const currency = req.salaryCurrency || 'INR';
    const fmt = (n: number) => new Intl.NumberFormat('en-IN', { style: 'currency', currency, maximumFractionDigits: 0 }).format(n);
    if (req.minSalary != null && req.maxSalary != null) {
      return `${fmt(req.minSalary)} - ${fmt(req.maxSalary)}`;
    }
    return fmt(req.minSalary ?? req.maxSalary!);
  }

  experienceLabel(j: CandidateJobDetailResponse): string {
    const req = j.requirements;
    if (!req) return '';
    if (req.minExperienceYears != null && req.maxExperienceYears != null) {
      return `${req.minExperienceYears}-${req.maxExperienceYears} years`;
    }
    const only = req.minExperienceYears ?? req.maxExperienceYears;
    return only != null ? `${only}+ years` : '';
  }

  qualificationsText(): string {
    const req = this.job()?.requirements;
    if (!req) return '';
    const parts: string[] = [];
    if (req.education) parts.push(req.education);
    if (req.certifications) parts.push(req.certifications);
    if (req.languages) parts.push(`Languages: ${req.languages}`);
    return parts.join('\n');
  }

  postedLabel(publishedAt: string | null): string {
    if (!publishedAt) return 'recently';
    const days = Math.floor((Date.now() - new Date(publishedAt).getTime()) / (1000 * 60 * 60 * 24));
    if (days <= 0) return 'today';
    if (days === 1) return '1 day ago';
    if (days < 7) return `${days} days ago`;
    const weeks = Math.floor(days / 7);
    return weeks === 1 ? '1 week ago' : `${weeks} weeks ago`;
  }

  goBack() {
    this.router.navigate(['/candidate/browse-jobs']);
  }

  apply() {
    const jobId = this.job()?.id;
    if (!jobId || this.alreadyApplied() || !this.isOpenForApplications()) return;
    this.applying.set(true);
    this.state.applyToJob(jobId).subscribe({
      next: () => this.applying.set(false),
      error: (err) => {
        this.applying.set(false);
        this.state.showToast(err?.error?.message ?? 'Could not submit this application.');
      },
    });
  }

  save() {
    const jobId = this.job()?.id;
    if (!jobId) return;
    this.state.toggleSaveJob(jobId, this.state.savedJobIds().has(jobId));
  }
}
