import { Component, computed, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { ApplicationResponse, ApplicationStage, ApplicationStatus } from '../../core/models/application.model';
import { applicationStageLabel, applicationStatusBucket, compareToCurrentStage } from '../../core/utils/application.mapper';

@Component({
  selector: 'app-candidate-applications',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <!-- LIST VIEW -->
    <section class="section csection active" id="csec-applications" *ngIf="!selectedApp()">
      <div class="page-head">
        <div>
          <h1>My applications</h1>
          <p>Track the status of every job you've applied to.</p>
        </div>
      </div>

      <div class="card">
        <!-- Filter Row -->
        <div class="filter-row">
          <div class="filter-tabs">
            <button [class.active]="filter() === 'all'" (click)="filter.set('all')">All {{ allApps().length }}</button>
            <button [class.active]="filter() === 'active'" (click)="filter.set('active')">Active {{ countByStatus('active') }}</button>
            <button [class.active]="filter() === 'offer'" (click)="filter.set('offer')">Offer {{ countByStatus('offer') }}</button>
            <button [class.active]="filter() === 'rejected'" (click)="filter.set('rejected')">Rejected {{ countByStatus('rejected') }}</button>
          </div>
        </div>

        <!-- Applications Table -->
        <table class="table" *ngIf="filteredApps().length > 0; else emptyState">
          <thead>
            <tr>
              <th>Job title</th>
              <th>Applied on</th>
              <th>Current stage</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let app of filteredApps()">
              <td><b>{{ app.jobTitleSnapshot }}</b></td>
              <td class="muted">{{ app.appliedAt | date: 'MMM d, y' }}</td>
              <td>
                <span class="badge" [ngClass]="getBadgeClass(app)">
                  {{ stageLabel(app) }}
                </span>
              </td>
              <td>
                <span class="row-link" (click)="selectedApp.set(app)">View</span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Empty State -->
        <ng-template #emptyState>
          <div class="empty-state">
            <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M3 9h18" stroke="currentColor" stroke-width="1.8" fill="none"/></svg>
            <p>No applications in this filter</p>
          </div>
        </ng-template>
      </div>
    </section>

    <!-- DETAIL VIEW -->
    <section class="section csection active" id="csec-application-detail" *ngIf="selectedApp() as app">
      <button class="btn-text" style="padding-left:0; margin-bottom:14px;" (click)="selectedApp.set(null)">
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
        <!-- Timeline Card -->
        <div class="card">
          <div class="card-head">
            <h2>Application progress</h2>
            <span class="badge stage">{{ stageLabel(app) }}</span>
          </div>

          <div class="timeline">
            <div class="tl-track"></div>

            <!-- Applied -->
            <div class="tl-item">
              <div class="tl-node done">
                <svg viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
              </div>
              <div class="tl-content">
                <div class="tl-top">
                  <span class="tl-title">Applied</span>
                  <span class="tl-date">{{ app.appliedAt | date: 'MMM d, y' }}</span>
                </div>
                <div class="tl-card">
                  <span class="tl-note">Applied via <b>Cadence job search</b>.</span>
                </div>
              </div>
            </div>

            <!-- AI resume match -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.AI_RESUME_SCREENING)">
                <svg *ngIf="stepStatus(app, stages.AI_RESUME_SCREENING) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.AI_RESUME_SCREENING) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top">
                  <span class="tl-title">AI resume match</span>
                </div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.AI_RESUME_SCREENING) === 'pending' }">
                  <ng-container *ngIf="app.resumeMatchScore != null; else noScoreYet">
                    <div class="tl-score-row">
                      <div class="tl-score">
                        <div class="sv">{{ app.resumeMatchScore }}%</div>
                        <div class="sl">Match score</div>
                      </div>
                    </div>
                  </ng-container>
                  <ng-template #noScoreYet>Not yet scored.</ng-template>
                </div>
              </div>
            </div>

            <!-- AI interview -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.AI_INTERVIEW)">
                <svg *ngIf="stepStatus(app, stages.AI_INTERVIEW) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.AI_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">AI interview</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.AI_INTERVIEW) === 'pending' }">
                  <ng-container *ngIf="app.aiInterviewScore != null; else pendingAiInterview">
                    <div class="tl-score-row">
                      <div class="tl-score"><div class="sv" style="color:var(--teal);">{{ app.aiInterviewScore }}</div><div class="sl">Overall</div></div>
                    </div>
                  </ng-container>
                  <ng-template #pendingAiInterview>
                    <ng-container *ngIf="app.currentStatus === statuses.AI_INTERVIEW_PENDING; else notStartedYet">
                      <button class="btn-primary-sm" (click)="startAiInterview(app)">Start AI interview</button>
                    </ng-container>
                    <ng-template #notStartedYet>Not started yet.</ng-template>
                  </ng-template>
                </div>
              </div>
            </div>

            <!-- Coding assessment -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.CODING_ASSESSMENT)">
                <svg *ngIf="stepStatus(app, stages.CODING_ASSESSMENT) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.CODING_ASSESSMENT) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">Coding assessment</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.CODING_ASSESSMENT) === 'pending' }">
                  <ng-container *ngIf="app.codingScore != null; else pendingCoding">
                    <div class="tl-score-row">
                      <div class="tl-score"><div class="sv">{{ app.codingScore }}%</div><div class="sl">Score</div></div>
                    </div>
                  </ng-container>
                  <ng-template #pendingCoding>Not started yet.</ng-template>
                </div>
              </div>
            </div>

            <!-- Technical interview -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.TECHNICAL_INTERVIEW)">
                <svg *ngIf="stepStatus(app, stages.TECHNICAL_INTERVIEW) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.TECHNICAL_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">Technical interview</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.TECHNICAL_INTERVIEW) === 'pending' }">
                  <span class="tl-note" *ngIf="stepStatus(app, stages.TECHNICAL_INTERVIEW) !== 'pending'; else pendingTech">Feedback recorded internally by the panel.</span>
                  <ng-template #pendingTech>Not scheduled yet.</ng-template>
                </div>
              </div>
            </div>

            <!-- HR interview -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.HR_INTERVIEW)">
                <svg *ngIf="stepStatus(app, stages.HR_INTERVIEW) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
                <div *ngIf="stepStatus(app, stages.HR_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">HR interview</span></div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.HR_INTERVIEW) === 'pending' }" [style.borderColor]="stepStatus(app, stages.HR_INTERVIEW) === 'current' ? 'var(--indigo)' : ''" [style.background]="stepStatus(app, stages.HR_INTERVIEW) === 'current' ? 'var(--indigo-tint)' : ''">
                  <span class="tl-note" *ngIf="stepStatus(app, stages.HR_INTERVIEW) !== 'pending'; else pendingHr">Meeting link activates 10 minutes before start.</span>
                  <ng-template #pendingHr>Unlocks once earlier rounds are complete.</ng-template>
                </div>
              </div>
            </div>

            <!-- Offer -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.OFFER)">
                <svg *ngIf="stepStatus(app, stages.OFFER) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
              </div>
              <div class="tl-content">
                <div class="tl-top">
                  <span class="tl-title" [style.color]="stepStatus(app, stages.OFFER) === 'pending' ? 'var(--ink-faint)' : ''">Offer</span>
                </div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.OFFER) === 'pending' }">
                  <ng-container *ngIf="stepStatus(app, stages.OFFER) === 'pending'; else offerReleased">Unlocks once earlier rounds are complete.</ng-container>
                  <ng-template #offerReleased>Offer released — <a routerLink="/candidate/offers">see the Offers section</a>.</ng-template>
                </div>
              </div>
            </div>

            <!-- Hired -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="stepStatus(app, stages.HIRED)">
                <svg *ngIf="stepStatus(app, stages.HIRED) === 'done'" viewBox="0 0 24 24" style="width:12px; height:12px; stroke:white; stroke-width:3; fill:none;"><path d="M5 13l4 4L19 7"/></svg>
              </div>
              <div class="tl-content">
                <div class="tl-top">
                  <span class="tl-title" [style.color]="stepStatus(app, stages.HIRED) === 'pending' ? 'var(--ink-faint)' : ''">Joined</span>
                </div>
                <div class="tl-card" [ngClass]="{ pending: stepStatus(app, stages.HIRED) === 'pending' }">
                  <ng-container *ngIf="stepStatus(app, stages.HIRED) === 'pending'; else hired">Unlocks once you accept your offer.</ng-container>
                  <ng-template #hired>Welcome aboard!</ng-template>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar Actions -->
        <div>
          <div class="card" style="margin-bottom:16px;">
            <div class="card-head"><h2>About this job</h2></div>
            <p style="font-size:12.5px; color:var(--ink-soft); line-height:1.6;">
              {{ app.jobTitleSnapshot }}
            </p>
          </div>
          <button class="btn-ghost" style="width:100%; color:var(--danger); border-color:var(--danger-tint);" (click)="withdraw()">
            Withdraw application
          </button>
        </div>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateApplicationsComponent implements OnInit {
  filter = signal<string>('all');
  selectedApp = signal<ApplicationResponse | null>(null);
  stages = ApplicationStage;
  statuses = ApplicationStatus;

  constructor(public state: AppStateService, private router: Router) {}

  ngOnInit() {
    this.state.loadMyApplications();
  }

  allApps = computed(() => this.state.candidateApplications());

  filteredApps = computed(() => {
    const f = this.filter();
    return this.allApps().filter((app) => f === 'all' || applicationStatusBucket(app.currentStatus) === f);
  });

  countByStatus(bucket: 'active' | 'offer' | 'rejected'): number {
    return this.allApps().filter((app) => applicationStatusBucket(app.currentStatus) === bucket).length;
  }

  stageLabel(app: ApplicationResponse): string {
    return applicationStageLabel(app.currentStage);
  }

  stepStatus(app: ApplicationResponse, stage: ApplicationStage): 'done' | 'current' | 'pending' {
    const cmp = compareToCurrentStage(app, stage);
    return cmp < 0 ? 'done' : cmp === 0 ? 'current' : 'pending';
  }

  getBadgeClass(app: ApplicationResponse): string {
    const bucket = applicationStatusBucket(app.currentStatus);
    if (bucket === 'offer') return 'published';
    if (bucket === 'rejected') return 'archived';
    return 'stage';
  }

  getInitials(name: string): string {
    return name.split(' ').map(n => n[0]).join('').toUpperCase().substring(0, 2);
  }

  startAiInterview(app: ApplicationResponse) {
    this.router.navigate(['/candidate/ai-interview', app.id]);
  }

  withdraw() {
    const current = this.selectedApp();
    if (current && confirm('Withdraw this application?')) {
      this.state.withdrawApplication(current.id);
      this.selectedApp.set(null);
    }
  }
}
