import { Component, computed, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { ApplicationStage, ApplicationStatus } from '../../core/models/application.model';
import { applicationStageLabel, compareToCurrentStage, nextStatus } from '../../core/utils/application.mapper';

@Component({
  selector: 'app-candidate-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="detail-viewport" *ngIf="app(); else noCandidate">
      <!-- Back button -->
      <button class="btn-text" style="padding-left:0; margin-bottom:14px;" (click)="goBack()">
        &larr; Back to candidates
      </button>

      <!-- Header section -->
      <div class="cd-header">
        <div class="avatar-xl">{{ initials() }}</div>
        <div>
          <div class="cd-name">{{ app()?.candidateNameSnapshot }}</div>
          <div class="cd-sub">Applying for {{ app()?.jobTitleSnapshot }}</div>
          <div class="cd-meta-row">
            <span>{{ app()?.candidateEmailSnapshot }}</span>
          </div>
        </div>
      </div>

      <!-- Layout columns -->
      <div class="cd-layout">
        <!-- Timeline Column -->
        <div class="card">
          <div class="card-head">
            <h2>Hiring progress</h2>
            <span class="badge stage">{{ stageLabel() }}</span>
          </div>
          <!--
            The granular per-round content below (AI interview category breakdowns,
            coding test-case counts, technical/HR panel names+ratings) is owned by
            AI Interview Service / Coding Assessment Service / Interview Management
            Service respectively (Modules 8/9/10, not yet integrated) -- left as
            static illustrative content, flagged here rather than fabricated as if real.
            Only the stage gating (done/current/pending) and the two real scores
            (resumeMatchScore via overallScore, aiInterviewScore, codingScore) below
            are driven by application-service's actual ApplicationResponse.
          -->
          <div class="timeline">
            <div class="tl-track"></div>

            <!-- Step 0: Applied -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="getStepStatus(stages.APPLICATION)">
                <svg *ngIf="getStepStatus(stages.APPLICATION) === 'done'" viewBox="0 0 24 24"><path d="M5 13l4 4L19 7" stroke="#fff" fill="none" stroke-width="2.6"/></svg>
                <div *ngIf="getStepStatus(stages.APPLICATION) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">Applied</span><span class="tl-date">{{ app()?.appliedAt | date: 'MMM d, y' }}</span></div>
                <div class="tl-card"><span class="tl-note">Applied for {{ app()?.jobTitleSnapshot }}.</span></div>
              </div>
            </div>

            <!-- Step 1: AI Shortlisted / Resume screening -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="getStepStatus(stages.AI_RESUME_SCREENING)">
                <svg *ngIf="getStepStatus(stages.AI_RESUME_SCREENING) === 'done'" viewBox="0 0 24 24"><path d="M5 13l4 4L19 7" stroke="#fff" fill="none" stroke-width="2.6"/></svg>
                <div *ngIf="getStepStatus(stages.AI_RESUME_SCREENING) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">AI Shortlisted &middot; Resume screening</span></div>
                <div class="tl-card">
                  <div class="tl-score-row">
                    <div class="tl-score"><div class="sv">{{ app()?.resumeMatchScore ?? '—' }}{{ app()?.resumeMatchScore != null ? '%' : '' }}</div><div class="sl">Match score</div></div>
                  </div>
                  <a class="card-link" (click)="downloadResume()">Download parsed resume</a>
                </div>
              </div>
            </div>

            <!-- Step 2: AI Interview -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="getStepStatus(stages.AI_INTERVIEW)">
                <svg *ngIf="getStepStatus(stages.AI_INTERVIEW) === 'done'" viewBox="0 0 24 24"><path d="M5 13l4 4L19 7" stroke="#fff" fill="none" stroke-width="2.6"/></svg>
                <div *ngIf="getStepStatus(stages.AI_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">AI Interview</span></div>
                <div class="tl-card" [ngClass]="{'pending': getStepStatus(stages.AI_INTERVIEW) === 'pending'}">
                  <ng-container *ngIf="getStepStatus(stages.AI_INTERVIEW) !== 'pending'; else pendingAi">
                    <div class="tl-score-row">
                      <div class="tl-score"><div class="sv" style="color:var(--teal);">{{ app()?.aiInterviewScore ?? '—' }}</div><div class="sl">Overall</div></div>
                    </div>
                    <div class="tl-note">Detailed transcript available once Module 8 (AI Interview) is wired.</div>
                  </ng-container>
                  <ng-template #pendingAi>
                    Schedule AI Interview to start evaluation.
                  </ng-template>
                </div>
              </div>
            </div>

            <!-- Step 3: Coding assessment -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="getStepStatus(stages.CODING_ASSESSMENT)">
                <svg *ngIf="getStepStatus(stages.CODING_ASSESSMENT) === 'done'" viewBox="0 0 24 24"><path d="M5 13l4 4L19 7" stroke="#fff" fill="none" stroke-width="2.6"/></svg>
                <div *ngIf="getStepStatus(stages.CODING_ASSESSMENT) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">Coding assessment</span></div>
                <div class="tl-card" [ngClass]="{'pending': getStepStatus(stages.CODING_ASSESSMENT) === 'pending'}">
                  <ng-container *ngIf="getStepStatus(stages.CODING_ASSESSMENT) !== 'pending'; else pendingCoding">
                    <div class="tl-score-row">
                      <div class="tl-score"><div class="sv">{{ app()?.codingScore ?? '—' }}{{ app()?.codingScore != null ? '%' : '' }}</div><div class="sl">Score</div></div>
                    </div>
                    <div class="tl-note">Full submission review available once Module 9 (Coding Assessment) is wired.</div>
                  </ng-container>
                  <ng-template #pendingCoding>
                    Unlocks after AI Interview is complete.
                  </ng-template>
                </div>
              </div>
            </div>

            <!-- Step 4: Technical round -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="getStepStatus(stages.TECHNICAL_INTERVIEW)">
                <svg *ngIf="getStepStatus(stages.TECHNICAL_INTERVIEW) === 'done'" viewBox="0 0 24 24"><path d="M5 13l4 4L19 7" stroke="#fff" fill="none" stroke-width="2.6"/></svg>
                <div *ngIf="getStepStatus(stages.TECHNICAL_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">Technical round</span></div>
                <div class="tl-card" [ngClass]="{'pending': getStepStatus(stages.TECHNICAL_INTERVIEW) === 'pending'}">
                  <ng-container *ngIf="getStepStatus(stages.TECHNICAL_INTERVIEW) !== 'pending'; else pendingTech">
                    <div class="tl-note">Panel feedback available once Module 10 (Interview Management) is wired.</div>
                  </ng-container>
                  <ng-template #pendingTech>
                    Pending technical panel scheduling.
                  </ng-template>
                </div>
              </div>
            </div>

            <!-- Step 5: HR round -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="getStepStatus(stages.HR_INTERVIEW)">
                <svg *ngIf="getStepStatus(stages.HR_INTERVIEW) === 'done'" viewBox="0 0 24 24"><path d="M5 13l4 4L19 7" stroke="#fff" fill="none" stroke-width="2.6"/></svg>
                <div *ngIf="getStepStatus(stages.HR_INTERVIEW) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title">HR round</span></div>
                <div class="tl-card" [ngClass]="{'pending': getStepStatus(stages.HR_INTERVIEW) === 'pending'}" [style.borderColor]="getStepStatus(stages.HR_INTERVIEW) === 'current' ? 'var(--indigo)' : ''" [style.background]="getStepStatus(stages.HR_INTERVIEW) === 'current' ? 'var(--indigo-tint)' : ''">
                  <ng-container *ngIf="getStepStatus(stages.HR_INTERVIEW) !== 'pending'; else pendingHr">
                    <div class="tl-note">Meeting link activates 10 minutes before start.</div>
                  </ng-container>
                  <ng-template #pendingHr>
                    Unlocks once technical round feedback is approved.
                  </ng-template>
                </div>
              </div>
            </div>

            <!-- Step 6: Offer -->
            <div class="tl-item">
              <div class="tl-node" [ngClass]="getStepStatus(stages.OFFER)">
                <svg *ngIf="getStepStatus(stages.OFFER) === 'done'" viewBox="0 0 24 24"><path d="M5 13l4 4L19 7" stroke="#fff" fill="none" stroke-width="2.6"/></svg>
                <div *ngIf="getStepStatus(stages.OFFER) === 'current'" class="pulse-dot"></div>
              </div>
              <div class="tl-content">
                <div class="tl-top"><span class="tl-title" [style.color]="getStepStatus(stages.OFFER) === 'pending' ? 'var(--ink-faint)' : ''">Offer</span></div>
                <div class="tl-card pending" [ngClass]="{'pending': getStepStatus(stages.OFFER) === 'pending'}">
                  <ng-container *ngIf="getStepStatus(stages.OFFER) !== 'pending'; else pendingOffer">
                    Offer generated and sent to candidate -- see Module 12 (Offer Management).
                  </ng-container>
                  <ng-template #pendingOffer>
                    Unlocks once HR round feedback is submitted.
                  </ng-template>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- Sidebar / Actions Column -->
        <div>
          <!-- Quick actions card -->
          <div class="card" style="margin-bottom:16px;">
            <div class="card-head"><h2>Quick actions</h2></div>
            <div class="profile-stats">
              <div class="pstat"><div class="pv">{{ app()?.overallScore ?? '—' }}{{ app()?.overallScore != null ? '%' : '' }}</div><div class="pl">Overall score</div></div>
              <div class="pstat"><div class="pv">{{ app()?.resumeMatchScore ?? '—' }}{{ app()?.resumeMatchScore != null ? '%' : '' }}</div><div class="pl">Resume match</div></div>
            </div>
            <button class="btn-ghost" style="width:100%; justify-content:center; margin-bottom:8px;" (click)="downloadResume()">
              Download resume
            </button>
            <div class="profile-actions">
              <button class="pa-reject" (click)="reject()">Reject</button>
              <button class="pa-schedule" [disabled]="!canAdvance()" (click)="advanceStage()">Next round</button>
            </div>
          </div>

          <!-- Recruiter Note card -->
          <div class="card">
            <div class="card-head"><h2>Recruiter notes</h2></div>
            <div class="list-card" style="margin-bottom:12px;" *ngIf="app()?.notes?.length">
              <div class="interview-item" *ngFor="let note of app()?.notes">
                <div class="meta">
                  <div class="who">{{ note.note }}</div>
                  <div class="role">{{ note.createdAt | date: 'MMM d, y, h:mm a' }}</div>
                </div>
              </div>
            </div>
            <textarea
              #noteText
              style="width:100%; min-height:80px; border:1px solid var(--line); border-radius:8px; padding:10px; font-family:'Inter',sans-serif; font-size:13px; outline:none;"
              placeholder="Add a note for the hiring team…"
            ></textarea>
            <button class="btn-primary-sm" style="margin-top:10px;" (click)="saveNote(noteText); noteText.value = ''">
              Save note
            </button>
          </div>
        </div>
      </div>
    </div>

    <!-- Empty Detail state -->
    <ng-template #noCandidate>
      <div class="empty-state" style="padding: 80px 24px;">
        <svg viewBox="0 0 24 24" width="44" height="44" style="stroke:var(--ink-faint); fill:none; stroke-width:1.5; margin-bottom:10px;"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2M9 7A4 4 0 119 7z"/></svg>
        <h3>No Candidate Selected</h3>
        <p style="font-size:13px; color:var(--ink-soft); margin-bottom:12px;">Go back to candidates list to view profiles.</p>
        <button class="btn-primary-sm" (click)="goBack()">View candidates</button>
      </div>
    </ng-template>
  `,
  styles: [`
    @use 'variables' as *;

    .detail-viewport {
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 16px;
      font-family: $font-sans;
    }
  `]
})
export class CandidateDetailComponent implements OnInit {
  stages = ApplicationStage;

  constructor(public state: AppStateService, public router: Router, private route: ActivatedRoute) {}

  app = computed(() => this.state.selectedApplication());

  ngOnInit() {
    const id = this.route.snapshot.paramMap.get('id');
    if (id && this.app()?.id !== id) {
      this.state.loadApplicationDetail(id);
    }
  }

  initials = computed(() => {
    const name = this.app()?.candidateNameSnapshot;
    return name ? name.split(' ').map(n => n[0]).join('').toUpperCase() : '';
  });

  stageLabel(): string {
    const app = this.app();
    return app ? applicationStageLabel(app.currentStage) : '';
  }

  getStepStatus(stage: ApplicationStage): 'done' | 'current' | 'pending' {
    const app = this.app();
    if (!app) return 'pending';
    const cmp = compareToCurrentStage(app, stage);
    return cmp < 0 ? 'done' : cmp === 0 ? 'current' : 'pending';
  }

  canAdvance(): boolean {
    const app = this.app();
    return !!app && nextStatus(app.currentStatus) != null;
  }

  goBack() {
    this.state.clearSelectedApplication();
    this.router.navigate(['/recruiter/candidates']);
  }

  downloadResume() {
    const app = this.app();
    if (!app) return;
    this.state.recruiterDownloadResume(app.resumeId, `${app.candidateNameSnapshot}_resume.pdf`);
  }

  reject() {
    const app = this.app();
    if (!app) return;
    this.state.changeApplicationStatus(app.id, ApplicationStatus.REJECTED);
  }

  advanceStage() {
    const app = this.app();
    if (!app) return;
    const next = nextStatus(app.currentStatus);
    if (!next) {
      this.state.showToast('Candidate is already at the final stage');
      return;
    }
    this.state.changeApplicationStatus(app.id, next);
  }

  saveNote(noteText: HTMLTextAreaElement) {
    const app = this.app();
    if (!app || !noteText.value.trim()) return;
    this.state.addApplicationNote(app.id, noteText.value.trim());
  }
}
