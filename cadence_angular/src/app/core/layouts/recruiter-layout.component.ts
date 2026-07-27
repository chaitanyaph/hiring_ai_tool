import { Component, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { AppStateService } from '../services/app-state.service';
import { SidebarComponent } from '../../shared/components/sidebar.component';
import { TopbarComponent } from '../../shared/components/topbar.component';
import { TeamRole } from '../models/company.model';
import { AssessmentType, Difficulty, ProgrammingLanguage } from '../models/coding-assessment.model';
import { RoundType } from '../models/interview-management.model';

@Component({
  selector: 'app-recruiter-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent],
  template: `
    <div class="layout-wrapper">
      <!-- Sidebar -->
      <app-sidebar [isCandidate]="false"></app-sidebar>

      <!-- Main Panel -->
      <div class="main-panel">
        <app-topbar 
          [isCandidate]="false"
          (onImportResumes)="state.openModal('candidate')"
          (onNewJob)="openJobWizard()"
        ></app-topbar>
        
        <!-- Main Scrollable Content area -->
        <div class="content-viewport">
          <router-outlet></router-outlet>
        </div>
      </div>

      <!-- Toast Message overlay -->
      <div class="toast-overlay" *ngIf="state.toastMessage()">
        <svg viewBox="0 0 24 24" width="16" height="16" style="stroke:#8FD9C4; fill:none; stroke-width:2.4; margin-right:6px;"><path d="M22 11.08V12a10 10 0 11-5.93-9.14M22 4L12 14.01 9 11.01"/></svg>
        {{ state.toastMessage() }}
      </div>

      <!-- ================= MODAL: JOB WIZARD ================= -->
      <div class="modal-overlay" [class.show]="state.activeModal() === 'job'" (click)="state.closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3 id="job-modal-title">{{ isEditMode() ? 'Edit job - Backend Engineer' : 'Create a new job' }}</h3>
              <p>Define role requirements, budget, and coding assessments.</p>
            </div>
            <button class="modal-close" (click)="state.closeModal()" aria-label="Close dialog">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>

          <div class="modal-body">
            <!-- Wizard Steps Header -->
            <div class="wizard-steps">
              <div class="wstep" [class.active]="wizardStep() === 1" [class.done]="wizardStep() > 1">1. Basic Info</div>
              <div class="wstep" [class.active]="wizardStep() === 2" [class.done]="wizardStep() > 2">2. Details</div>
              <div class="wstep" [class.active]="wizardStep() === 3" [class.done]="wizardStep() > 3">3. Stages</div>
              <div class="wstep" [class.active]="wizardStep() === 4" [class.done]="wizardStep() > 4">4. Review</div>
            </div>

            <!-- Panel 1: Basic Info -->
            <div class="wizard-panel" [class.active]="wizardStep() === 1">
              <div class="field-full">
                <label>Job title</label>
                <input type="text" placeholder="e.g. Backend Engineer" [value]="jobTitle()" (input)="jobTitle.set($any($event.target).value)">
              </div>
              <div class="field-full">
                <label>Department</label>
                <select [value]="jobDept()" (change)="jobDept.set($any($event.target).value)">
                  <option value="Engineering">Engineering</option>
                  <option value="Infrastructure">Infrastructure</option>
                  <option value="People">People</option>
                  <option value="Design">Design</option>
                </select>
              </div>
              <div class="field-full">
                <label>Role description</label>
                <textarea placeholder="Outline key expectations for this job opening…" [value]="jobDesc()" (input)="jobDesc.set($any($event.target).value)"></textarea>
              </div>
            </div>

            <!-- Panel 2: Details -->
            <div class="wizard-panel" [class.active]="wizardStep() === 2">
              <div class="form-row-2">
                <div class="field-full">
                  <label>Workplace type</label>
                  <select [value]="jobWorkType()" (change)="jobWorkType.set($any($event.target).value)">
                    <option value="Hybrid">Hybrid</option>
                    <option value="Remote">Remote</option>
                    <option value="On-site">On-site</option>
                  </select>
                </div>
                <div class="field-full">
                  <label>Location</label>
                  <input type="text" placeholder="e.g. Bengaluru (Hybrid)" [value]="jobLoc()" (input)="jobLoc.set($any($event.target).value)">
                </div>
              </div>
              <div class="form-row-2">
                <div class="field-full">
                  <label>Employment type</label>
                  <select [value]="jobEmpType()" (change)="jobEmpType.set($any($event.target).value)">
                    <option value="Full-time">Full-time</option>
                    <option value="Part-time">Part-time</option>
                    <option value="Contract">Contract</option>
                  </select>
                </div>
                <div class="field-full">
                  <label>Openings</label>
                  <input type="number" [value]="jobOpenings()" (input)="jobOpenings.set($any($event.target).valueAsNumber || 1)">
                </div>
              </div>
              <div class="form-row-3">
                <div class="field-full">
                  <label>Experience level</label>
                  <select [value]="jobExp()" (change)="jobExp.set($any($event.target).value)">
                    <option value="0-2 years">0-2 years</option>
                    <option value="3-6 years">3-6 years</option>
                    <option value="5-8 years">5-8 years</option>
                    <option value="5+ years">5+ years</option>
                  </select>
                </div>
                <div class="field-full">
                  <label>Salary min (₹ LPA)</label>
                  <input type="number" [value]="jobSalMin()" (input)="jobSalMin.set($any($event.target).valueAsNumber || 12)">
                </div>
                <div class="field-full">
                  <label>Salary max (₹ LPA)</label>
                  <input type="number" [value]="jobSalMax()" (input)="jobSalMax.set($any($event.target).valueAsNumber || 28)">
                </div>
              </div>
              <div class="field-full">
                <label>Max. notice period</label>
                <select [value]="jobNotice()" (change)="jobNotice.set($any($event.target).value)">
                  <option value="30 days">30 days</option>
                  <option value="60 days">60 days</option>
                  <option value="90 days">90 days</option>
                  <option value="Any">Any</option>
                </select>
              </div>
            </div>

            <!-- Panel 3: Stages -->
            <div class="wizard-panel" [class.active]="wizardStep() === 3">
              <div class="stage-toggle-row">
                <div>
                  <div class="st-name">AI Resume Screening</div>
                  <div class="st-sub">Auto-score and categorize every applicant</div>
                </div>
                <div class="switch" [class.on]="stageScreening()" (click)="stageScreening.set(!stageScreening())"></div>
              </div>
              <div class="stage-toggle-row">
                <div>
                  <div class="st-name">AI Interview</div>
                  <div class="st-sub">Voice / video / text screening interview</div>
                </div>
                <div class="switch" [class.on]="stageAiInterview()" (click)="stageAiInterview.set(!stageAiInterview())"></div>
              </div>
              <div class="stage-toggle-row">
                <div>
                  <div class="st-name">Coding assessment</div>
                  <div class="st-sub">Online IDE with auto-evaluation</div>
                </div>
                <div class="switch" [class.on]="stageCoding()" (click)="stageCoding.set(!stageCoding())"></div>
              </div>
              <div class="stage-toggle-row">
                <div>
                  <div class="st-name">Technical round</div>
                  <div class="st-sub">Live interview with engineering panel</div>
                </div>
                <div class="switch" [class.on]="stageTech()" (click)="stageTech.set(!stageTech())"></div>
              </div>
              <div class="stage-toggle-row">
                <div>
                  <div class="st-name">HR round</div>
                  <div class="st-sub">Culture fit &amp; offer discussion</div>
                </div>
                <div class="switch" [class.on]="stageHr()" (click)="stageHr.set(!stageHr())"></div>
              </div>
              <div class="field-full" style="margin-top:16px;">
                <label>Application deadline</label>
                <input type="date" [value]="jobDeadline()" (input)="jobDeadline.set($any($event.target).value)">
              </div>
            </div>

            <!-- Panel 4: Review -->
            <div class="wizard-panel" [class.active]="wizardStep() === 4">
              <div class="review-block">
                <div class="review-row"><span>Job title</span><span>{{ jobTitle() }}</span></div>
                <div class="review-row"><span>Department</span><span>{{ jobDept() }}</span></div>
                <div class="review-row"><span>Location</span><span>{{ jobLoc() }} ({{ jobWorkType() }})</span></div>
                <div class="review-row"><span>Openings</span><span>{{ jobOpenings() }}</span></div>
                <div class="review-row"><span>Employment type</span><span>{{ jobEmpType() }}</span></div>
                <div class="review-row"><span>Salary range</span><span>₹{{ jobSalMin() }} - {{ jobSalMax() }} LPA</span></div>
              </div>
              <div class="radio-card-row">
                <div class="radio-card" [class.selected]="publishMode() === 'draft'" (click)="publishMode.set('draft')">
                  <div class="rc-title">Save as draft</div>
                  <div class="rc-sub">Keep private, publish later</div>
                </div>
                <div class="radio-card" [class.selected]="publishMode() === 'publish'" (click)="publishMode.set('publish')">
                  <div class="rc-title">Publish now</div>
                  <div class="rc-sub">Generate public job link</div>
                </div>
              </div>
            </div>
          </div>

          <div class="modal-foot">
            <button class="btn-text" [style.visibility]="wizardStep() > 1 ? 'visible' : 'hidden'" (click)="wizardBack()">Back</button>
            <div class="foot-right">
              <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
              <button class="btn-primary-sm" (click)="wizardNext()">
                {{ wizardStep() === 4 ? (publishMode() === 'publish' ? 'Publish job' : 'Save draft') : 'Continue' }}
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- ================= MODAL: SCHEDULE INTERVIEW ================= -->
      <div class="modal-overlay" [class.show]="state.activeModal() === 'interview'" (click)="state.closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3>Schedule an interview</h3>
              <p>Sends a calendar invite and meeting link automatically</p>
            </div>
            <button class="modal-close" (click)="state.closeModal()" aria-label="Close dialog">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="field-full">
              <label>Candidate</label>
              <select #intCand>
                <option *ngFor="let app of state.companyApplications()" [value]="app.id">{{ app.candidateNameSnapshot }} — {{ app.jobTitleSnapshot }}</option>
              </select>
            </div>
            <div class="field-full">
              <label>Interview type</label>
              <select #intType>
                <option value="AI Interview">AI Interview</option>
                <option value="TECHNICAL">Technical round</option>
                <option value="HR">HR round</option>
              </select>
            </div>
            <div class="form-row-3">
              <div class="field-full"><label>Date</label><input type="date" value="2026-07-04" #intDate></div>
              <div class="field-full"><label>Time</label><input type="time" value="15:30" #intTime></div>
              <div class="field-full">
                <label>Duration</label>
                <select #intDur>
                  <option>30 min</option>
                  <option>45 min</option>
                  <option selected>60 min</option>
                </select>
              </div>
            </div>
            <div class="field-full">
              <label>Interview panel</label>
              <div class="chip-group">
                <span class="select-chip" [class.on]="panelAR()" (click)="panelAR.set(!panelAR())">Ananya Rao</span>
                <span class="select-chip" [class.on]="panelVS()" (click)="panelVS.set(!panelVS())">Vikram Shah</span>
                <span class="select-chip" [class.on]="panelMN()" (click)="panelMN.set(!panelMN())">Meera Nair</span>
                <span class="select-chip" [class.on]="panelFS()" (click)="panelFS.set(!panelFS())">Farhan Sheikh</span>
              </div>
            </div>
            <div class="stage-toggle-row">
              <div><div class="st-name">Auto-generate Google Meet link</div></div>
              <div class="switch" [class.on]="meetLink()" (click)="meetLink.set(!meetLink())"></div>
            </div>
            <div class="stage-toggle-row">
              <div><div class="st-name">Notify candidate by email</div></div>
              <div class="switch" [class.on]="emailNotify()" (click)="emailNotify.set(!emailNotify())"></div>
            </div>
          </div>
          <div class="modal-foot">
            <span></span>
            <div class="foot-right">
              <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
              <button class="btn-primary-sm" (click)="submitInterview(intCand.value, intType.value, intDate.value, intTime.value, intDur.value)">Schedule interview</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ================= MODAL: ADD / IMPORT CANDIDATE ================= -->
      <div class="modal-overlay" [class.show]="state.activeModal() === 'candidate'" (click)="state.closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3>Add candidates</h3>
              <p>Manually, by resume, or in bulk</p>
            </div>
            <button class="modal-close" (click)="state.closeModal()" aria-label="Close dialog">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="modal-tabs">
              <button [class.active]="candidateTab() === 'manual'" (click)="candidateTab.set('manual')">Manual entry</button>
              <button [class.active]="candidateTab() === 'upload'" (click)="candidateTab.set('upload')">Resume upload</button>
              <button [class.active]="candidateTab() === 'bulk'" (click)="candidateTab.set('bulk')">Bulk import</button>
            </div>

            <div class="modal-tab-pane" [class.active]="candidateTab() === 'manual'">
              <div class="form-row-2">
                <div class="field-full"><label>Full name</label><input placeholder="Candidate name" #manName></div>
                <div class="field-full"><label>Email</label><input placeholder="candidate@email.com" #manEmail></div>
              </div>
              <div class="form-row-2">
                <div class="field-full"><label>Phone</label><input placeholder="+91" #manPhone></div>
                <div class="field-full">
                  <label>Applying for</label>
                  <select #manJob>
                    <option>Backend Engineer</option>
                    <option>Senior DevOps Engineer</option>
                    <option>Frontend Engineer</option>
                  </select>
                </div>
              </div>
              <div class="modal-dropzone" (click)="state.showToast('Attaching file…')">
                <svg viewBox="0 0 24 24"><path d="M12 16V4M12 4l-4 4M12 4l4 4"/><path d="M4 16v3a2 2 0 002 2h12a2 2 0 002-2v-3"/></svg>
                <b>Attach resume (optional)</b>
                <span>PDF, DOCX up to 10MB</span>
              </div>
            </div>

            <div class="modal-tab-pane" [class.active]="candidateTab() === 'upload'">
              <div class="field-full">
                <label>Applying for</label>
                <select>
                  <option>Backend Engineer</option>
                  <option>Senior DevOps Engineer</option>
                  <option>Frontend Engineer</option>
                </select>
              </div>
              <div class="modal-dropzone" (click)="state.showToast('Selecting resumes…')">
                <svg viewBox="0 0 24 24"><path d="M12 16V4M12 4l-4 4M12 4l4 4"/><path d="M4 16v3a2 2 0 002 2h12a2 2 0 002-2v-3"/></svg>
                <b>Drop resumes here, or browse</b>
                <span>Supports multiple PDF / DOCX files</span>
              </div>
              <div class="field-hint">The Resume Parser will auto-extract skills, experience and education, then AI Matching will score each candidate against this job.</div>
            </div>

            <div class="modal-tab-pane" [class.active]="candidateTab() === 'bulk'">
              <div class="modal-dropzone" (click)="state.showToast('Selecting CSV file…')">
                <svg viewBox="0 0 24 24"><path d="M12 16V4M12 4l-4 4M12 4l4 4"/><path d="M4 16v3a2 2 0 002 2h12a2 2 0 002-2v-3"/></svg>
                <b>Upload a CSV file</b>
                <span>Name, email, phone, resume link columns</span>
              </div>
              <a class="card-link" style="font-size:12.5px; cursor:pointer;" (click)="state.showToast('Downloading sample template…')">Download sample template</a>
            </div>
          </div>
          <div class="modal-foot">
            <span></span>
            <div class="foot-right">
              <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
              <button class="btn-primary-sm" (click)="submitCandidate(manName, manEmail, manPhone, manJob)">Add candidate</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ================= MODAL: NEW ASSESSMENT ================= -->
      <div class="modal-overlay" [class.show]="state.activeModal() === 'assessment'" (click)="state.closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3>Create assessment</h3>
            </div>
            <button class="modal-close" (click)="state.closeModal()" aria-label="Close dialog">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="modal-tabs">
              <button [class.active]="assessTab() === 'coding'" (click)="assessTab.set('coding')">Coding assessment</button>
              <button [class.active]="assessTab() === 'mcq'" (click)="assessTab.set('mcq')">MCQ assessment</button>
            </div>

            <div class="modal-tab-pane" [class.active]="assessTab() === 'coding'">
              <div class="field-full">
                <label>Assessment name</label>
                <input placeholder="e.g. Java Backend Challenge" #cName>
              </div>
              <div class="field-full">
                <label>Languages</label>
                <div class="chip-group">
                  <span class="select-chip" [class.on]="langJava()" (click)="langJava.set(!langJava())">Java</span>
                  <span class="select-chip" [class.on]="langPy()" (click)="langPy.set(!langPy())">Python</span>
                  <span class="select-chip" [class.on]="langJS()" (click)="langJS.set(!langJS())">JavaScript</span>
                  <span class="select-chip" [class.on]="langCpp()" (click)="langCpp.set(!langCpp())">C++</span>
                  <span class="select-chip" [class.on]="langSQL()" (click)="langSQL.set(!langSQL())">SQL</span>
                </div>
              </div>
              <div class="form-row-2">
                <div class="field-full">
                  <label>Difficulty</label>
                  <select #cDiff>
                    <option value="EASY">Easy</option>
                    <option value="MEDIUM" selected>Medium</option>
                    <option value="HARD">Hard</option>
                  </select>
                </div>
                <div class="field-full"><label>Time limit (min)</label><input type="number" value="60" #cTime></div>
              </div>
              <div class="stage-toggle-row">
                <div><div class="st-name">Anti-cheat monitoring</div></div>
                <div class="switch" [class.on]="cAntiCheat()" (click)="cAntiCheat.set(!cAntiCheat())"></div>
              </div>
              <div class="stage-toggle-row">
                <div><div class="st-name">Plagiarism detection</div></div>
                <div class="switch" [class.on]="cPlagiarism()" (click)="cPlagiarism.set(!cPlagiarism())"></div>
              </div>
            </div>

            <div class="modal-tab-pane" [class.active]="assessTab() === 'mcq'">
              <div class="field-full">
                <label>Assessment name</label>
                <input placeholder="e.g. Java &amp; Spring Fundamentals" #mName>
              </div>
              <div class="form-row-2">
                <div class="field-full">
                  <label>Category</label>
                  <select #mCat>
                    <option>Java &amp; Spring</option>
                    <option>SQL</option>
                    <option>Aptitude</option>
                    <option>Logical reasoning</option>
                    <option>English</option>
                  </select>
                </div>
                <div class="field-full"><label>Number of questions</label><input type="number" value="30" #mCount></div>
              </div>
              <div class="field-full"><label>Time limit (min)</label><input type="number" value="20" #mTime></div>
              <div class="stage-toggle-row">
                <div><div class="st-name">Randomize question order</div></div>
                <div class="switch on"></div>
              </div>
            </div>
          </div>
          <div class="modal-foot">
            <span></span>
            <div class="foot-right">
              <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
              <button class="btn-primary-sm" (click)="submitAssessment(cName, cDiff, cTime, mName, mCat, mCount, mTime)">Create assessment</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ================= MODAL: INVITE TEAMMATE ================= -->
      <div class="modal-overlay" [class.show]="state.activeModal() === 'invite'" (click)="state.closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3>Invite a teammate</h3>
              <p>They'll skip registration and land straight in the workspace</p>
            </div>
            <button class="modal-close" (click)="state.closeModal()" aria-label="Close dialog">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="field-full">
              <label>Email address</label>
              <input placeholder="teammate@acmecorp.com" #invEmail>
            </div>
            <a class="card-link" style="display:inline-block; margin-bottom:16px; font-size:12.5px; cursor:pointer;" (click)="state.showToast('Added another email field')">+ Add another email</a>
            <div class="form-row-2">
              <div class="field-full">
                <label>Role</label>
                <select #invRole>
                  <option>Recruiter</option>
                  <option>Hiring Manager</option>
                  <option>Interviewer</option>
                </select>
              </div>
              <div class="field-full">
                <label>Department</label>
                <select #invDept>
                  <option>Engineering</option>
                  <option>Infrastructure</option>
                  <option>People</option>
                  <option>Design</option>
                </select>
              </div>
            </div>
            <div class="field-full">
              <label>Personal message (optional)</label>
              <textarea placeholder="Add a short note to include in the invite email…"></textarea>
            </div>
          </div>
          <div class="modal-foot">
            <span></span>
            <div class="foot-right">
              <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
              <button class="btn-primary-sm" (click)="submitInvite(invEmail.value, invRole.value, invDept.value)">Send invite</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ================= MODAL: ADD DEPARTMENT ================= -->
      <div class="modal-overlay" [class.show]="state.activeModal() === 'department'" (click)="state.closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3>Add a department</h3>
              <p>Used when creating jobs and inviting teammates</p>
            </div>
            <button class="modal-close" (click)="state.closeModal()" aria-label="Close dialog">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="field-full">
              <label>Department name</label>
              <input placeholder="e.g. Customer Success" #deptName>
            </div>
          </div>
          <div class="modal-foot">
            <span></span>
            <div class="foot-right">
              <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
              <button class="btn-primary-sm" (click)="submitDept(deptName.value)">Add department</button>
            </div>
          </div>
        </div>
      </div>

      <!-- ================= MODAL: ADD OFFICE ================= -->
      <div class="modal-overlay" [class.show]="state.activeModal() === 'office'" (click)="state.closeModal()">
        <div class="modal" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3>Add an office</h3>
              <p>Shown on job postings with a remote/hybrid/on-site location</p>
            </div>
            <button class="modal-close" (click)="state.closeModal()" aria-label="Close dialog">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="field-full">
              <label>Office name</label>
              <input placeholder="e.g. Hyderabad office" #offName>
            </div>
            <div class="field-full">
              <label>Address</label>
              <input placeholder="e.g. Gachibowli, Hyderabad" #offAddr>
            </div>
          </div>
          <div class="modal-foot">
            <span></span>
            <div class="foot-right">
              <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
              <button class="btn-primary-sm" (click)="submitOffice(offName.value, offAddr.value)">Add office</button>
            </div>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: []
})
export class RecruiterLayoutComponent {
  wizardStep = signal<number>(1);
  isEditMode = signal<boolean>(false);

  // Wizard fields
  jobTitle = signal<string>('');
  jobDept = signal<string>('Engineering');
  jobDesc = signal<string>('');
  jobWorkType = signal<string>('Hybrid');
  jobLoc = signal<string>('');
  jobEmpType = signal<string>('Full-time');
  jobOpenings = signal<number>(1);
  jobExp = signal<string>('3-6 years');
  jobSalMin = signal<number>(12);
  jobSalMax = signal<number>(28);
  jobNotice = signal<string>('30 days');
  jobDeadline = signal<string>('2026-07-20');
  
  stageScreening = signal<boolean>(true);
  stageAiInterview = signal<boolean>(true);
  stageCoding = signal<boolean>(true);
  stageTech = signal<boolean>(true);
  stageHr = signal<boolean>(true);
  
  publishMode = signal<string>('publish');

  // Interview fields
  panelAR = signal<boolean>(true);
  panelVS = signal<boolean>(false);
  panelMN = signal<boolean>(false);
  panelFS = signal<boolean>(false);
  meetLink = signal<boolean>(true);
  emailNotify = signal<boolean>(true);

  // Candidate tabs
  candidateTab = signal<string>('manual');

  // Assessment tabs
  assessTab = signal<string>('coding');
  langJava = signal<boolean>(true);
  langPy = signal<boolean>(false);
  langJS = signal<boolean>(true);
  langCpp = signal<boolean>(false);
  langSQL = signal<boolean>(false);
  cAntiCheat = signal<boolean>(true);
  cPlagiarism = signal<boolean>(true);

  constructor(public state: AppStateService) {
    effect(() => {
      if (this.state.activeModal() === 'job' && !this.isEditMode()) {
        this.resetJobWizard();
      }
    });
    // Prefetch once per session so the invite modal's best-effort department
    // matching (see submitInvite) and the Settings > Company tab both have
    // data ready without every screen needing its own load call.
    this.state.loadCompany();
    this.state.loadDepartments();
    this.state.loadOffices();
    this.state.loadJobs();
  }

  resetJobWizard() {
    this.wizardStep.set(1);
    this.jobTitle.set('');
    this.jobDept.set('Engineering');
    this.jobDesc.set('');
    this.jobWorkType.set('Hybrid');
    this.jobLoc.set('');
    this.jobEmpType.set('Full-time');
    this.jobOpenings.set(1);
    this.jobExp.set('3-6 years');
    this.jobSalMin.set(12);
    this.jobSalMax.set(28);
    this.jobNotice.set('30 days');
    this.jobDeadline.set('2026-07-20');
  }

  openJobWizard() {
    this.isEditMode.set(false);
    this.resetJobWizard();
    this.state.openModal('job');
  }

  wizardNext() {
    if (this.wizardStep() < 4) {
      if (this.wizardStep() === 1 && !this.jobTitle().trim()) {
        this.state.showToast('Please enter a job title');
        return;
      }
      this.wizardStep.update(s => s + 1);
    } else {
      // Done / Publish
      this.state.createJobFromWizard({
        title: this.jobTitle().trim(),
        departmentName: this.jobDept(),
        description: this.jobDesc().trim() || 'Role created via quick wizard.',
        workType: this.jobWorkType(),
        location: this.jobLoc().trim() || 'Pune (Hybrid)',
        empType: this.jobEmpType(),
        openings: this.jobOpenings(),
        experienceRange: this.jobExp(),
        salaryMin: this.jobSalMin(),
        salaryMax: this.jobSalMax(),
        noticePeriod: this.jobNotice(),
        deadline: this.jobDeadline(),
        stages: [
          { name: 'AI Resume Screening', enabled: this.stageScreening() },
          { name: 'AI Interview', enabled: this.stageAiInterview() },
          { name: 'Coding Assessment', enabled: this.stageCoding() },
          { name: 'Technical Interview', enabled: this.stageTech() },
          { name: 'HR Interview', enabled: this.stageHr() },
        ],
        publish: this.publishMode() === 'publish',
      });
    }
  }

  wizardBack() {
    if (this.wizardStep() > 1) {
      this.wizardStep.update(s => s - 1);
    }
  }

  /**
   * "Interview panel" chips (Ananya Rao/Vikram Shah/Meera Nair/Farhan Sheikh) are static
   * illustrative names with no real interviewer directory behind them -- no endpoint in
   * this session lists a company's team members as selectable platform user IDs, so
   * panelistIds defaults to the scheduling recruiter themselves rather than guessing.
   */
  submitInterview(cand: string, type: string, date: string, time: string, duration: string) {
    const app = this.state.companyApplications().find((a) => a.id === cand);
    if (!app) {
      this.state.showToast('Select a candidate to schedule with.');
      return;
    }
    const panelistId = this.state.currentUserId();
    const durationMinutes = parseInt(duration, 10) || 60;

    if (type === 'AI Interview') {
      this.state.startAiInterviewInvite(app.id, app.jobId, app.candidateId);
      this.state.closeModal();
      return;
    }

    this.state.scheduleRecruiterInterview({
      applicationId: app.id,
      jobId: app.jobId,
      candidateId: app.candidateId,
      roundType: type as RoundType,
      scheduledDate: date,
      scheduledTime: time,
      durationMinutes,
      panelistIds: panelistId ? [panelistId] : [],
    });
    this.state.closeModal();
  }

  submitCandidate(nameEl: HTMLInputElement, emailEl: HTMLInputElement, phoneEl: HTMLInputElement, jobEl: HTMLSelectElement) {
    const name = nameEl.value.trim();
    const email = emailEl.value.trim();
    const phone = phoneEl.value.trim();
    const job = jobEl.value;

    if (this.candidateTab() === 'manual') {
      if (!name || !email) {
        this.state.showToast('Name and email are required');
        return;
      }
      this.state.candidates.update(list => [
        ...list,
        {
          id: `cand-${Date.now()}`,
          name,
          email,
          phone: phone || '+91 99999 88888',
          currentCompany: 'Previous Employer',
          matchScore: Math.floor(Math.random() * 25) + 75,
          stage: 'AI Shortlisted',
          sourcedFrom: 'Manual entry',
          appliedDate: 'Just now',
          noticePeriod: '30 days',
          expectedSalary: '₹18 LPA',
          recruiterNote: 'Manually imported profile.'
        }
      ]);
      this.state.showToast('Candidate added successfully');
    } else {
      this.state.showToast('Resumes uploaded successfully');
    }

    // Reset inputs
    nameEl.value = '';
    emailEl.value = '';
    phoneEl.value = '';
    this.state.closeModal();
  }

  /**
   * jobId: no job selector exists in this modal (same gap as the invite/shortlisting
   * modals) -- defaults to the first open job. questionCount/passingScorePercent/
   * totalMarks: CreateAssessmentRequest requires these but the wizard never collects
   * them, so reasonable defaults are used (2 questions matching the candidate exam's
   * own hardcoded 2-question flow, 60% passing, 100 total marks) -- flagged here since
   * they're not real user input, not because it's a fabricated feature.
   */
  submitAssessment(
    cName: HTMLInputElement, cDiff: HTMLSelectElement, cTime: HTMLInputElement,
    mName: HTMLInputElement, mCat: HTMLSelectElement, mCount: HTMLInputElement, mTime: HTMLInputElement
  ) {
    const jobId = this.state.jobs()[0]?.id;
    if (!jobId) {
      this.state.showToast('No job available to attach this assessment to.');
      return;
    }

    if (this.assessTab() === 'coding') {
      const languages: ProgrammingLanguage[] = [];
      if (this.langJava()) languages.push(ProgrammingLanguage.JAVA);
      if (this.langPy()) languages.push(ProgrammingLanguage.PYTHON);
      if (this.langJS()) languages.push(ProgrammingLanguage.JAVASCRIPT);
      if (this.langCpp()) languages.push(ProgrammingLanguage.CPP);
      if (this.langSQL()) languages.push(ProgrammingLanguage.SQL);

      this.state.createCodingAssessment({
        name: cName.value,
        jobId,
        type: AssessmentType.CODING,
        allowedLanguages: languages.length ? languages : [ProgrammingLanguage.JAVA],
        difficulty: cDiff.value as Difficulty,
        durationMinutes: Number(cTime.value) || 60,
        questionCount: 2,
        passingScorePercent: 60,
        totalMarks: 100,
        antiCheatMonitoring: this.cAntiCheat(),
        plagiarismDetection: this.cPlagiarism(),
      }).subscribe({
        next: () => this.state.closeModal(),
        error: (err) => this.state.showToast(err?.error?.message ?? 'Could not create this assessment.'),
      });
    } else {
      this.state.createCodingAssessment({
        name: mName.value,
        jobId,
        type: AssessmentType.MCQ,
        allowedLanguages: [ProgrammingLanguage.JAVA],
        difficulty: Difficulty.MEDIUM,
        durationMinutes: Number(mTime.value) || 20,
        questionCount: Number(mCount.value) || 30,
        passingScorePercent: 60,
        totalMarks: 100,
      }).subscribe({
        next: () => this.state.closeModal(),
        error: (err) => this.state.showToast(err?.error?.message ?? 'Could not create this assessment.'),
      });
    }
  }

  private static readonly ROLE_LABEL_MAP: Record<string, TeamRole> = {
    Recruiter: TeamRole.HR_RECRUITER,
    'Hiring Manager': TeamRole.HIRING_MANAGER,
    Interviewer: TeamRole.INTERVIEWER,
  };

  submitInvite(email: string, roleLabel: string, departmentLabel: string) {
    if (!email.trim()) {
      this.state.showToast('Please enter an email address');
      return;
    }
    const role = RecruiterLayoutComponent.ROLE_LABEL_MAP[roleLabel] ?? TeamRole.HR_RECRUITER;
    // The department dropdown's options are a fixed illustrative list, not the
    // company's actual departments (that would require turning it into an
    // *ngFor over state.departments(), a template-structure change) -- best
    // effort match by name; departmentId is optional on the backend, so this
    // degrades gracefully to "no department" when there's no match.
    const matchedDept = this.state.departments().find((d) => d.departmentName === departmentLabel);
    this.state.inviteTeammate({ email: email.trim(), role, departmentId: matchedDept?.id });
  }

  submitDept(name: string) {
    if (!name.trim()) {
      this.state.showToast('Please enter a department name');
      return;
    }
    this.state.addDepartment({ departmentName: name.trim() });
  }

  submitOffice(name: string, address: string) {
    if (!name.trim()) {
      this.state.showToast('Please enter an office name');
      return;
    }
    this.state.addOffice({ officeName: name.trim(), address: address.trim() || undefined });
  }
}
