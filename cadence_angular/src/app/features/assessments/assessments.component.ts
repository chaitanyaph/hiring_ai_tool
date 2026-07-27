import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';

@Component({
  selector: 'app-assessments',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section active" id="sec-assessments">
      <div class="page-head">
        <div>
          <h1>Assessments</h1>
          <p>Coding & MCQ test library</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-primary-sm" (click)="state.openModal('assessment')">
            <svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" fill="none" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg>
            New assessment
          </button>
        </div>
      </div>

      <!-- Filter Tabs -->
      <div class="filter-tabs" style="margin-bottom:16px;">
        <button [class.active]="activeTab() === 'coding'" (click)="activeTab.set('coding')">Coding</button>
        <button [class.active]="activeTab() === 'mcq'" (click)="activeTab.set('mcq')">MCQ</button>
      </div>

      <!-- Coding Tab: the real list -- AssessmentListItemResponse has no type field, so it
           can't actually be split Coding/MCQ client-side; shown here since coding-assessment-service
           is fundamentally coding-first. -->
      <div *ngIf="activeTab() === 'coding'">
        <div class="assess-card" *ngFor="let item of state.codingAssessmentsList()">
          <div class="assess-top">
            <span class="aname">{{ item.name }}</span>
            <span class="diff-badge" [ngClass]="item.status === 'PUBLISHED' ? 'easy' : (item.status === 'DRAFT' ? 'medium' : 'hard')">{{ item.status }}</span>
          </div>
          <div class="assess-meta">
            <span>Job: <b>{{ item.jobTitle }}</b></span>
            <span>Languages: <b>{{ item.allowedLanguages.join(', ') }}</b></span>
            <span>Time limit: <b>{{ item.durationMinutes }} min</b></span>
            <span>Questions: <b>{{ item.questionCount }}</b></span>
            <span>Invited: <b>{{ item.invitedCount }}</b></span>
          </div>
          <div class="assess-foot">
            <button class="card-link-btn" (click)="openResults(item.id)">View results</button>
            <button class="row-link" *ngIf="item.status === 'DRAFT'" (click)="publish(item.id)">Publish</button>
            <button class="row-link" (click)="clone(item.id)">Duplicate</button>
          </div>
        </div>
        <div class="empty-state" *ngIf="!state.codingAssessmentsList().length"><p>No assessments yet.</p></div>
      </div>

      <!-- MCQ Tab -->
      <div *ngIf="activeTab() === 'mcq'">
        <div class="empty-state">
          <p>Assessment type isn't distinguishable from the list endpoint (AssessmentListItemResponse has no type field) -- MCQ filtering isn't available without an N+1 lookup per row.</p>
        </div>
      </div>
    </section>
  `,
  styles: [`
    @use 'variables' as *;

    /* Filter Tabs (match list component) */
    .filter-tabs {
      display: inline-flex;
      background: var(--line-soft);
      border-radius: 9px;
      padding: 3px;
      margin-bottom: 16px;

      button {
        border: none;
        background: none;
        font-family: $font-sans;
        font-size: 12.5px;
        font-weight: 500;
        padding: 6px 14px;
        border-radius: 6px;
        color: var(--ink-soft);
        cursor: pointer;
        @include transition-base;

        &.active {
          background: var(--paper-card);
          color: var(--ink);
          box-shadow: 0 1px 2px rgba(28,27,41,0.08);
          font-weight: 600;
        }
      }
    }

    .assess-card {
      border: 1px solid var(--line);
      border-radius: 12px;
      padding: 16px;
      margin-bottom: 12px;
      background: var(--paper-card);
      transition: box-shadow 0.15s, border-color 0.15s;

      &:hover {
        box-shadow: 0 4px 12px rgba(28,27,41,0.04);
        border-color: #C9C4B2;
      }
    }

    .assess-top {
      display: flex;
      align-items: center;
      justify-content: space-between;
      margin-bottom: 8px;
    }

    .assess-top .aname {
      font-size: 14px;
      font-weight: 600;
      color: var(--ink);
    }

    .assess-meta {
      display: flex;
      gap: 16px;
      font-size: 12px;
      color: var(--ink-soft);
      margin-bottom: 12px;
      flex-wrap: wrap;

      b {
        color: var(--ink);
      }
    }

    .assess-foot {
      display: flex;
      align-items: center;
      justify-content: space-between;
    }

    .card-link-btn {
      color: var(--indigo);
      background: none;
      border: none;
      font-family: $font-sans;
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      padding: 0;

      &:hover {
        text-decoration: underline;
      }
    }

    .row-link {
      color: var(--indigo);
      background: none;
      border: none;
      font-family: $font-sans;
      font-size: 12.5px;
      font-weight: 500;
      cursor: pointer;
      padding: 0;

      &:hover {
        text-decoration: underline;
      }
    }

    .diff-badge {
      font-size: 10.5px;
      font-weight: 600;
      padding: 3px 8px;
      border-radius: 999px;
      text-transform: capitalize;

      &.easy {
        background: var(--teal-tint);
        color: var(--teal);
      }
      &.medium {
        background: var(--gold-tint);
        color: #8A6A1F;
      }
      &.hard {
        background: var(--danger-tint);
        color: var(--danger);
      }
    }
  `]
})
export class AssessmentsComponent implements OnInit {
  activeTab = signal<'coding' | 'mcq'>('coding');

  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadCodingAssessments();
  }

  publish(id: string) {
    this.state.publishCodingAssessment(id);
  }

  clone(id: string) {
    this.state.cloneCodingAssessment(id);
  }

  openResults(id: string) {
    this.state.showToast('Opening results for this assessment…');
  }
}
