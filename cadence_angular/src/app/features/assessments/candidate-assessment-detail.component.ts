import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { ProgrammingLanguage } from '../../core/models/coding-assessment.model';

const LANG_LABEL_TO_ENUM: Record<string, ProgrammingLanguage> = {
  Java: ProgrammingLanguage.JAVA,
  Python: ProgrammingLanguage.PYTHON,
  JavaScript: ProgrammingLanguage.JAVASCRIPT,
  'C++': ProgrammingLanguage.CPP,
  SQL: ProgrammingLanguage.SQL,
};

@Component({
  selector: 'app-candidate-assessment-detail',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div id="assessment-view" class="show">
      <!-- INTRO / SETUP -->
      <div class="iv-intro-screen" id="ca-intro" *ngIf="step() === 1 && intro() as intro">
        <div class="iv-intro-card">
          <div style="display:flex; align-items:center; gap:12px; margin-bottom:6px;">
            <div class="mark" style="width:44px; height:44px; font-size:16px; flex-shrink:0;">&#123; &#125;</div>
            <div style="text-align:left;">
              <div style="font-size:12.5px; font-weight:600; color:var(--ink-soft);">{{ intro.companyName }}</div>
              <span class="badge published">{{ intro.status }}</span>
            </div>
          </div>
          <h1>Coding Assessment — {{ intro.jobTitle }}</h1>
          <p>{{ intro.companyName }} · {{ intro.durationMinutes }} minutes · {{ intro.questionCount }} problems · Auto-submits when time runs out</p>
          <div class="review-block" style="text-align:left; margin-bottom:18px;">
            <div class="review-row"><span>Position</span><span>{{ intro.jobTitle }}</span></div>
            <div class="review-row"><span>Duration</span><span>{{ intro.durationMinutes }} minutes</span></div>
            <div class="review-row"><span>Questions</span><span>{{ intro.questionCount }}</span></div>
            <div class="review-row"><span>Languages allowed</span><span>{{ intro.allowedLanguages.join(', ') }}</span></div>
          </div>
          <p *ngIf="intro.candidateInstructions" style="font-size:12.5px; color:var(--ink-soft); margin-bottom:14px;">{{ intro.candidateInstructions }}</p>
          <ul class="iv-tips">
            <li>Tab switching and window focus are monitored and shown to the recruiter</li>
            <li>You can run your code against sample test cases before submitting</li>
            <li>Submitting early doesn't affect your score — only correctness and time do</li>
          </ul>
          <div class="field-full">
            <label>Preferred language</label>
            <select id="ca-language-select" (change)="preferredLang.set($any($event.target).value)">
              <option *ngFor="let lang of intro.allowedLanguages" [value]="lang">{{ lang }}</option>
            </select>
          </div>
          <button class="btn-primary" style="width:100%;" (click)="step.set(2)">Start assessment</button>
          <button class="btn-ghost" style="width:100%; margin-top:8px;" (click)="goBack()">Not now</button>
        </div>
      </div>

      <!-- RULES & PROCTORING AGREEMENT -->
      <div class="iv-intro-screen" id="ca-rules" *ngIf="step() === 2">
        <div class="iv-intro-card" style="max-width:520px;">
          <h1>Assessment rules</h1>
          <p>Please read carefully before you begin — this attempt is proctored.</p>
          <ul class="rules-checklist">
            <li><svg viewBox="0 0 24 24"><path d="M23 7l-7 10-6-6"/><path d="M14 2H4a2 2 0 00-2 2v16a2 2 0 002 2h13a2 2 0 002-2V9z"/></svg>Camera may be enabled to verify identity</li>
            <li><svg viewBox="0 0 24 24"><path d="M12 1a4 4 0 00-4 4v6a4 4 0 008 0V5a4 4 0 00-4-4z"/><path d="M19 10v1a7 7 0 01-14 0v-1M12 19v3"/></svg>Microphone permission may be requested</li>
            <li><svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M3 9h18"/></svg>No tab switching — every switch is logged and flagged</li>
            <li><svg viewBox="0 0 24 24"><rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15V5a2 2 0 012-2h10"/><path d="M6 6l12 12" stroke="var(--danger)"/></svg>No copy-paste inside the code editor</li>
            <li><svg viewBox="0 0 24 24"><path d="M23 7l-7 5 7 5V7z"/><rect x="1" y="5" width="15" height="14" rx="2"/></svg>No screen recording or screen sharing tools</li>
            <li><svg viewBox="0 0 24 24"><rect x="7" y="2" width="10" height="20" rx="2"/><path d="M11 18h2"/></svg>No mobile phones or secondary devices at your desk</li>
            <li><svg viewBox="0 0 24 24"><path d="M5 12.55a11 11 0 0114.08 0M1.42 9a16 16 0 0121.16 0M8.53 16.11a6 6 0 016.95 0M12 20h.01"/></svg>A stable internet connection is required throughout</li>
            <li><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg>The timer cannot be paused once started</li>
            <li><svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg>The assessment auto-submits when time runs out</li>
            <li><svg viewBox="0 0 24 24"><path d="M12 9v4M12 17h.01"/><circle cx="12" cy="12" r="9"/></svg>Refreshing or closing the browser may forfeit your attempt</li>
            <li><svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><path d="M8 3v4M16 3v4M3 9h18"/></svg>The assessment runs in fullscreen mode</li>
            <li><svg viewBox="0 0 24 24"><path d="M4 7V4a1 1 0 011-1h4M20 7V4a1 1 0 00-1-1h-4M4 17v3a1 1 0 001 1h4M20 17v3a1 1 0 01-1 1h-4"/></svg>Typing activity and focus are monitored throughout</li>
            <li><svg viewBox="0 0 24 24"><path d="M10.29 3.86L1.82 18a2 2 0 001.71 3h16.94a2 2 0 001.71-3L13.71 3.86a2 2 0 00-3.42 0z"/><path d="M12 9v4M12 17h.01"/></svg>Suspicious activity is logged and shared with the recruiter</li>
          </ul>
          <label class="agree-checkbox-row">
            <input type="checkbox" id="ca-rules-agree" (change)="agreed.set($any($event.target).checked)">
            <span>I agree to the assessment rules.</span>
          </label>
          <button class="btn-primary" style="width:100%;" id="ca-rules-start-btn" [disabled]="!agreed()" (click)="startExam()">Start assessment</button>
          <button class="btn-ghost" style="width:100%; margin-top:8px;" (click)="step.set(1)">Back</button>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class CandidateAssessmentDetailComponent implements OnInit {
  step = signal<number>(1);
  agreed = signal<boolean>(false);
  preferredLang = signal<string>('JAVA');
  assessmentId = '';

  intro = computed(() => this.state.candidateAssessmentIntro());

  constructor(
    public state: AppStateService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.route.paramMap.subscribe(params => {
      this.assessmentId = params.get('id') || '';
      if (this.assessmentId) this.state.loadCandidateAssessmentIntro(this.assessmentId);
    });
  }

  goBack() {
    this.router.navigate(['/candidate/coding-assessments']);
  }

  startExam() {
    this.state.acceptAssessmentRules(this.assessmentId).subscribe({
      next: () => {
        this.state.showToast('Launching workspace in fullscreen mode...');
        const lang = LANG_LABEL_TO_ENUM[this.preferredLang()] ?? this.preferredLang();
        this.router.navigate(['/candidate/coding-assessments', this.assessmentId, 'exam'], {
          queryParams: { lang },
        });
      },
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not accept the rules.'),
    });
  }
}
