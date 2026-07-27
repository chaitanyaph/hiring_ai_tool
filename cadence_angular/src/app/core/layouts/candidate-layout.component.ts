import { Component, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet } from '@angular/router';
import { Observable } from 'rxjs';
import { AppStateService } from '../services/app-state.service';
import { SidebarComponent } from '../../shared/components/sidebar.component';
import { TopbarComponent } from '../../shared/components/topbar.component';

const SKILL_OPTIONS = ['Java', 'Spring Boot', 'Kafka', 'Redis', 'MySQL', 'Kubernetes', 'AWS'];
const LANGUAGE_OPTIONS = ['English (Professional)', 'Hindi (Native)', 'Marathi (Conversational)'];

@Component({
  selector: 'app-candidate-layout',
  standalone: true,
  imports: [CommonModule, RouterOutlet, SidebarComponent, TopbarComponent],
  template: `
    <div class="layout-wrapper">
      <!-- Sidebar -->
      <app-sidebar [isCandidate]="true"></app-sidebar>

      <!-- Main Panel -->
      <div class="main-panel">
        <app-topbar [isCandidate]="true"></app-topbar>
        
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

      <!-- ================= MODAL: CANDIDATE PROFILE WIZARD (10 STEPS) ================= -->
      <div class="modal-overlay" [class.show]="state.activeModal() === 'profile-wizard'" (click)="state.closeModal()">
        <div class="modal wide" (click)="$event.stopPropagation()">
          <div class="modal-head">
            <div>
              <h3>Complete your profile</h3>
              <p>Step {{ wizardStep() }} of 10</p>
            </div>
            <button class="modal-close" (click)="state.closeModal()" aria-label="Close dialog">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>

          <div class="modal-body">
            <!-- Steps Progress list -->
            <div class="wizard-steps cwiz-steps">
              <div class="wstep" [class.active]="wizardStep() === 1" [class.done]="wizardStep() > 1">1 · Info</div>
              <div class="wstep" [class.active]="wizardStep() === 2" [class.done]="wizardStep() > 2">2 · Resume</div>
              <div class="wstep" [class.active]="wizardStep() === 3" [class.done]="wizardStep() > 3">3 · Edu</div>
              <div class="wstep" [class.active]="wizardStep() === 4" [class.done]="wizardStep() > 4">4 · Exp</div>
              <div class="wstep" [class.active]="wizardStep() === 5" [class.done]="wizardStep() > 5">5 · Skills</div>
              <div class="wstep" [class.active]="wizardStep() === 6" [class.done]="wizardStep() > 6">6 · Proj</div>
              <div class="wstep" [class.active]="wizardStep() === 7" [class.done]="wizardStep() > 7">7 · Certs</div>
              <div class="wstep" [class.active]="wizardStep() === 8" [class.done]="wizardStep() > 8">8 · Lang</div>
              <div class="wstep" [class.active]="wizardStep() === 9" [class.done]="wizardStep() > 9">9 · Prefs</div>
              <div class="wstep" [class.active]="wizardStep() === 10" [class.done]="wizardStep() > 10">10 · Port</div>
            </div>

            <!-- STEP 1: BASIC INFO -->
            <div class="wizard-panel" [class.active]="wizardStep() === 1">
              <div class="form-row-2">
                <div class="field-full"><label>Full name</label><input type="text" #fullNameInput [value]="profile()?.fullName ?? ''"></div>
                <div class="field-full"><label>Headline</label><input type="text" #headlineInput placeholder="e.g. Backend Engineer" [value]="profile()?.headline ?? ''"></div>
              </div>
              <div class="form-row-2">
                <div class="field-full"><label>Email</label><input type="email" [value]="profile()?.email ?? state.currentUser()?.email ?? ''" disabled></div>
                <div class="field-full"><label>Phone</label><input type="text" #phoneInput [value]="profile()?.phone ?? ''"></div>
              </div>
              <div class="field-full"><label>Current location</label><input type="text" #locationInput [value]="profile()?.location ?? ''"></div>
            </div>

            <!-- STEP 2: RESUME -->
            <div class="wizard-panel" [class.active]="wizardStep() === 2">
              <input #resumeFileInput type="file" accept="application/pdf" hidden (change)="onResumeFileSelected($event)">
              <div class="modal-dropzone" (click)="resumeFileInput.click()">
                <svg viewBox="0 0 24 24"><path d="M12 16V4M12 4l-4 4M12 4l4 4"/><path d="M4 16v3a2 2 0 002 2h12a2 2 0 002-2v-3"/></svg>
                <b>{{ selectedResumeFile()?.name ?? profile()?.resumeFilename ?? 'Choose a PDF resume' }}</b>
                <span>{{ profile()?.resumeUrl ? 'Uploaded — click to replace' : 'PDF only, click to upload' }}</span>
              </div>
              <div class="field-hint">Uploading a new resume re-runs AI parsing and may update the fields below.</div>
            </div>

            <!-- STEP 3: EDUCATION -->
            <div class="wizard-panel" [class.active]="wizardStep() === 3">
              <div class="form-row-2">
                <div class="field-full"><label>Degree</label><input type="text" #degreeInput [value]="profile()?.education?.[0]?.degree ?? ''"></div>
                <div class="field-full"><label>Institution</label><input type="text" #institutionInput [value]="profile()?.education?.[0]?.institution ?? ''"></div>
              </div>
              <div class="form-row-2">
                <div class="field-full"><label>Passing year</label><input type="number" #passingYearInput [value]="profile()?.education?.[0]?.endYear ?? ''"></div>
                <div class="field-full"><label>Grade / CGPA</label><input type="text" #gradeInput [value]="profile()?.education?.[0]?.grade ?? ''"></div>
              </div>
            </div>

            <!-- STEP 4: EXPERIENCE -->
            <div class="wizard-panel" [class.active]="wizardStep() === 4">
              <div class="form-row-2">
                <div class="field-full"><label>Job Title</label><input type="text" #jobTitleInput [value]="profile()?.experience?.[0]?.jobTitle ?? ''"></div>
                <div class="field-full"><label>Company name</label><input type="text" #expCompanyInput [value]="profile()?.experience?.[0]?.companyName ?? ''"></div>
              </div>
              <div class="form-row-2">
                <div class="field-full"><label>Start date</label><input type="date" #startDateInput [value]="profile()?.experience?.[0]?.startDate ?? ''"></div>
                <div class="field-full"><label>End date</label><input type="text" value="Present" disabled></div>
              </div>
              <div class="field-full"><label>Description</label><textarea #achievementsInput>{{ profile()?.experience?.[0]?.achievements ?? '' }}</textarea></div>
            </div>

            <!-- STEP 5: SKILLS -->
            <div class="wizard-panel" [class.active]="wizardStep() === 5">
              <div class="field-full">
                <label>Key skills</label>
                <div class="chip-group">
                  <span class="select-chip" *ngFor="let skill of skillOptions" [class.on]="selectedSkills().has(skill)" (click)="toggleSkill(skill)">{{ skill }}</span>
                </div>
              </div>
            </div>

            <!-- STEP 6: PROJECTS -->
            <div class="wizard-panel" [class.active]="wizardStep() === 6">
              <div class="field-full"><label>Project title</label><input type="text" #projectTitleInput [value]="profile()?.projects?.[0]?.title ?? ''"></div>
              <div class="field-full"><label>Details</label><textarea #projectDescInput>{{ profile()?.projects?.[0]?.description ?? '' }}</textarea></div>
            </div>

            <!-- STEP 7: CERTIFICATIONS -->
            <div class="wizard-panel" [class.active]="wizardStep() === 7">
              <div class="field-full"><label>Certificate name</label><input type="text" #certNameInput [value]="profile()?.certifications?.[0]?.name ?? ''"></div>
            </div>

            <!-- STEP 8: LANGUAGES -->
            <div class="wizard-panel" [class.active]="wizardStep() === 8">
              <div class="field-full">
                <label>Languages spoken</label>
                <div class="chip-group">
                  <span class="select-chip" *ngFor="let lang of languageOptions" [class.on]="selectedLanguages().has(lang)" (click)="toggleLanguage(lang)">{{ lang }}</span>
                </div>
              </div>
            </div>

            <!-- STEP 9: PREFERENCES -->
            <div class="wizard-panel" [class.active]="wizardStep() === 9">
              <div class="form-row-2">
                <div class="field-full">
                  <label>Preferred type</label>
                  <select #prefTypeSelect>
                    <option [selected]="profile()?.jobPreferences?.preferredEmploymentType !== 'Contract'">Full-time</option>
                    <option [selected]="profile()?.jobPreferences?.preferredEmploymentType === 'Contract'">Contract</option>
                  </select>
                </div>
                <div class="field-full">
                  <label>Workplace type</label>
                  <select #workTypeSelect>
                    <option [selected]="(profile()?.jobPreferences?.preferredWorkType ?? 'Hybrid') === 'Hybrid'">Hybrid</option>
                    <option [selected]="profile()?.jobPreferences?.preferredWorkType === 'Remote'">Remote</option>
                    <option [selected]="profile()?.jobPreferences?.preferredWorkType === 'On-site'">On-site</option>
                  </select>
                </div>
              </div>
            </div>

            <!-- STEP 10: PORTFOLIO -->
            <div class="wizard-panel" [class.active]="wizardStep() === 10">
              <div class="form-row-2">
                <div class="field-full"><label>GitHub link</label><input type="text" #githubInput [value]="profile()?.portfolio?.githubUrl ?? ''"></div>
                <div class="field-full"><label>LinkedIn link</label><input type="text" #linkedinInput [value]="profile()?.portfolio?.linkedinUrl ?? ''"></div>
              </div>
            </div>
          </div>

          <div class="modal-foot">
            <button class="btn-text" [style.visibility]="wizardStep() > 1 ? 'visible' : 'hidden'" (click)="wizardBack()">Back</button>
            <div class="foot-right">
              <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
              <button class="btn-primary-sm" [disabled]="wizardSaving()" (click)="wizardNext(
                fullNameInput, headlineInput, phoneInput, locationInput,
                degreeInput, institutionInput, passingYearInput, gradeInput,
                jobTitleInput, expCompanyInput, startDateInput, achievementsInput,
                projectTitleInput, projectDescInput, certNameInput,
                prefTypeSelect, workTypeSelect, githubInput, linkedinInput
              )">
                {{ wizardSaving() ? 'Saving…' : (wizardStep() === 10 ? 'Save & Submit' : 'Continue') }}
              </button>
            </div>
          </div>
        </div>
      </div>

    </div>
  `,
  styles: []
})
export class CandidateLayoutComponent {
  wizardStep = signal<number>(1);
  wizardSaving = signal<boolean>(false);

  skillOptions = SKILL_OPTIONS;
  languageOptions = LANGUAGE_OPTIONS;
  selectedSkills = signal<Set<string>>(new Set(['Java', 'Spring Boot', 'Kafka', 'Redis', 'MySQL']));
  selectedLanguages = signal<Set<string>>(new Set(['English (Professional)', 'Hindi (Native)']));
  selectedResumeFile = signal<File | null>(null);

  profile = computed(() => this.state.candidateProfile());

  constructor(public state: AppStateService) {
    this.state.loadCandidateProfile();
  }

  toggleSkill(skill: string) {
    this.selectedSkills.update((set) => {
      const next = new Set(set);
      next.has(skill) ? next.delete(skill) : next.add(skill);
      return next;
    });
  }

  toggleLanguage(lang: string) {
    this.selectedLanguages.update((set) => {
      const next = new Set(set);
      next.has(lang) ? next.delete(lang) : next.add(lang);
      return next;
    });
  }

  onResumeFileSelected(event: Event) {
    const file = (event.target as HTMLInputElement).files?.[0];
    this.selectedResumeFile.set(file ?? null);
  }

  private runStep(observable: Observable<unknown>) {
    this.wizardSaving.set(true);
    observable.subscribe({
      next: () => {
        this.wizardSaving.set(false);
        if (this.wizardStep() < 10) {
          this.wizardStep.update((s) => s + 1);
        }
      },
      error: (err) => {
        this.wizardSaving.set(false);
        this.state.showToast(err?.error?.message ?? 'Could not save this step. Please try again.');
      },
    });
  }

  wizardNext(
    fullNameInput: HTMLInputElement, headlineInput: HTMLInputElement, phoneInput: HTMLInputElement, locationInput: HTMLInputElement,
    degreeInput: HTMLInputElement, institutionInput: HTMLInputElement, passingYearInput: HTMLInputElement, gradeInput: HTMLInputElement,
    jobTitleInput: HTMLInputElement, expCompanyInput: HTMLInputElement, startDateInput: HTMLInputElement, achievementsInput: HTMLTextAreaElement,
    projectTitleInput: HTMLInputElement, projectDescInput: HTMLTextAreaElement, certNameInput: HTMLInputElement,
    prefTypeSelect: HTMLSelectElement, workTypeSelect: HTMLSelectElement, githubInput: HTMLInputElement, linkedinInput: HTMLInputElement
  ) {
    switch (this.wizardStep()) {
      case 1:
        return this.runStep(
          this.state.saveCandidateBasicInfo({
            fullName: fullNameInput.value,
            headline: headlineInput.value || undefined,
            phone: phoneInput.value || undefined,
            location: locationInput.value || undefined,
          })
        );
      case 2: {
        const file = this.selectedResumeFile();
        if (!file) {
          // No new file chosen -- resume upload isn't mandatory to proceed if one already exists.
          this.wizardStep.update((s) => s + 1);
          return;
        }
        return this.runStep(this.state.uploadCandidateResume(file));
      }
      case 3: {
        const existingId = this.profile()?.education?.[0]?.id;
        return this.runStep(
          this.state.saveCandidateEducation([
            {
              id: existingId,
              degree: degreeInput.value,
              institution: institutionInput.value,
              endYear: passingYearInput.value ? Number(passingYearInput.value) : undefined,
              grade: gradeInput.value || undefined,
            },
          ])
        );
      }
      case 4: {
        const existingId = this.profile()?.experience?.[0]?.id;
        return this.runStep(
          this.state.saveCandidateExperience([
            {
              id: existingId,
              jobTitle: jobTitleInput.value,
              companyName: expCompanyInput.value,
              startDate: startDateInput.value || undefined,
              currentlyWorking: true,
              achievements: achievementsInput.value || undefined,
            },
          ])
        );
      }
      case 5:
        return this.runStep(this.state.saveCandidateSkills(Array.from(this.selectedSkills())));
      case 6: {
        const existingId = this.profile()?.projects?.[0]?.id;
        return this.runStep(
          this.state.saveCandidateProjects([
            { id: existingId, title: projectTitleInput.value, description: projectDescInput.value || undefined },
          ])
        );
      }
      case 7: {
        const existingId = this.profile()?.certifications?.[0]?.id;
        return this.runStep(
          this.state.saveCandidateCertifications([{ id: existingId, name: certNameInput.value }])
        );
      }
      case 8:
        return this.runStep(this.state.saveCandidateLanguages(Array.from(this.selectedLanguages())));
      case 9:
        return this.runStep(
          this.state.saveCandidateJobPreferences({
            preferredEmploymentType: prefTypeSelect.value,
            preferredWorkType: workTypeSelect.value,
          })
        );
      case 10:
        return this.runStep(
          this.state.saveCandidatePortfolio({
            githubUrl: githubInput.value || undefined,
            linkedinUrl: linkedinInput.value || undefined,
          })
        );
    }
  }

  wizardBack() {
    if (this.wizardStep() > 1) {
      this.wizardStep.update(s => s - 1);
    }
  }
}
