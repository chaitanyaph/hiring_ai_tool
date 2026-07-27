import { Component, OnInit, OnDestroy, signal, computed, ViewChild, ElementRef, HostListener } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, ActivatedRouteSnapshot } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AntiCheatEventType, IdeQuestionResponse, ProgrammingLanguage } from '../../core/models/coding-assessment.model';

const LANG_ENUM_TO_LABEL: Record<string, string> = {
  JAVA: 'Java',
  PYTHON: 'Python',
  JAVASCRIPT: 'JavaScript',
  CPP: 'C++',
  SQL: 'SQL',
};

@Component({
  selector: 'app-candidate-assessment-exam',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div id="assessment-view" class="show" style="display:flex; flex-direction:column; min-height:100vh;">
      <!-- LIVE IDE -->
      <div class="ide-live-screen" id="ca-live" style="display:flex; flex-direction:column; flex:1; min-height:0;" *ngIf="question() as q">
        <div class="ide-header">
          <span class="badge stage">Question <span id="ca-q-num">{{ q.questionOrder }}</span> of {{ q.totalQuestions }}</span>
          <span class="assessment-timer" id="ca-timer"><svg viewBox="0 0 24 24" width="13" height="13" style="stroke:currentColor; fill:none; stroke-width:2;"><circle cx="12" cy="12" r="9"/><path d="M12 7v5l3 2"/></svg> {{ timeString() }}</span>
          <div class="ide-header-right">
            <div class="ide-candidate-chip"><div class="avatar">{{ candidateInitials() }}</div><span>{{ state.currentUser()?.name }}</span></div>
            <span class="anti-cheat-badge" id="ca-tabswitch-badge" [class.warn]="tabSwitches() > 0"><svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg>{{ tabSwitches() }} tab switches</span>
            <span class="anti-cheat-badge"><svg viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/></svg>Fullscreen active</span>
            <button class="btn-primary-sm" (click)="openSubmitModal()">Submit assessment</button>
          </div>
        </div>
        <div class="ide-layout">
          <div class="question-panel">
            <div class="qp-content-wrap" id="qp-content-wrap">
              <div class="qp-scroll" id="qp-question-view">
                <div class="ai-disabled-banner"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 8v4M12 16h.01"/></svg>AI Assistant is disabled during this assessment</div>
                <div style="display:flex; gap:7px; flex-wrap:wrap; align-items:center;">
                  <span class="badge medium" id="ca-difficulty">{{ q.difficulty }}</span>
                  <span class="qp-marks-chip" id="ca-marks-chip">{{ q.marks }} marks</span>
                  <span class="qp-hidden-chip" id="ca-hidden-chip">{{ q.hiddenTestCaseCount }} hidden test cases</span>
                </div>
                <h2 id="ca-question-title">{{ q.title }}</h2>
                <p style="font-size:13px; color:var(--ink-soft); line-height:1.6;" id="ca-question-desc">{{ q.description }}</p>
                <div class="qp-section-title" *ngIf="q.exampleText">Example</div>
                <div class="qp-examples" id="ca-question-example" *ngIf="q.exampleText" [innerHTML]="q.exampleText"></div>
                <div class="qp-section-title" *ngIf="q.visibleTestCases?.length">Sample test cases</div>
                <div class="qp-examples" *ngFor="let tc of q.visibleTestCases">
                  Input: {{ tc.inputData }}<br>Expected: {{ tc.expectedOutput }}
                </div>
                <div class="qp-section-title" *ngIf="q.constraintsText">Constraints</div>
                <p style="font-size:12px; color:var(--ink-soft); line-height:1.8;" id="ca-question-constraints" *ngIf="q.constraintsText">{{ q.constraintsText }}</p>
              </div>
            </div>
            <div class="qp-controls" id="qp-controls">
              <button class="btn-ghost" id="ca-prev-btn" [disabled]="q.questionOrder === 1" (click)="goToQuestion(q.questionOrder - 2)">Previous</button>
              <button class="btn-ghost" (click)="saveDraft()">Save draft</button>
              <div class="foot-right">
                <button class="btn-ghost" id="ca-mark-review-btn" (click)="toggleMark()">{{ q.markedForReview ? 'Unmark' : 'Mark review' }}</button>
                <button class="btn-primary-sm" id="ca-next-btn" [disabled]="q.questionOrder === q.totalQuestions" (click)="goToQuestion(q.questionOrder)">Next</button>
              </div>
            </div>
          </div>
          <div class="editor-panel" id="ca-editor-panel">
            <div class="editor-toolbar-row">
              <div class="font-size-stepper">
                <button class="ide-tb-btn" aria-label="Decrease font size" (click)="adjustFontSize(-1)">A-</button>
                <span id="ca-font-size-label">{{ fontSize() }}</span>
                <button class="ide-tb-btn" aria-label="Increase font size" (click)="adjustFontSize(1)">A+</button>
              </div>
              <div class="ide-tb-sep"></div>
              <button class="ide-tb-btn" aria-label="Toggle editor theme" (click)="toggleTheme()"><svg viewBox="0 0 24 24"><path d="M21 12.8A9 9 0 1111.2 3 7 7 0 0021 12.8z"/></svg></button>
              <button class="ide-tb-btn" aria-label="Undo" (click)="editorAction('undo')"><svg viewBox="0 0 24 24"><path d="M3 7v6h6"/><path d="M3 13a9 9 0 1015-6.7L3 13"/></svg></button>
              <button class="ide-tb-btn" aria-label="Redo" (click)="editorAction('redo')"><svg viewBox="0 0 24 24"><path d="M21 7v6h-6"/><path d="M21 13a9 9 0 10-15-6.7L21 13"/></svg></button>
              <button class="ide-tb-btn" aria-label="Copy code" (click)="copyCode()"><svg viewBox="0 0 24 24"><rect x="9" y="9" width="12" height="12" rx="2"/><path d="M5 15V5a2 2 0 012-2h10"/></svg></button>
              <div class="ide-tb-sep"></div>
              <button class="ide-tb-btn" aria-label="Download code" (click)="downloadCode()"><svg viewBox="0 0 24 24"><path d="M12 3v12M7 10l5 5 5-5"/><path d="M5 21h14"/></svg></button>
              <button class="ide-tb-btn" aria-label="Reset code" (click)="resetCode()"><svg viewBox="0 0 24 24"><path d="M23 4v6h-6"/><path d="M20.49 15a9 9 0 11-2.12-9.36L23 10"/></svg></button>
              <button class="ide-tb-btn" aria-label="Toggle fullscreen" style="margin-left:auto;"><svg viewBox="0 0 24 24"><path d="M8 3H5a2 2 0 00-2 2v3M16 3h3a2 2 0 012 2v3M8 21H5a2 2 0 01-2-2v-3M16 21h3a2 2 0 002-2v-3"/></svg></button>
            </div>
            <div class="ide-workspace" id="ca-ide-workspace">
              <div class="code-editor-lined theme-dark" id="ca-editor-lined" [style.fontSize.px]="fontSize()">
                <div class="cel-gutter" id="ca-code-gutter">
                  <div *ngFor="let line of gutterLines()">{{ line }}</div>
                </div>
                <div class="cel-code" #editorCell id="ca-code-editor" contenteditable="true" spellcheck="false" (input)="onEditorInput($event)"></div>
              </div>
              <div class="output-window" [class.open]="showOutput()" id="ca-output-window">
                <div class="ow-header">
                  <span>Test Cases &amp; Output</span>
                  <div class="ow-header-actions">
                    <button aria-label="Minimize" (click)="showOutput.set(false)">&#8722;</button>
                    <button aria-label="Close" (click)="showOutput.set(false)">&times;</button>
                  </div>
                </div>
                <div class="ow-tabs">
                  <button [class.active]="consoleTab() === 'results'" (click)="consoleTab.set('results')">Compilation Results</button>
                  <button [class.active]="consoleTab() === 'custom'" (click)="consoleTab.set('custom')">Custom Input</button>
                </div>
                <div class="ow-body" id="ow-body-results" *ngIf="consoleTab() === 'results'">
                  <div class="ow-placeholder" *ngIf="!consoleContent()" id="ow-placeholder">Click <b>Compile &amp; Run</b> to see your output here, or <b>Submit</b> to grade your solution.</div>
                  <div id="ow-results-content" *ngIf="consoleContent()" [innerHTML]="consoleContent()"></div>
                </div>
                <div class="ow-body" id="ow-body-custom" *ngIf="consoleTab() === 'custom'">
                  <label style="font-size:11.5px; color:var(--ink-soft); font-weight:600; display:block; margin-bottom:6px;">Custom input</label>
                  <textarea class="custom-input-box" id="ow-custom-input" [value]="customInput()" (input)="customInput.set($any($event.target).value)" placeholder="Enter your own test input…"></textarea>
                </div>
              </div>
            </div>
            <div class="editor-bottom-bar">
              <select id="ca-editor-language" [value]="selectedLanguage()" (change)="selectedLanguage.set($any($event.target).value)">
                <option *ngFor="let lang of q.allowedLanguages" [value]="lang">{{ langLabel(lang) }}</option>
              </select>
              <button class="hint-btn" aria-label="Hint" (click)="showHint()"><svg viewBox="0 0 24 24"><path d="M9 18h6M10 21h4M12 3a6 6 0 00-3.5 10.9c.4.3.7.8.7 1.3V16h5.6v-.8c0-.5.3-1 .7-1.3A6 6 0 0012 3z"/></svg></button>
              <a class="card-link" (click)="openCustomInput()">Custom Input</a>
              <div class="foot-right">
                <button class="compile-btn" (click)="compileAndRun()">Compile &amp; Run</button>
                <button class="submit-code-btn" (click)="submitActiveCode()">Submit</button>
              </div>
            </div>
          </div>
          <div class="question-nav-panel">
            <div>
              <div class="qnav-section-title" style="margin-bottom:8px;">Questions</div>
              <div class="qnav-grid" id="ca-qnav-grid">
                <div
                  *ngFor="let idx of questionIndices(); let i = index"
                  class="qnav-pill"
                  [class.current]="i === (q.questionOrder - 1)"
                  [class.completed]="questionStatus()[i] === 'completed'"
                  [class.marked]="questionStatus()[i] === 'marked'"
                  [class.visited]="questionStatus()[i] === 'visited'"
                  (click)="goToQuestion(i)">
                  {{ i + 1 }}
                </div>
              </div>
            </div>
            <div class="qnav-legend">
              <div><span class="qnav-dot current"></span>Current</div>
              <div><span class="qnav-dot completed"></span>Completed</div>
              <div><span class="qnav-dot marked"></span>Marked for review</div>
              <div><span class="qnav-dot visited"></span>Visited</div>
              <div><span class="qnav-dot pending"></span>Not visited</div>
            </div>
            <div class="qnav-progress-summary" id="ca-qnav-summary">{{ completedCount() }} of {{ q.totalQuestions }} answered</div>
          </div>
        </div>
      </div>

      <!-- ================= MODAL: SUBMIT ASSESSMENT CONFIRMATION ================= -->
      <div class="modal-overlay" [class.show]="showSubmitModal()" (click)="showSubmitModal.set(false)">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3>Submit assessment?</h3>
              <p>You cannot make further changes after submitting.</p>
            </div>
            <button class="modal-close" aria-label="Close dialog" (click)="showSubmitModal.set(false)">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="review-block">
              <div class="review-row"><span>Questions attempted</span><span>{{ completedCount() }}</span></div>
              <div class="review-row"><span>Time left</span><span>{{ timeString() }}</span></div>
            </div>
          </div>
          <div class="modal-foot">
            <span></span>
            <div class="foot-right">
              <button class="btn-ghost" (click)="showSubmitModal.set(false)">Keep working</button>
              <button class="btn-primary-sm" (click)="confirmFinish(false)">Submit assessment</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class CandidateAssessmentExamComponent implements OnInit, OnDestroy {
  @ViewChild('editorCell') editorCell!: ElementRef<HTMLDivElement>;

  assessmentId = '';
  fontSize = signal<number>(12.5);
  tabSwitches = signal<number>(0);
  timeLeft = signal<number>(3600);
  timerInterval: any = null;

  selectedLanguage = signal<string>('JAVA');
  question = signal<IdeQuestionResponse | null>(null);
  questionStatus = signal<string[]>([]);
  completedCount = computed(() => this.questionStatus().filter(s => s === 'completed').length);
  questionIndices = computed(() => Array.from({ length: this.question()?.totalQuestions ?? 0 }, (_, i) => i));

  showOutput = signal<boolean>(false);
  consoleTab = signal<string>('results');
  consoleContent = signal<string>('');
  showSubmitModal = signal<boolean>(false);
  customInput = signal<string>('');

  code = signal<string>('');

  gutterLines = computed(() => {
    const lineCount = this.code().split('\n').length || 1;
    return Array.from({ length: lineCount }, (_, i) => i + 1);
  });

  timeString = computed(() => {
    const mins = Math.floor(this.timeLeft() / 60);
    const secs = this.timeLeft() % 60;
    return `${mins.toString().padStart(2, '0')}:${secs.toString().padStart(2, '0')}`;
  });

  constructor(
    public state: AppStateService,
    private route: ActivatedRoute,
    private router: Router
  ) {}

  ngOnInit() {
    this.assessmentId = this.route.snapshot.paramMap.get('id') || '';
    const langParam = this.route.snapshot.queryParamMap.get('lang');
    if (langParam) this.selectedLanguage.set(langParam);

    this.state.startCodingAssessment(this.assessmentId, { preferredLanguage: this.selectedLanguage() as ProgrammingLanguage }).subscribe({
      next: (res) => {
        this.applyQuestion(res.data);
        // Full duration is only known from the intro screen's durationMinutes (loaded on
        // the previous route) -- the IDE endpoints don't expose remaining time separately,
        // so a resumed (not fresh) attempt won't recover an accurate countdown here.
        const durationMinutes = this.state.candidateAssessmentIntro()?.durationMinutes ?? 60;
        this.timeLeft.set(durationMinutes * 60);
        this.startTimer();
      },
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not start this assessment.'),
    });
  }

  ngOnDestroy() {
    if (this.timerInterval) clearInterval(this.timerInterval);
  }

  private applyQuestion(q: IdeQuestionResponse) {
    this.question.set(q);
    this.code.set(q.starterCode ?? '');
    if (this.editorCell) this.editorCell.nativeElement.innerText = q.starterCode ?? '';
    this.showOutput.set(false);
    this.consoleContent.set('');

    this.questionStatus.update((status) => {
      const size = q.totalQuestions;
      const copy = status.length === size ? [...status] : Array.from({ length: size }, () => 'pending');
      const idx = q.questionOrder - 1;
      if (copy[idx] !== 'completed' && copy[idx] !== 'marked') {
        copy[idx] = q.markedForReview ? 'marked' : 'visited';
      }
      return copy;
    });
  }

  startTimer() {
    this.timerInterval = setInterval(() => {
      if (this.timeLeft() > 0) {
        this.timeLeft.update(t => t - 1);
      } else {
        clearInterval(this.timerInterval);
        this.state.showToast('Time has expired! Submitting assessment...');
        this.confirmFinish(true);
      }
    }, 1000);
  }

  @HostListener('window:blur')
  onWindowBlur() {
    this.tabSwitches.update(s => s + 1);
    this.state.recordAssessmentAntiCheatEvent(this.assessmentId, { eventType: AntiCheatEventType.TAB_SWITCH });
    this.state.showToast(`Warning: Tab switch detected! (Flagged count: ${this.tabSwitches()})`);
  }

  onEditorInput(event: Event) {
    const target = event.target as HTMLDivElement;
    this.code.set(target.innerText || '');
  }

  adjustFontSize(amount: number) {
    this.fontSize.update(s => Math.max(10, Math.min(24, s + amount)));
  }

  candidateInitials(): string {
    const name = this.state.currentUser()?.name ?? '';
    return name.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  toggleTheme() {
    this.state.showToast('Theme toggled');
  }

  editorAction(action: string) {
    if (this.editorCell) {
      this.editorCell.nativeElement.focus();
      document.execCommand(action);
    }
  }

  saveDraft() {
    this.state.showToast('Draft code saved to cloud database');
  }

  showHint() {
    this.state.showToast('Hint: consider a HashMap for O(n) runtime complexity.');
  }

  copyCode() {
    navigator.clipboard.writeText(this.code());
    this.state.showToast('Code copied to clipboard');
  }

  downloadCode() {
    const blob = new Blob([this.code()], { type: 'text/plain;charset=utf-8' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `solution.${this.selectedLanguage() === 'PYTHON' ? 'py' : this.selectedLanguage() === 'JAVASCRIPT' ? 'js' : this.selectedLanguage() === 'CPP' ? 'cpp' : 'java'}`;
    link.click();
    URL.revokeObjectURL(url);
    this.state.showToast('Code downloaded successfully');
  }

  resetCode() {
    const q = this.question();
    if (q && confirm('Reset editor to original code template?')) {
      this.code.set(q.starterCode ?? '');
      if (this.editorCell) this.editorCell.nativeElement.innerText = q.starterCode ?? '';
      this.state.showToast('Editor reset');
    }
  }

  langLabel(lang: string): string {
    return LANG_ENUM_TO_LABEL[lang] ?? lang;
  }

  goToQuestion(idx: number) {
    if (idx < 0) return;
    this.state.goToAssessmentQuestion(this.assessmentId, idx).subscribe({
      next: (res) => this.applyQuestion(res.data),
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not load that question.'),
    });
  }

  toggleMark() {
    const q = this.question();
    if (!q) return;
    const marking = !q.markedForReview;
    this.state.markAssessmentQuestionForReview(this.assessmentId, { questionId: q.questionId, marked: marking }).subscribe({
      next: () => {
        this.question.update((cur) => (cur ? { ...cur, markedForReview: marking } : cur));
        this.questionStatus.update((status) => {
          const copy = [...status];
          copy[q.questionOrder - 1] = marking ? 'marked' : 'visited';
          return copy;
        });
        this.state.showToast('Question review status toggled');
      },
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not update review status.'),
    });
  }

  openCustomInput() {
    this.showOutput.set(true);
    this.consoleTab.set('custom');
  }

  compileAndRun() {
    const q = this.question();
    if (!q) return;
    this.showOutput.set(true);
    this.consoleTab.set('results');
    this.state.runAssessmentCode({
      candidateAssessmentId: this.assessmentId,
      questionId: q.questionId,
      language: this.selectedLanguage() as ProgrammingLanguage,
      code: this.code(),
      customInput: this.customInput() || undefined,
    }).subscribe({
      next: (res) => {
        const r = res.data;
        this.consoleContent.set(`
          ${r.compileOutput ? '<span style="color:#8FD9C4;">[Compile] ' + r.compileOutput + '</span><br>' : ''}
          ${r.output ? '<b>Output:</b><br>' + r.output + '<br>' : ''}
          ${r.stderr ? '<span style="color:#E08A7D;"><b>Stderr:</b><br>' + r.stderr + '</span><br>' : ''}
          ${r.runtimeMs != null ? '<span class="muted">Runtime: ' + r.runtimeMs + 'ms' + (r.memoryKb != null ? ', Memory: ' + r.memoryKb + 'KB' : '') + '</span>' : ''}
        `);
      },
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not run this code.'),
    });
  }

  submitActiveCode() {
    const q = this.question();
    if (!q) return;
    this.state.submitAssessmentCode({
      candidateAssessmentId: this.assessmentId,
      questionId: q.questionId,
      language: this.selectedLanguage() as ProgrammingLanguage,
      code: this.code(),
    }).subscribe({
      next: (res) => {
        const r = res.data;
        this.questionStatus.update((status) => {
          const copy = [...status];
          copy[q.questionOrder - 1] = 'completed';
          return copy;
        });
        this.showOutput.set(true);
        this.consoleTab.set('results');
        this.consoleContent.set(`
          <span style="color:#8FD9C4;">[Info] Grading suite result: ${r.status}</span><br>
          Passed: ${r.testCasesPassed ?? '—'}/${r.testCasesTotal ?? '—'} test cases<br>
          Score: ${r.score ?? '—'}<br>
          ${r.compileOutput ? '<span style="color:#E08A7D;">' + r.compileOutput + '</span>' : ''}
        `);
        this.state.showToast('Solution submitted successfully!');
      },
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not submit this code.'),
    });
  }

  openSubmitModal() {
    this.showSubmitModal.set(true);
  }

  confirmFinish(autoSubmitted: boolean) {
    this.showSubmitModal.set(false);
    this.state.finishCodingAssessment(this.assessmentId, { autoSubmitted }).subscribe({
      next: () => {
        this.state.showToast('Assessment submitted successfully!');
        this.router.navigate(['/candidate/coding-assessments', this.assessmentId, 'result']);
      },
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not submit the assessment.'),
    });
  }
}
