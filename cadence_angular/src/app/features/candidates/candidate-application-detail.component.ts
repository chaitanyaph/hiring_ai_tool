import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { ApplicationResponse, ApplicationStage } from '../../core/models/application.model';
import { applicationStageLabel, compareToCurrentStage } from '../../core/utils/application.mapper';

/**
 * Reachable at /candidate/applications/:id, but nothing in the app currently
 * navigates here -- candidate-applications.component.ts's own "View" link uses
 * an internal signal toggle instead of this route. Pre-existing since before
 * this integration pass; left working (real data, same as the list's internal
 * detail view) rather than silently left broken, but not otherwise linked to.
 */
@Component({
  selector: 'app-candidate-application-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-application-detail" *ngIf="application() as app; else notFound">
      <button class="btn-text" style="padding-left:0; margin-bottom:14px;" (click)="goBack()">
        ← Back to applications
      </button>

      <div class="cd-header">
        <div class="avatar-xl">{{ getInitials(app.jobTitleSnapshot) }}</div>
        <div>
          <div class="cd-name">{{ app.jobTitleSnapshot }}</div>
          <div class="cd-sub">Applied {{ app.appliedAt | date: 'MMM d, y' }}</div>
        </div>
      </div>

      <div class="cd-layout">
        <div class="card">
          <div class="card-head">
            <h2>Application progress</h2>
            <span class="badge stage">{{ stageLabel(app) }}</span>
          </div>
          <div class="timeline">
            <div class="tl-track"></div>
            <div class="tl-item">
              <div class="tl-node done">
                <svg viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
              </div>
              <div class="tl-content">
                <div class="tl-top">
                  <span class="tl-title">Applied</span>
                  <span class="tl-date">{{ app.appliedAt | date: 'MMM d, y' }}</span>
                </div>
                <div class="tl-card"><span class="tl-note">Applied via <b>Cadence job search</b>.</span></div>
              </div>
            </div>

            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.AI_RESUME_SCREENING)">
                <svg *ngIf="stepStatus(app, stages.AI_RESUME_SCREENING) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.AI_RESUME_SCREENING) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">AI resume match</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.AI_RESUME_SCREENING) === 'pending' }">
                  <ng-container *ngIf="app.resumeMatchScore != null; else noScore1">
                    <div class="tl-score-row"><div class="tl-score"><div class="sv">{{ app.resumeMatchScore }}%</div><div class="sl">Match score</div></div></div>
                  </ng-container>
                  <ng-template #noScore1>Not yet scored.</ng-template>
                </div>
              </div>
            </div>

            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.AI_INTERVIEW)">
                <svg *ngIf="stepStatus(app, stages.AI_INTERVIEW) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.AI_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">AI interview</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.AI_INTERVIEW) === 'pending' }">
                  <ng-container *ngIf="app.aiInterviewScore != null; else noScore2">
                    <div class="tl-score-row"><div class="tl-score"><div class="sv" style="color:var(--teal);">{{ app.aiInterviewScore }}</div><div class="sl">Overall</div></div></div>
                  </ng-container>
                  <ng-template #noScore2>Not started yet.</ng-template>
                </div>
              </div>
            </div>

            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.CODING_ASSESSMENT)">
                <svg *ngIf="stepStatus(app, stages.CODING_ASSESSMENT) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.CODING_ASSESSMENT) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">Coding assessment</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.CODING_ASSESSMENT) === 'pending' }">
                  <ng-container *ngIf="app.codingScore != null; else noScore3">
                    <div class="tl-score-row"><div class="tl-score"><div class="sv">{{ app.codingScore }}%</div><div class="sl">Score</div></div></div>
                  </ng-container>
                  <ng-template #noScore3>Not started yet.</ng-template>
                </div>
              </div>
            </div>

            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.TECHNICAL_INTERVIEW)">
                <svg *ngIf="stepStatus(app, stages.TECHNICAL_INTERVIEW) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.TECHNICAL_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">Technical interview</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.TECHNICAL_INTERVIEW) === 'pending' }">
                  <span *ngIf="stepStatus(app, stages.TECHNICAL_INTERVIEW) !== 'pending'; else noTech">Feedback recorded internally by the panel.</span>
                  <ng-template #noTech>Not scheduled yet.</ng-template>
                </div>
              </div>
            </div>

            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.HR_INTERVIEW)">
                <svg *ngIf="stepStatus(app, stages.HR_INTERVIEW) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.HR_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">HR interview</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.HR_INTERVIEW) === 'pending' }">
                  <span *ngIf="stepStatus(app, stages.HR_INTERVIEW) !== 'pending'; else noHr">Meeting link activates 10 minutes before start.</span>
                  <ng-template #noHr>Unlocks once earlier rounds are complete.</ng-template>
                </div>
              </div>
            </div>

            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.OFFER)">
                <svg *ngIf="stepStatus(app, stages.OFFER) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title" [style.color]="stepStatus(app, stages.OFFER) === 'pending' ? 'var(--ink-faint)' : ''">Offer</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.OFFER) === 'pending' }">
                  {{ stepStatus(app, stages.OFFER) === 'pending' ? 'Unlocks once earlier rounds are complete.' : 'Offer released -- see Offers section.' }}
                </div>
              </div>
            </div>
          </div>
        </div>

        <div>
          <div class="card" style="margin-bottom:16px;">
            <div class="card-head"><h2>About this job</h2></div>
            <p style="font-size:12.5px; color:var(--ink-soft); line-height:1.6;">{{ app.jobTitleSnapshot }}</p>
          </div>
          <button class="btn-ghost" style="width:100%; color:var(--danger); border-color:var(--danger-tint);" (click)="withdraw()">
            Withdraw application
          </button>
        </div>
      </div>
    </section>

    <ng-template #notFound>
      <div class="empty-state" style="padding: 80px 24px;">
        <p>Application not found.</p>
        <button class="btn-primary-sm" (click)="goBack()">Back to applications</button>
      </div>
    </ng-template>
  `,
  styles: []
})
export class CandidateApplicationDetailComponent implements OnInit {
  applicationId = signal<string | null>(null);
  stages = ApplicationStage;

  constructor(
    public state: AppStateService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  application = computed(() => this.state.candidateApplications().find((a) => a.id === this.applicationId()) ?? null);

  ngOnInit() {
    this.state.loadMyApplications();
    this.route.paramMap.subscribe((params) => this.applicationId.set(params.get('id')));
  }

  goBack() {
    this.router.navigate(['/candidate/applications']);
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  stageLabel(app: ApplicationResponse): string {
    return applicationStageLabel(app.currentStage);
  }

  stepStatus(app: ApplicationResponse, stage: ApplicationStage): 'done' | 'current' | 'pending' {
    const cmp = compareToCurrentStage(app, stage);
    return cmp < 0 ? 'done' : cmp === 0 ? 'current' : 'pending';
  }

  withdraw() {
    const current = this.application();
    if (current && confirm('Withdraw this application?')) {
      this.state.withdrawApplication(current.id);
      this.router.navigate(['/candidate/applications']);
    }
  }
}
