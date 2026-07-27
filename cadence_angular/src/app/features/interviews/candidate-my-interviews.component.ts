import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { CandidateInterviewResponse, InterviewMode, RoundType } from '../../core/models/interview-management.model';

@Component({
  selector: 'app-candidate-my-interviews',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-interviews">
      <div class="page-head"><div><h1>My interviews</h1><p>Every interview you're scheduled for, plus how past ones went.</p></div></div>

      <div class="filter-tabs" style="margin-bottom:14px;">
        <button [class.active]="activeTab() === 'upcoming'" (click)="setTab('upcoming')">Upcoming</button>
        <button [class.active]="activeTab() === 'past'" (click)="setTab('past')">Past</button>
      </div>

      <div class="card">
        <div class="list-card" style="max-height:none;" *ngIf="!state.myInterviewsLoading(); else loadingState">
          <div class="interview-item" *ngFor="let item of filteredInterviews()" (click)="openDetail(item)" style="cursor:pointer;">
            <div class="avatar">{{ initials(item) }}</div>
            <div class="meta">
              <div class="who">{{ roundLabel(item.roundType) }} · {{ item.jobTitle }}</div>
              <div class="role">{{ item.companyName }}</div>
              <span class="tag" [ngClass]="tagClass(item)" *ngIf="item.upcoming">{{ modeLabel(item.mode) }}</span>
              <span class="badge" [ngClass]="statusBadgeClass(item)" *ngIf="!item.upcoming">{{ statusLabel(item) }}</span>
            </div>
            <div class="when">{{ item.upcoming ? (item.scheduledDate | date: 'MMM d') : (item.scheduledDate | date: 'MMM d') }}<br>{{ item.scheduledTime | slice:0:5 }}</div>
          </div>
          <div class="empty-state" *ngIf="!filteredInterviews().length">
            <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="17" rx="2"/><path d="M3 9h18" stroke="currentColor" stroke-width="1.8" fill="none"/></svg>
            <p>No {{ activeTab() }} interviews.</p>
          </div>
        </div>
        <ng-template #loadingState>
          <div class="skeleton-card" style="margin-bottom:10px;" *ngFor="let i of [1,2,3]">
            <div class="skeleton sk-title"></div>
            <div class="skeleton sk-sub"></div>
          </div>
        </ng-template>
      </div>
    </section>

    <!-- DETAIL DRAWER -->
    <div class="drawer-overlay" [ngClass]="{ show: detail() }" (click)="closeDetail()">
      <div class="drawer" (click)="$event.stopPropagation()" *ngIf="detail() as d">
        <div class="drawer-head">
          <div><h3>{{ roundLabel(d.roundType) }}</h3><p>{{ d.jobTitle }} · {{ d.companyName }}</p></div>
          <button class="modal-close" aria-label="Close panel" (click)="closeDetail()"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></button>
        </div>
        <div class="drawer-body">
          <section>
            <div class="review-block">
              <div class="review-row"><span>Date &amp; time</span><span>{{ d.scheduledDate | date:'MMM d, y' }}, {{ d.scheduledTime | slice:0:5 }}</span></div>
              <div class="review-row"><span>Mode</span><span>{{ modeLabel(d.mode) }}</span></div>
              <div class="review-row"><span>Interviewer(s)</span><span>{{ d.interviewerNames.length ? d.interviewerNames.join(', ') : '—' }}</span></div>
              <div class="review-row"><span>Status</span><span class="badge" [ngClass]="statusBadgeClass(d)">{{ statusLabel(d) }}</span></div>
            </div>
          </section>
        </div>
        <div class="drawer-foot" *ngIf="d.upcoming">
          <button class="btn-ghost" (click)="requestReschedule(d.id)">Request reschedule</button>
          <div class="foot-right">
            <button class="btn-ghost" (click)="closeDetail()">Close</button>
            <button class="btn-primary-sm" (click)="joinMeeting(d.id)">Join meeting</button>
          </div>
        </div>
        <div class="drawer-foot" *ngIf="!d.upcoming">
          <span></span>
          <div class="foot-right"><button class="btn-primary-sm" (click)="closeDetail()">Close</button></div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class CandidateMyInterviewsComponent implements OnInit {
  activeTab = signal<'upcoming' | 'past'>('upcoming');

  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadMyInterviews();
  }

  setTab(tab: 'upcoming' | 'past') {
    this.activeTab.set(tab);
  }

  allInterviews = computed(() => this.state.myInterviews());

  filteredInterviews = computed(() => {
    const upcoming = this.activeTab() === 'upcoming';
    return this.allInterviews()
      .filter((i) => i.upcoming === upcoming)
      .sort((a, b) => (a.scheduledDate + a.scheduledTime).localeCompare(b.scheduledDate + b.scheduledTime));
  });

  detail = computed(() => this.state.myInterviewDetail());

  openDetail(item: CandidateInterviewResponse) {
    this.state.loadMyInterviewDetail(item.id);
  }

  closeDetail() {
    this.state.clearMyInterviewDetail();
  }

  requestReschedule(id: string) {
    this.state.requestMyInterviewReschedule(id);
  }

  joinMeeting(id: string) {
    this.state.joinMyInterview(id);
  }

  initials(item: CandidateInterviewResponse): string {
    return this.roundLabel(item.roundType).charAt(0).toUpperCase();
  }

  roundLabel(type: RoundType): string {
    switch (type) {
      case RoundType.TECHNICAL: return 'Technical round';
      case RoundType.HR: return 'HR round';
      case RoundType.MANAGER: return 'Manager round';
      case RoundType.ARCHITECT: return 'Architect round';
      default: return 'Custom round';
    }
  }

  modeLabel(mode: InterviewMode): string {
    switch (mode) {
      case InterviewMode.ONLINE: return 'Video call';
      case InterviewMode.OFFLINE: return 'In-person';
      default: return 'Hybrid';
    }
  }

  tagClass(item: CandidateInterviewResponse): string {
    if (item.roundType === RoundType.HR) return 'hr';
    return 'tech';
  }

  statusLabel(item: CandidateInterviewResponse): string {
    switch (item.status) {
      case 'COMPLETED': return 'Completed';
      case 'CANCELLED': return 'Cancelled';
      case 'RESCHEDULED': return 'Rescheduled';
      default: return 'Scheduled';
    }
  }

  statusBadgeClass(item: CandidateInterviewResponse): string {
    switch (item.status) {
      case 'COMPLETED': return 'published';
      case 'CANCELLED': return 'archived';
      case 'RESCHEDULED': return 'processing';
      default: return 'stage';
    }
  }
}
