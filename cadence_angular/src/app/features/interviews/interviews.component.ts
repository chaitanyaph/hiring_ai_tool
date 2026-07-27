import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { InterviewListItemResponse, InterviewStatus, RoundType } from '../../core/models/interview-management.model';

@Component({
  selector: 'app-interviews',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="interviews-viewport">
      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>Interviews</h1>
          <p>{{ scheduledCount() }} scheduled this week</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-primary-sm" (click)="scheduleInterview()">
            <svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg>
            Schedule interview
          </button>
        </div>
      </div>

      <!-- Card Container -->
      <div class="card">
        <!-- Filter Row -->
        <div class="filter-row">
          <div class="filter-tabs">
            <button
              *ngFor="let t of timeTabs"
              [class.active]="activeTimeTab() === t"
              (click)="activeTimeTab.set(t)">
              {{ t }}
            </button>
          </div>
          <div class="filter-tabs">
            <button
              *ngFor="let t of typeTabs"
              [class.active]="activeTypeTab() === t"
              (click)="activeTypeTab.set(t)">
              {{ t }}
            </button>
          </div>
        </div>

        <!--
          Day groups are built from real scheduledDate values (interview-management-service
          has no fixed "today/tomorrow" concept) -- this is the established *ngFor exception
          for static repeated markup that can't represent variable-length real data.
        -->
        <div class="day-group" *ngFor="let group of dayGroups()">
          <div class="day-label">{{ group.label }}</div>

          <div class="interview-row" *ngFor="let item of group.items">
            <div class="avatar">{{ initials(item.candidateName) }}</div>
            <div class="meta">
              <div class="who">{{ item.candidateName }}</div>
              <div class="role">{{ item.jobTitle }}</div>
            </div>
            <span class="tag" [ngClass]="getTagClass(item.roundType)">{{ typeLabel(item.roundType) }}</span>

            <!--
              PanelistResponse only carries interviewerId/interviewerRole, not a name --
              no interviewer directory endpoint exists in this session, so avatars fall
              back to the role's initial rather than fabricating names.
            -->
            <div class="panel-stack">
              <div class="p-avatar" *ngFor="let p of item.panelists">{{ (p.interviewerRole || '?').charAt(0).toUpperCase() }}</div>
            </div>

            <div class="time">{{ item.scheduledTime | slice:0:5 }}</div>

            <button class="join-btn" *ngIf="item.status !== 'CANCELLED' && item.status !== 'COMPLETED'" (click)="joinMeeting(item)">
              Join
            </button>
            <button class="join-btn secondary-join" *ngIf="item.status === 'COMPLETED'" (click)="remindMe(item)">
              {{ item.feedbackSubmitted ? 'Feedback submitted' : 'Awaiting feedback' }}
            </button>
          </div>
        </div>

        <div class="empty-state" *ngIf="dayGroups().length === 0">
          <p>No interviews match your filters.</p>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .interviews-viewport {
      padding: 0;
      font-family: $font-sans;
    }

    .filter-row {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-bottom: 16px;
      flex-wrap: wrap;
    }

    .filter-tabs {
      display: flex;
      background: var(--line-soft);
      border-radius: 8px;
      padding: 2px;

      button {
        border: none;
        background: none;
        font-size: 12.5px;
        font-weight: 500;
        color: var(--ink-soft);
        padding: 7px 13px;
        border-radius: 6px;
        cursor: pointer;
        font-family: $font-sans;
        @include transition-base;

        &.active {
          background: var(--paper-card);
          color: var(--ink);
          box-shadow: 0 1px 2px rgba(28,27,41,0.08);
        }
      }
    }

    .day-group {
      margin-bottom: 22px;

      &:last-child {
        margin-bottom: 0;
      }
    }

    .day-label {
      font-size: 12px;
      font-weight: 600;
      color: var(--ink-soft);
      text-transform: uppercase;
      letter-spacing: 0.04em;
      margin-bottom: 10px;
    }

    .interview-row {
      display: flex;
      align-items: center;
      gap: 14px;
      padding: 13px 14px;
      border: 1px solid var(--line);
      border-radius: 11px;
      margin-bottom: 8px;
      background: var(--paper-card);

      .avatar {
        width: 36px;
        height: 36px;
        border-radius: 50%;
        background: var(--indigo-tint);
        color: var(--indigo);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 12px;
        font-weight: 700;
        flex-shrink: 0;
      }

      .meta {
        flex: 1;
        min-width: 0;

        .who {
          font-size: 13.5px;
          font-weight: 600;
          color: var(--ink);
        }

        .role {
          font-size: 12px;
          color: var(--ink-soft);
          margin-top: 1px;
        }
      }

      .time {
        font-size: 12.5px;
        color: var(--ink-soft);
        white-space: nowrap;
        width: 130px;
        text-align: right;
      }
    }

    .tag {
      font-size: 10px;
      font-weight: 600;
      padding: 2px 7px;
      border-radius: 5px;
      display: inline-block;

      &.ai { background: var(--indigo-tint); color: var(--indigo); }
      &.tech { background: var(--gold-tint); color: #8A6A1F; }
      &.hr { background: var(--teal-tint); color: var(--teal); }
    }

    .panel-stack {
      display: flex;
      align-items: center;

      .p-avatar {
        width: 24px;
        height: 24px;
        border-radius: 50%;
        background: var(--gold-tint);
        color: #8A6A1F;
        font-size: 10px;
        font-weight: 700;
        display: flex;
        align-items: center;
        justify-content: center;
        border: 2px solid var(--paper-card);
        margin-left: -7px;

        &:first-child {
          margin-left: 0;
        }
      }
    }

    .join-btn {
      background: var(--indigo);
      color: var(--paper-card);
      border: none;
      border-radius: 7px;
      padding: 7px 13px;
      font-size: 12px;
      font-weight: 600;
      cursor: pointer;
      white-space: nowrap;
      font-family: $font-sans;
      @include transition-base;

      &:hover {
        background: var(--indigo-deep);
      }

      &.secondary-join {
        background: var(--paper-card);
        border: 1px solid var(--line);
        color: var(--ink);

        &:hover {
          background: var(--line-soft);
        }
      }
    }

    .empty-state {
      text-align: center;
      padding: 40px 0;
      color: var(--ink-soft);
      font-size: 13.5px;
    }
  `]
})
export class InterviewsComponent implements OnInit {
  timeTabs = ['This week', 'Today', 'All'];
  typeTabs = ['All types', 'Technical', 'HR'];

  activeTimeTab = signal<string>('This week');
  activeTypeTab = signal<string>('All types');

  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadInterviews();
  }

  private todayIso(): string {
    return new Date().toISOString().slice(0, 10);
  }

  filteredInterviews = computed(() => {
    const timeTab = this.activeTimeTab();
    const typeTab = this.activeTypeTab();
    const today = this.todayIso();
    const weekAhead = new Date();
    weekAhead.setDate(weekAhead.getDate() + 7);
    const weekAheadIso = weekAhead.toISOString().slice(0, 10);

    return this.state.interviews().filter((item) => {
      if (timeTab === 'Today' && item.scheduledDate !== today) return false;
      if (timeTab === 'This week' && (item.scheduledDate < today || item.scheduledDate > weekAheadIso)) return false;

      if (typeTab === 'Technical' && item.roundType !== RoundType.TECHNICAL) return false;
      if (typeTab === 'HR' && item.roundType !== RoundType.HR) return false;

      return true;
    });
  });

  dayGroups = computed(() => {
    const items = [...this.filteredInterviews()].sort((a, b) => (a.scheduledDate + a.scheduledTime).localeCompare(b.scheduledDate + b.scheduledTime));
    const today = this.todayIso();
    const tomorrow = new Date();
    tomorrow.setDate(tomorrow.getDate() + 1);
    const tomorrowIso = tomorrow.toISOString().slice(0, 10);

    const groups: { date: string; label: string; items: InterviewListItemResponse[] }[] = [];
    for (const item of items) {
      let group = groups.find((g) => g.date === item.scheduledDate);
      if (!group) {
        let label = new Date(item.scheduledDate).toLocaleDateString('en-US', { weekday: 'short', month: 'short', day: 'numeric' });
        if (item.scheduledDate === today) label = `Today · ${label}`;
        else if (item.scheduledDate === tomorrowIso) label = `Tomorrow · ${label}`;
        group = { date: item.scheduledDate, label, items: [] };
        groups.push(group);
      }
      group.items.push(item);
    }
    return groups;
  });

  scheduledCount = computed(() => this.state.interviews().filter((i) => i.status === InterviewStatus.SCHEDULED || i.status === InterviewStatus.RESCHEDULED).length);

  initials(name: string): string {
    return name
      .split(' ')
      .map((p) => p.charAt(0))
      .join('')
      .slice(0, 2)
      .toUpperCase();
  }

  typeLabel(type: RoundType): string {
    switch (type) {
      case RoundType.TECHNICAL: return 'Technical round';
      case RoundType.HR: return 'HR round';
      case RoundType.MANAGER: return 'Manager round';
      case RoundType.ARCHITECT: return 'Architect round';
      default: return 'Custom round';
    }
  }

  getTagClass(type: RoundType): string {
    if (type === RoundType.TECHNICAL || type === RoundType.ARCHITECT) return 'tech';
    if (type === RoundType.HR) return 'hr';
    return 'ai';
  }

  joinMeeting(item: InterviewListItemResponse) {
    this.state.joinInterview(item.id);
  }

  remindMe(item: InterviewListItemResponse) {
    this.state.showToast(`Reminder set for ${item.candidateName}'s interview.`);
  }

  scheduleInterview() {
    this.state.openModal('interview');
  }
}
