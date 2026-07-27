import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { CandidateAssessmentHistoryItemResponse, CandidateAssessmentStatus } from '../../core/models/coding-assessment.model';

function bucketOf(status: CandidateAssessmentStatus): 'Completed' | 'Pending' | 'Expired' {
  if (status === CandidateAssessmentStatus.COMPLETED) return 'Completed';
  if (status === CandidateAssessmentStatus.EXPIRED) return 'Expired';
  return 'Pending'; // NOT_STARTED and IN_PROGRESS both read as "Pending" -- no separate filter tab exists for in-progress.
}

@Component({
  selector: 'app-candidate-coding-assessments',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-coding-history">
      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>Coding assessments</h1>
          <p>Track every coding assessment you've been invited to.</p>
        </div>
      </div>

      <!-- Filter tabs -->
      <div class="filter-tabs" id="coding-history-tabs" style="margin-bottom:14px;">
        <button [class.active]="filter() === 'all'" (click)="filter.set('all')">All</button>
        <button [class.active]="filter() === 'Completed'" (click)="filter.set('Completed')">Completed</button>
        <button [class.active]="filter() === 'Pending'" (click)="filter.set('Pending')">Pending</button>
        <button [class.active]="filter() === 'Expired'" (click)="filter.set('Expired')">Expired</button>
      </div>

      <!-- Card wrapper for table -->
      <div class="card">
        <table class="table" *ngIf="filteredAssessments().length > 0; else emptyState">
          <thead>
            <tr>
              <th>Assessment</th>
              <th>Company</th>
              <th>Status</th>
              <th>Score</th>
              <th>Date</th>
              <th></th>
            </tr>
          </thead>
          <tbody>
            <tr *ngFor="let item of filteredAssessments()">
              <td><b>{{ item.assessmentName }}</b></td>
              <td class="muted">{{ item.companyName }}</td>
              <td>
                <span class="badge" [ngClass]="getBadgeClass(item.status)">
                  {{ item.status }}
                </span>
              </td>
              <td>
                <span class="badge match-high" *ngIf="item.status === 'COMPLETED'">{{ item.score }}%</span>
                <span class="muted" *ngIf="item.status !== 'COMPLETED'">—</span>
              </td>
              <td class="muted">{{ item.relevantDate | date: 'MMM d, y' }}</td>
              <td>
                <span class="row-link" *ngIf="item.status === 'COMPLETED'" (click)="viewSubmission(item)">
                  View submission
                </span>
                <span class="row-link" *ngIf="item.status === 'NOT_STARTED'" (click)="startAssessment(item)">
                  Start assessment
                </span>
                <span class="row-link" *ngIf="item.status === 'IN_PROGRESS'" (click)="startAssessment(item)">
                  Resume
                </span>
                <span class="muted" *ngIf="item.status === 'EXPIRED'">—</span>
              </td>
            </tr>
          </tbody>
        </table>

        <!-- Empty state template -->
        <ng-template #emptyState>
          <div class="empty-state" id="coding-history-empty">
            <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8" fill="none"/></svg>
            <p>No assessments match this filter.</p>
            <button class="btn-ghost" (click)="filter.set('all')">Clear filter</button>
          </div>
        </ng-template>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateCodingAssessmentsComponent implements OnInit {
  filter = signal<string>('all');

  constructor(public state: AppStateService, private router: Router) {}

  ngOnInit() {
    this.state.loadCandidateAssessmentHistory();
  }

  filteredAssessments = computed(() => {
    const f = this.filter();
    if (f === 'all') return this.state.candidateAssessmentHistory();
    return this.state.candidateAssessmentHistory().filter((a) => bucketOf(a.status) === f);
  });

  getBadgeClass(status: CandidateAssessmentStatus) {
    if (status === 'COMPLETED') return 'published';
    if (status === 'NOT_STARTED' || status === 'IN_PROGRESS') return 'draft';
    return 'failed';
  }

  viewSubmission(item: CandidateAssessmentHistoryItemResponse) {
    this.state.showToast(`Opening submission for ${item.assessmentName}...`);
  }

  startAssessment(item: CandidateAssessmentHistoryItemResponse) {
    this.router.navigate(['/candidate/coding-assessments', item.candidateAssessmentId]);
  }
}
