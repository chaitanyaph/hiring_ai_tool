import { Component, OnInit, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import {
  CreateQuestionRequest,
  Difficulty,
  ProgrammingLanguage,
  QuestionResponse,
  QuestionStatus,
  StarterCodeItem,
  TestCaseItem,
  TestCaseVisibility,
} from '../../core/models/coding-assessment.model';

/** Recruiter-facing question bank: create/edit/archive coding questions and manage their test cases, feeding the assessment builder's question picker. Layout follows this app's existing page-head/card/table conventions -- no new visual language introduced. */
@Component({
  selector: 'app-question-bank',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="jobs-viewport">

      <!-- ============ LIST VIEW ============ -->
      <ng-container *ngIf="view() === 'list'">
        <div class="page-head">
          <div>
            <h1>Question Bank</h1>
            <p>{{ state.questionBankList().length }} question{{ state.questionBankList().length === 1 ? '' : 's' }} -- reusable across every coding assessment</p>
          </div>
          <div class="page-head-actions">
            <button class="btn-primary-sm" (click)="openCreate()">
              <svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2.2" stroke-linecap="round"/></svg>
              New question
            </button>
          </div>
        </div>

        <div class="card">
          <div class="filter-row">
            <div class="filter-tabs">
              <button [class.active]="statusFilter() === ''" (click)="setStatusFilter('')">All</button>
              <button [class.active]="statusFilter() === 'DRAFT'" (click)="setStatusFilter('DRAFT')">Draft</button>
              <button [class.active]="statusFilter() === 'ACTIVE'" (click)="setStatusFilter('ACTIVE')">Active</button>
              <button [class.active]="statusFilter() === 'INACTIVE'" (click)="setStatusFilter('INACTIVE')">Inactive</button>
              <button [class.active]="statusFilter() === 'ARCHIVED'" (click)="setStatusFilter('ARCHIVED')">Archived</button>
            </div>
            <div class="search-inline">
              <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8" fill="none"/></svg>
              <input placeholder="Search by title…" #searchVal [value]="searchTerm()" (input)="setSearch(searchVal.value)">
            </div>
            <select class="filter-select" [value]="difficultyFilter()" (change)="setDifficultyFilter($any($event.target).value)">
              <option value="">All difficulties</option>
              <option value="EASY">Easy</option>
              <option value="MEDIUM">Medium</option>
              <option value="HARD">Hard</option>
              <option value="MIXED">Mixed</option>
            </select>
          </div>

          <table class="table" *ngIf="state.questionBankList().length > 0; else emptyState">
            <thead>
              <tr>
                <th>Title</th>
                <th>Status</th>
                <th>Difficulty</th>
                <th>Marks</th>
                <th>Test cases</th>
                <th>Used in</th>
                <th></th>
              </tr>
            </thead>
            <tbody>
              <tr *ngFor="let q of state.questionBankList()">
                <td>
                  <b style="font-weight:600; color:var(--ink); cursor:pointer;" (click)="openEdit(q)">{{ q.title }}</b>
                  <div class="muted" style="font-size:11.5px; margin-top:2px;" *ngIf="q.topics.length">{{ q.topics.join(', ') }}</div>
                </td>
                <td><span class="badge" [ngClass]="q.status.toLowerCase()">{{ q.status }}</span></td>
                <td class="muted">{{ q.difficulty }}</td>
                <td>{{ q.marks }}</td>
                <td class="muted">{{ q.testCaseCount }}</td>
                <td class="muted">{{ q.usedInAssessmentCount }} assessment{{ q.usedInAssessmentCount === 1 ? '' : 's' }}</td>
                <td style="text-align:right; white-space:nowrap; vertical-align:middle;">
                  <span class="row-link" (click)="openEdit(q)">Edit</span>
                  <span class="row-link" style="margin-left:12px;" (click)="state.duplicateQuestion(q.id)">Duplicate</span>
                  <span *ngIf="q.status !== 'ACTIVE' && q.status !== 'ARCHIVED'" class="row-link" style="margin-left:12px;" (click)="state.activateQuestion(q.id)">Activate</span>
                  <span *ngIf="q.status === 'ACTIVE'" class="row-link" style="margin-left:12px;" (click)="state.deactivateQuestion(q.id)">Deactivate</span>
                  <span *ngIf="q.status !== 'ARCHIVED'" class="row-link" style="margin-left:12px;" (click)="state.archiveQuestion(q.id)">Archive</span>
                  <span *ngIf="q.usedInAssessmentCount === 0" class="row-link" style="margin-left:12px;" (click)="confirmDelete(q)">Delete</span>
                </td>
              </tr>
            </tbody>
          </table>

          <ng-template #emptyState>
            <div class="empty-state">
              <svg viewBox="0 0 24 24" fill="none"><circle cx="11" cy="11" r="7" stroke="currentColor" stroke-width="1.8"/><path d="M21 21l-4-4" stroke="currentColor" stroke-width="1.8" stroke-linecap="round"/></svg>
              <p>{{ state.questionBankLoading() ? 'Loading questions…' : 'No questions yet.' }}</p>
              <button *ngIf="!state.questionBankLoading()" (click)="openCreate()">Create your first question</button>
            </div>
          </ng-template>
        </div>
      </ng-container>

      <!-- ============ EDITOR VIEW ============ -->
      <ng-container *ngIf="view() === 'editor'">
        <div class="page-head">
          <div>
            <h1>{{ editingId() ? 'Edit question' : 'New question' }}</h1>
            <p>Saved questions become selectable from the assessment builder once activated</p>
          </div>
          <div class="page-head-actions">
            <button class="btn-ghost" (click)="backToList()">Cancel</button>
            <button class="btn-primary-sm" [disabled]="editorLoading()" (click)="save()">{{ editingId() ? 'Save changes' : 'Create question' }}</button>
          </div>
        </div>

        <div class="card" *ngIf="editorLoading()" style="padding:40px; text-align:center; color:var(--ink-soft);">Loading question…</div>

        <div class="card" *ngIf="!editorLoading()" style="padding:20px; display:flex; flex-direction:column; gap:16px;">
          <div class="field-full">
            <label>Title</label>
            <input placeholder="e.g. Two Sum" [value]="title()" (input)="title.set($any($event.target).value)">
          </div>

          <div class="form-row-2">
            <div class="field-full">
              <label>Difficulty</label>
              <select [value]="difficulty()" (change)="difficulty.set($any($event.target).value)">
                <option value="EASY">Easy</option>
                <option value="MEDIUM">Medium</option>
                <option value="HARD">Hard</option>
                <option value="MIXED">Mixed</option>
              </select>
            </div>
            <div class="field-full">
              <label>Marks</label>
              <input type="number" [value]="marks()" (input)="marks.set($any($event.target).valueAsNumber || 1)">
            </div>
          </div>

          <div class="field-full">
            <label>Problem statement</label>
            <textarea rows="5" placeholder="Describe the problem…" [value]="description()" (input)="description.set($any($event.target).value)"></textarea>
          </div>

          <div class="form-row-2">
            <div class="field-full">
              <label>Input format</label>
              <textarea rows="3" [value]="inputFormat()" (input)="inputFormat.set($any($event.target).value)"></textarea>
            </div>
            <div class="field-full">
              <label>Output format</label>
              <textarea rows="3" [value]="outputFormat()" (input)="outputFormat.set($any($event.target).value)"></textarea>
            </div>
          </div>

          <div class="form-row-2">
            <div class="field-full">
              <label>Example</label>
              <textarea rows="3" [value]="exampleText()" (input)="exampleText.set($any($event.target).value)"></textarea>
            </div>
            <div class="field-full">
              <label>Constraints</label>
              <textarea rows="3" [value]="constraintsText()" (input)="constraintsText.set($any($event.target).value)"></textarea>
            </div>
          </div>

          <div class="field-full">
            <label>Explanation (shown after a candidate solves it)</label>
            <textarea rows="3" [value]="explanation()" (input)="explanation.set($any($event.target).value)"></textarea>
          </div>

          <div class="field-full">
            <label>Hints</label>
            <div *ngFor="let h of hints(); let i = index" style="display:flex; gap:8px; margin-bottom:6px;">
              <input style="flex:1;" [value]="h" (input)="setHint(i, $any($event.target).value)" placeholder="Hint {{ i + 1 }}">
              <button type="button" class="btn-ghost" (click)="removeHint(i)">Remove</button>
            </div>
            <button type="button" class="btn-ghost" (click)="addHint()">+ Add hint</button>
          </div>

          <div class="form-row-2">
            <div class="field-full">
              <label>Tags (comma-separated)</label>
              <input [value]="tagsCsv()" (input)="tagsCsv.set($any($event.target).value)" placeholder="arrays, hashing">
            </div>
            <div class="field-full">
              <label>Topics (comma-separated)</label>
              <input [value]="topicsCsv()" (input)="topicsCsv.set($any($event.target).value)" placeholder="Data Structures, Algorithms">
            </div>
          </div>

          <div class="form-row-2">
            <div class="field-full">
              <label>Time limit (ms)</label>
              <input type="number" [value]="timeLimitMs()" (input)="timeLimitMs.set($any($event.target).valueAsNumber || 2000)">
            </div>
            <div class="field-full">
              <label>Memory limit (MB)</label>
              <input type="number" [value]="memoryLimitMb()" (input)="memoryLimitMb.set($any($event.target).valueAsNumber || 256)">
            </div>
          </div>

          <div class="field-full">
            <label>Allowed languages</label>
            <div class="chip-group">
              <span class="select-chip" [class.on]="langJava()" (click)="langJava.set(!langJava())">Java</span>
              <span class="select-chip" [class.on]="langPy()" (click)="langPy.set(!langPy())">Python</span>
              <span class="select-chip" [class.on]="langJS()" (click)="langJS.set(!langJS())">JavaScript</span>
              <span class="select-chip" [class.on]="langCpp()" (click)="langCpp.set(!langCpp())">C++</span>
              <span class="select-chip" [class.on]="langSQL()" (click)="langSQL.set(!langSQL())">SQL</span>
            </div>
          </div>

          <!-- Test cases -->
          <div class="field-full">
            <label>Test cases ({{ testCases().length }})</label>
            <div *ngFor="let tc of testCases(); let i = index" style="border:1px solid var(--border, #e5e7eb); border-radius:8px; padding:12px; margin-bottom:10px;">
              <div style="display:flex; justify-content:space-between; align-items:center; margin-bottom:8px;">
                <div class="chip-group">
                  <span class="select-chip" [class.on]="tc.visibility === 'VISIBLE'" (click)="setTestCaseVisibility(i, 'VISIBLE')">Visible (sample)</span>
                  <span class="select-chip" [class.on]="tc.visibility === 'HIDDEN'" (click)="setTestCaseVisibility(i, 'HIDDEN')">Hidden</span>
                </div>
                <div>
                  <span class="row-link" (click)="moveTestCase(i, -1)" *ngIf="i > 0">Move up</span>
                  <span class="row-link" style="margin-left:10px;" (click)="moveTestCase(i, 1)" *ngIf="i < testCases().length - 1">Move down</span>
                  <span class="row-link" style="margin-left:10px;" (click)="duplicateTestCaseRow(i)">Duplicate</span>
                  <span class="row-link" style="margin-left:10px;" (click)="removeTestCase(i)" *ngIf="testCases().length > 1">Remove</span>
                </div>
              </div>
              <div class="form-row-2">
                <div class="field-full">
                  <label>Input</label>
                  <textarea rows="2" [value]="tc.inputData || ''" (input)="setTestCaseField(i, 'inputData', $any($event.target).value)"></textarea>
                </div>
                <div class="field-full">
                  <label>Expected output</label>
                  <textarea rows="2" [value]="tc.expectedOutput" (input)="setTestCaseField(i, 'expectedOutput', $any($event.target).value)"></textarea>
                </div>
              </div>
              <div class="form-row-2">
                <div class="field-full">
                  <label>Explanation (optional)</label>
                  <input [value]="tc.explanation || ''" (input)="setTestCaseField(i, 'explanation', $any($event.target).value)">
                </div>
                <div class="field-full">
                  <label>Weight</label>
                  <input type="number" [value]="tc.weight || 1" (input)="setTestCaseWeight(i, $any($event.target).valueAsNumber || 1)">
                </div>
              </div>
            </div>
            <button type="button" class="btn-ghost" (click)="addTestCase()">+ Add test case</button>
          </div>

          <div class="stage-toggle-row" *ngIf="!editingId()">
            <div>
              <div class="st-name">Activate immediately</div>
              <div class="st-sub">Off saves as a draft, hidden from the assessment builder until activated</div>
            </div>
            <div class="switch" [class.on]="activateNow()" (click)="activateNow.set(!activateNow())"></div>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: [`
    .jobs-viewport { padding: 0; display: flex; flex-direction: column; gap: 18px; }
  `]
})
export class QuestionBankComponent implements OnInit {
  view = signal<'list' | 'editor'>('list');
  editingId = signal<string | null>(null);

  searchTerm = signal('');
  difficultyFilter = signal<string>('');
  statusFilter = signal<string>('');
  private searchDebounce: ReturnType<typeof setTimeout> | undefined;

  // editor fields
  title = signal('');
  difficulty = signal<string>('EASY');
  marks = signal(10);
  description = signal('');
  exampleText = signal('');
  constraintsText = signal('');
  inputFormat = signal('');
  outputFormat = signal('');
  explanation = signal('');
  tagsCsv = signal('');
  topicsCsv = signal('');
  hints = signal<string[]>([]);
  timeLimitMs = signal(2000);
  memoryLimitMb = signal(256);
  langJava = signal(true);
  langPy = signal(false);
  langJS = signal(true);
  langCpp = signal(false);
  langSQL = signal(false);
  activateNow = signal(false);
  testCases = signal<TestCaseItem[]>([{ visibility: TestCaseVisibility.VISIBLE, inputData: '', expectedOutput: '', explanation: '', weight: 1 }]);
  editorLoading = signal(false);
  /** No dedicated starter-code editor exists in this UI yet -- round-tripped as-is on save so editing a question that already has starter code stubs doesn't silently delete them (updateQuestion() fully replaces children). */
  private loadedStarterCodes: StarterCodeItem[] = [];

  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadQuestionBank();
  }

  private refresh() {
    this.state.loadQuestionBank(
      (this.difficultyFilter() || undefined) as Difficulty | undefined,
      (this.statusFilter() || undefined) as QuestionStatus | undefined,
      this.searchTerm() || undefined
    );
  }

  setStatusFilter(status: string) { this.statusFilter.set(status); this.refresh(); }
  setDifficultyFilter(difficulty: string) { this.difficultyFilter.set(difficulty); this.refresh(); }
  setSearch(term: string) {
    this.searchTerm.set(term);
    clearTimeout(this.searchDebounce);
    this.searchDebounce = setTimeout(() => this.refresh(), 300);
  }

  openCreate() {
    this.editingId.set(null);
    this.title.set('');
    this.difficulty.set('EASY');
    this.marks.set(10);
    this.description.set('');
    this.exampleText.set('');
    this.constraintsText.set('');
    this.inputFormat.set('');
    this.outputFormat.set('');
    this.explanation.set('');
    this.tagsCsv.set('');
    this.topicsCsv.set('');
    this.hints.set([]);
    this.timeLimitMs.set(2000);
    this.memoryLimitMb.set(256);
    this.langJava.set(true);
    this.langPy.set(false);
    this.langJS.set(true);
    this.langCpp.set(false);
    this.langSQL.set(false);
    this.activateNow.set(false);
    this.testCases.set([{ visibility: TestCaseVisibility.VISIBLE, inputData: '', expectedOutput: '', explanation: '', weight: 1 }]);
    this.loadedStarterCodes = [];
    this.view.set('editor');
  }

  /**
   * The row passed in comes from the list endpoint, which omits testCases/hints/
   * starterCodes for performance -- populating the form from it directly would
   * silently wipe those fields on save (updateQuestion() fully replaces children).
   * Fetch the full detail first and populate from that instead.
   */
  openEdit(row: QuestionResponse) {
    this.editingId.set(row.id);
    this.editorLoading.set(true);
    this.view.set('editor');
    this.state.getQuestionDetail(row.id).subscribe({
      next: (res) => { this.populateForm(res.data); this.editorLoading.set(false); },
      error: (err) => {
        this.state.showToast(err?.error?.message ?? 'Could not load this question.');
        this.editorLoading.set(false);
        this.backToList();
      },
    });
  }

  private populateForm(q: QuestionResponse) {
    this.title.set(q.title);
    this.difficulty.set(q.difficulty);
    this.marks.set(q.marks);
    this.description.set(q.description);
    this.exampleText.set(q.exampleText || '');
    this.constraintsText.set(q.constraintsText || '');
    this.inputFormat.set(q.inputFormat || '');
    this.outputFormat.set(q.outputFormat || '');
    this.explanation.set(q.explanation || '');
    this.tagsCsv.set(q.tags.join(', '));
    this.topicsCsv.set(q.topics.join(', '));
    this.hints.set([...q.hints]);
    this.timeLimitMs.set(q.timeLimitMs);
    this.memoryLimitMb.set(q.memoryLimitMb);
    this.langJava.set(q.allowedLanguages.includes('JAVA'));
    this.langPy.set(q.allowedLanguages.includes('PYTHON'));
    this.langJS.set(q.allowedLanguages.includes('JAVASCRIPT'));
    this.langCpp.set(q.allowedLanguages.includes('CPP'));
    this.langSQL.set(q.allowedLanguages.includes('SQL'));
    this.testCases.set(q.testCases.length
      ? q.testCases.map((tc) => ({ visibility: tc.visibility, inputData: tc.inputData, expectedOutput: tc.expectedOutput, explanation: tc.explanation, weight: tc.weight }))
      : [{ visibility: TestCaseVisibility.VISIBLE, inputData: '', expectedOutput: '', explanation: '', weight: 1 }]);
    this.loadedStarterCodes = Object.entries(q.starterCodes || {}).map(([language, code]) => ({ language: language as ProgrammingLanguage, code }));
  }

  backToList() {
    this.view.set('list');
    this.refresh();
  }

  addHint() { this.hints.update((h) => [...h, '']); }
  removeHint(i: number) { this.hints.update((h) => h.filter((_, idx) => idx !== i)); }
  setHint(i: number, value: string) { this.hints.update((h) => h.map((v, idx) => (idx === i ? value : v))); }

  addTestCase() {
    this.testCases.update((tcs) => [...tcs, { visibility: TestCaseVisibility.HIDDEN, inputData: '', expectedOutput: '', explanation: '', weight: 1 }]);
  }
  removeTestCase(i: number) { this.testCases.update((tcs) => tcs.filter((_, idx) => idx !== i)); }
  duplicateTestCaseRow(i: number) {
    this.testCases.update((tcs) => { const copy = { ...tcs[i] }; const next = [...tcs]; next.splice(i + 1, 0, copy); return next; });
  }
  moveTestCase(i: number, direction: -1 | 1) {
    this.testCases.update((tcs) => {
      const next = [...tcs];
      const j = i + direction;
      if (j < 0 || j >= next.length) return next;
      [next[i], next[j]] = [next[j], next[i]];
      return next;
    });
  }
  setTestCaseVisibility(i: number, visibility: 'VISIBLE' | 'HIDDEN') {
    this.testCases.update((tcs) => tcs.map((tc, idx) => (idx === i ? { ...tc, visibility: visibility as TestCaseVisibility } : tc)));
  }
  setTestCaseField(i: number, field: 'inputData' | 'expectedOutput' | 'explanation', value: string) {
    this.testCases.update((tcs) => tcs.map((tc, idx) => (idx === i ? { ...tc, [field]: value } : tc)));
  }
  setTestCaseWeight(i: number, weight: number) {
    this.testCases.update((tcs) => tcs.map((tc, idx) => (idx === i ? { ...tc, weight } : tc)));
  }

  confirmDelete(q: QuestionResponse) {
    if (confirm(`Delete "${q.title}"? This cannot be undone.`)) {
      this.state.deleteQuestion(q.id);
    }
  }

  private buildLanguages(): ProgrammingLanguage[] {
    const languages: ProgrammingLanguage[] = [];
    if (this.langJava()) languages.push(ProgrammingLanguage.JAVA);
    if (this.langPy()) languages.push(ProgrammingLanguage.PYTHON);
    if (this.langJS()) languages.push(ProgrammingLanguage.JAVASCRIPT);
    if (this.langCpp()) languages.push(ProgrammingLanguage.CPP);
    if (this.langSQL()) languages.push(ProgrammingLanguage.SQL);
    return languages.length ? languages : [ProgrammingLanguage.JAVA];
  }

  save() {
    if (!this.title().trim()) { this.state.showToast('Please enter a title'); return; }
    if (!this.description().trim()) { this.state.showToast('Please enter a problem statement'); return; }
    if (this.testCases().some((tc) => !tc.expectedOutput.trim())) { this.state.showToast('Every test case needs an expected output'); return; }

    const payload: CreateQuestionRequest = {
      title: this.title().trim(),
      difficulty: this.difficulty() as Difficulty,
      marks: this.marks(),
      description: this.description().trim(),
      exampleText: this.exampleText() || undefined,
      constraintsText: this.constraintsText() || undefined,
      inputFormat: this.inputFormat() || undefined,
      outputFormat: this.outputFormat() || undefined,
      explanation: this.explanation() || undefined,
      tags: this.tagsCsv().split(',').map((t) => t.trim()).filter(Boolean),
      topics: this.topicsCsv().split(',').map((t) => t.trim()).filter(Boolean),
      hints: this.hints().map((h) => h.trim()).filter(Boolean),
      timeLimitMs: this.timeLimitMs(),
      memoryLimitMb: this.memoryLimitMb(),
      allowedLanguages: this.buildLanguages(),
      starterCodes: this.loadedStarterCodes.length ? this.loadedStarterCodes : undefined,
      testCases: this.testCases(),
      activateNow: this.activateNow(),
    };

    const editingId = this.editingId();
    const request$ = editingId ? this.state.updateQuestion(editingId, payload) : this.state.createQuestion(payload);
    request$.subscribe({
      next: () => this.backToList(),
      error: (err) => this.state.showToast(err?.error?.message ?? 'Could not save this question.'),
    });
  }
}
