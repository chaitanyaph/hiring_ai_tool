import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { Job } from '../../core/models/models';

@Component({
  selector: 'app-candidate-job-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-job-detail" *ngIf="job()">
      <button class="btn-text" style="padding-left:0; margin-bottom:14px;" (click)="goBack()">
        ← Back to jobs
      </button>
      <div class="row-2">
        <div class="card">
          <div class="card-head">
            <div>
              <h2 id="jd-title">{{ job()?.title }}</h2>
              <div class="card-sub" id="jd-company">
                {{ job()?.id === 'job-1' ? 'Acme Corp' : (job()?.id === 'job-2' ? 'Nimbus Labs' : 'Orbit Systems') }} · {{ job()?.location }} · Posted 2 days ago
              </div>
            </div>
            <span class="badge" [ngClass]="getMatchClass()" id="jd-match">
              {{ matchScore() }}% match
            </span>
          </div>
          <div class="chip-group" style="margin-bottom:16px;">
            <span class="skill-pill" *ngFor="let skill of job()?.skills">{{ skill }}</span>
          </div>
          <div class="job-card-meta" style="margin-bottom:16px;">
            <span>{{ job()?.location }}</span>
            <span>{{ job()?.empType }}</span>
            <span>{{ job()?.salary }}</span>
            <span>{{ job()?.experience }} experience</span>
          </div>
          <h3 style="font-family:'Fraunces',serif; font-size:14px; margin-bottom:6px;">About the role</h3>
          <p style="font-size:13px; color:var(--ink-soft); line-height:1.6; margin-bottom:14px;">
            {{ job()?.description }}
          </p>
          <h3 style="font-family:'Fraunces',serif; font-size:14px; margin-bottom:6px;">Responsibilities</h3>
          <ul style="font-size:13px; color:var(--ink-soft); line-height:1.8; margin:0 0 14px 18px;">
            <li>Design and build scalable REST APIs using Spring Boot</li>
            <li>Own service reliability, observability and on-call rotation</li>
            <li>Collaborate with product on technical scoping</li>
          </ul>
        </div>
        <div>
          <div class="card" style="margin-bottom:16px;">
            <div class="card-head"><h2>Apply</h2></div>
            <p style="font-size:12.5px; color:var(--ink-soft); margin-bottom:14px;">
              Your profile is 82% complete — recruiters see your resume, skills and experience.
            </p>
            <button class="btn-primary" style="width:100%; margin-bottom:8px;" (click)="apply()">Apply now</button>
            <button class="btn-ghost" style="width:100%;" (click)="save()">Save for later</button>
          </div>
          <div class="card">
            <div class="card-head"><h2>Why you match</h2></div>
            <div class="tl-score-row" style="margin-bottom:6px;">
              <div class="tl-score">
                <div class="sv">{{ matchScore() }}%</div>
                <div class="sl">Overall match</div>
              </div>
              <div class="tl-score">
                <div class="sv">{{ skillsMatchedText() }}</div>
                <div class="sl">Skills matched</div>
              </div>
            </div>
            <p style="font-size:12px; color:var(--ink-soft);">
              Your experience closely matches this role's requirements.
            </p>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateJobDetailComponent implements OnInit {
  job = signal<Job | null>(null);
  matchScore = signal<number>(91);

  constructor(
    public state: AppStateService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      const id = params.get('id') || 'job-1';
      const foundJob = this.state.jobs().find(j => j.id === id);
      if (foundJob) {
        this.job.set(foundJob);
        if (id === 'job-1') this.matchScore.set(91);
        else if (id === 'job-2') this.matchScore.set(84);
        else this.matchScore.set(68);
      } else {
        this.router.navigate(['/candidate/browse-jobs']);
      }
    });
  }

  goBack() {
    this.router.navigate(['/candidate/browse-jobs']);
  }

  getMatchClass() {
    return this.matchScore() >= 80 ? 'match-high' : 'match-mid';
  }

  skillsMatchedText() {
    if (this.job()?.id === 'job-1') return '5/5';
    if (this.job()?.id === 'job-2') return '3/4';
    return '2/4';
  }

  apply() {
    // job.id is still local mock data here (job-service has no candidate-safe browse
    // endpoint yet -- see Module 3 gap notes), so this real call will fail until that's
    // resolved; wired now so it's correct the moment a real jobId is available.
    const jobId = this.job()?.id;
    if (!jobId) return;
    this.state.applyToJob(jobId).subscribe({
      next: () => this.router.navigate(['/candidate/applications']),
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not submit this application.'),
    });
  }

  save() {
    const jobId = this.job()?.id;
    if (!jobId) return;
    this.state.toggleSaveJob(jobId, false);
  }
}
