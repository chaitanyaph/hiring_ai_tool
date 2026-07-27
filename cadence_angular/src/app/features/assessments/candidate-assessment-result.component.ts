import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AssessmentResultResponse } from '../../core/models/coding-assessment.model';

@Component({
  selector: 'app-candidate-assessment-result',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div id="assessment-view" class="show">
      <!-- RESULT -->
      <div class="iv-completion-screen" id="ca-result" style="display:flex;" *ngIf="result() as r">
        <div class="iv-intro-card" style="max-width:560px; text-align:center;">
          <div class="success-check-pop"><svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg></div>
          <div class="result-hero" style="justify-content:center;">
            <div class="profile-ring">
              <svg viewBox="0 0 64 64">
                <circle class="ring-bg" cx="32" cy="32" r="28"/>
                <circle class="ring-fg" cx="32" cy="32" r="28" stroke="#1F7A6C" stroke-dasharray="175.9" id="ca-result-ring" [attr.stroke-dashoffset]="ringOffset()"/>
              </svg>
              <div class="ring-label" style="color:#1F7A6C;" id="ca-result-score">{{ r.scorePercent ?? '—' }}{{ r.scorePercent != null ? '%' : '' }}</div>
            </div>
            <div style="text-align:left;">
              <div style="font-family:'Fraunces',serif; font-weight:600; font-size:15px;">Assessment completed</div>
              <div style="font-size:12.5px; color:var(--ink-soft); margin-top:2px;" id="ca-result-summary">{{ r.testCasesPassed ?? '—' }} of {{ r.testCasesTotal ?? '—' }} test cases passed &middot; {{ r.timeUsedMinutes ?? '—' }} min used</div>
              <div style="font-size:11.5px; color:var(--ink-faint); margin-top:2px;" id="ca-result-time">{{ r.autoSubmitted ? 'Auto-submitted' : 'Submitted' }} {{ r.submittedAt | date: 'MMM d, y, h:mm a' }}</div>
            </div>
          </div>
          <p style="font-size:13px; color:var(--ink-soft); line-height:1.6;">Your submission has been recorded. The recruiter will review your results, and you'll be notified of the next step by email.</p>
          <button class="btn-primary" style="width:100%; margin-bottom:8px;" (click)="viewHistory()">View submission history</button>
          <button class="btn-ghost" style="width:100%;" (click)="exit()">Return to dashboard</button>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class CandidateAssessmentResultComponent implements OnInit {
  assessmentId = '';
  result = signal<AssessmentResultResponse | null>(null);

  ringOffset = computed(() => {
    const pct = this.result()?.scorePercent ?? 0;
    return (175.9 * (1 - pct / 100)).toFixed(1);
  });

  constructor(
    public state: AppStateService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.assessmentId = this.route.snapshot.paramMap.get('id') || '';
    if (this.assessmentId) {
      this.state.getAssessmentResult(this.assessmentId).subscribe({
        next: (res) => this.result.set(res.data),
        error: () => this.state.showToast('Could not load your result.'),
      });
    }
  }

  viewHistory() {
    this.router.navigate(['/candidate/coding-assessments']);
  }

  exit() {
    this.router.navigate(['/candidate/dashboard']);
  }
}
