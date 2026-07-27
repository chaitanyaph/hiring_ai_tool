import { Component, Input, OnInit, Output, EventEmitter } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';

@Component({
  selector: 'app-topbar',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="topbar">
      <!-- Search -->
      <div class="search-wrap">
        <svg class="search-icon" viewBox="0 0 24 24">
          <circle cx="11" cy="11" r="8" fill="none" stroke="currentColor" stroke-width="1.8"/>
          <line x1="21" y1="21" x2="16.65" y2="16.65" stroke="currentColor" stroke-width="1.8"/>
        </svg>
        <input
          type="text"
          #searchInput
          [placeholder]="isCandidate ? 'Ask your AI career coach anything…' : 'Show Java developers with Spring Boot, Kafka and Redis'"
          (keyup.enter)="submitQuery(searchInput.value); searchInput.value = ''"
        >
        <span class="ai-badge">AI</span>
      </div>

      <!-- Actions -->
      <div class="topbar-actions">
        <!-- Notification bell -->
        <div class="icon-btn" (click)="openNotifications()">
          <svg viewBox="0 0 24 24">
            <path d="M18 8A6 6 0 006 8c0 7-3 9-3 9h18s-3-2-3-9M13.73 21a2 2 0 01-3.46 0" fill="none" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/>
          </svg>
          <span class="dot" *ngIf="state.unreadNotificationCount() > 0"></span>
        </div>

        <!-- Recruiter buttons -->
        <ng-container *ngIf="!isCandidate">
          <button class="btn-ghost" (click)="onImportResumes.emit()">
            <svg viewBox="0 0 24 24">
              <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            Import resumes
          </button>
          <button class="btn-primary-sm" (click)="onNewJob.emit()">
            <svg viewBox="0 0 24 24">
              <line x1="12" y1="5" x2="12" y2="19" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
              <line x1="5" y1="12" x2="19" y2="12" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/>
            </svg>
            New job
          </button>
        </ng-container>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .topbar {
      position: sticky;
      top: 0;
      z-index: 5;
      background: rgba(250, 249, 244, 0.92);
      backdrop-filter: blur(6px);
      border-bottom: 1px solid var(--line);
      padding: 14px 32px;
      display: flex;
      align-items: center;
      gap: 16px;
      flex-shrink: 0;
    }

    .search-wrap {
      position: relative;
      flex: 1;
      max-width: 480px;

      .search-icon {
        position: absolute;
        left: 12px;
        top: 50%;
        transform: translateY(-50%);
        width: 15px;
        height: 15px;
        stroke: var(--ink-soft);
        fill: none;
        stroke-width: 1.8;
      }

      input {
        width: 100%;
        font-family: $font-sans;
        font-size: 13.5px;
        padding: 10px 52px 10px 38px;
        border: 1px solid var(--line);
        border-radius: 9px;
        background: var(--paper-card);
        outline: none;
        color: var(--ink);
        @include transition-base;

        &:focus {
          border-color: var(--indigo);
          box-shadow: 0 0 0 3px rgba(55, 47, 132, 0.12);
        }

        &::placeholder { color: var(--ink-faint); }
      }

      .ai-badge {
        position: absolute;
        right: 9px;
        top: 50%;
        transform: translateY(-50%);
        font-size: 10px;
        font-weight: 700;
        background: var(--indigo-tint);
        color: var(--indigo);
        padding: 3px 7px;
        border-radius: 5px;
      }
    }

    .topbar-actions {
      display: flex;
      align-items: center;
      gap: 10px;
      margin-left: auto;
    }

    .icon-btn {
      width: 36px;
      height: 36px;
      border-radius: 9px;
      border: 1px solid var(--line);
      background: var(--paper-card);
      display: flex;
      align-items: center;
      justify-content: center;
      cursor: pointer;
      position: relative;
      @include transition-base;

      &:hover {
        border-color: #C9C4B2;
        background: var(--line-soft);
      }

      svg {
        width: 16px;
        height: 16px;
        stroke: var(--ink-soft);
        fill: none;
        stroke-width: 1.8;
      }

      .dot {
        position: absolute;
        top: 7px;
        right: 7px;
        width: 6px;
        height: 6px;
        border-radius: 50%;
        background: var(--danger);
      }
    }
  `]
})
export class TopbarComponent implements OnInit {
  @Input() isCandidate: boolean = false;
  @Output() onImportResumes = new EventEmitter<void>();
  @Output() onNewJob = new EventEmitter<void>();

  constructor(public state: AppStateService, private router: Router) {}

  ngOnInit() {
    this.state.loadUnreadNotificationCount();
  }

  openNotifications() {
    this.router.navigate([this.isCandidate ? '/candidate/notifications' : '/recruiter/notifications']);
  }

  submitQuery(val: string) {
    const query = val.trim();
    if (!query) return;
    this.state.showToast(`Searching: '${query}'`);
    if (this.isCandidate) {
      this.router.navigate(['/candidate/ai-coach']);
      this.state.sendCandidateChatMessage(query);
    } else {
      this.router.navigate(['/recruiter/ai-assistant']);
      this.state.sendRecruiterChatMessage(query);
    }
  }
}
