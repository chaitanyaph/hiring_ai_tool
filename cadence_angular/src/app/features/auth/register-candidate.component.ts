import { Component, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AppButtonComponent } from '../../shared/components/app-button.component';
import { AuthService } from '../../core/services/auth.service';
import { UserType } from '../../core/models/auth.model';
import { isValidEmail, validatePassword } from '../../core/utils/validators';

@Component({
  selector: 'app-register-candidate',
  standalone: true,
  imports: [CommonModule, RouterLink, AppButtonComponent],
  template: `
    <div class="split-viewport">
      <!-- Left Panel: Brand Details -->
      <div class="brand-panel">
        <div class="brand-header">
          <div class="logo">C</div>
          <h2>Cadence</h2>
        </div>
        <div class="brand-content">
          <h1>Hiring pipelines.</h1>
          <p>Submit your resume, get matched by AI algorithms, and track your application milestones instantly.</p>
        </div>
      </div>

      <!-- Right Panel: Candidate Signup Form -->
      <div class="form-panel">
        <div class="form-container">
          <div class="header-sec">
            <h3>Candidate portal setup</h3>
            <p>Upload your resume to get started.</p>
          </div>

          <form (submit)="onSubmit($event)">
            <!-- Full Name -->
            <div class="input-field">
              <label>Full name</label>
              <input type="text" placeholder="e.g. Rahul Mehta" required #nameInput (input)="clearErrors()">
              <div class="field-error-msg" [class.show]="nameError()">{{ nameError() }}</div>
            </div>

            <!-- Email -->
            <div class="input-field">
              <label>Work or personal email</label>
              <input type="email" placeholder="rahul.mehta@email.com" required #emailInput (input)="clearErrors()">
              <div class="field-error-msg" [class.show]="emailError()">{{ emailError() }}</div>
            </div>

            <!-- Resume upload dropzone -->
            <div class="input-field">
              <label>Resume / CV</label>
              <div class="upload-zone" (click)="triggerUpload()">
                <svg viewBox="0 0 24 24"><path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
                <span>{{ uploadedFileName() ? uploadedFileName() : 'Upload PDF, DOCX or TXT' }}</span>
              </div>
            </div>

            <!-- Password -->
            <div class="input-field">
              <label>Password</label>
              <input type="password" placeholder="••••••••" required #passwordInput (input)="clearErrors()">
              <div style="color:var(--ink-soft); font-size:11px; margin-top:5px;">At least 8 characters, with an uppercase letter, lowercase letter, number and symbol.</div>
              <div class="field-error-msg" [class.show]="passwordError()">{{ passwordError() }}</div>
            </div>

            <!-- Action -->
            <app-button 
              label="Submit resume & Register" 
              type="submit" 
              styleClass="primary" 
              [fullWidth]="true"
              [height]="42"
            ></app-button>
          </form>

          <div class="back-link">
            Already registered? <a routerLink="/login">Log in here</a>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .split-viewport {
      display: flex;
      width: 100vw;
      height: 100vh;
      overflow: hidden;
      background-color: var(--paper);
    }

    .brand-panel {
      flex: 42;
      background: radial-gradient(circle at -80% -100%, var(--indigo) 0%, var(--indigo-deep) 65%);
      color: var(--paper-card);
      padding: 40px;
      display: flex;
      flex-direction: column;
      justify-content: space-between;

      @include respond-to('not-desktop') {
        display: none;
      }
    }

    .brand-header {
      display: flex;
      align-items: center;
      gap: 12px;

      .logo {
        width: 32px;
        height: 32px;
        background-color: var(--gold);
        color: var(--indigo-deep);
        border-radius: var(--radius-medium);
        display: flex;
        align-items: center;
        justify-content: center;
        font-weight: 700;
        font-size: 16px;
      }

      h2 {
        font-family: $font-sans;
        font-size: 16px;
        font-weight: 600;
      }
    }

    .brand-content {
      display: flex;
      flex-direction: column;
      gap: 24px;
      max-width: 440px;
      margin: auto 0;

      h1 {
        font-family: $font-serif;
        font-size: 34px;
        font-weight: 600;
        line-height: 1.25;
      }

      p {
        font-size: 14.5px;
        color: rgba(255, 255, 255, 0.7);
        line-height: 1.6;
      }
    }

    .form-panel {
      flex: 58;
      display: flex;
      align-items: center;
      justify-content: center;
      padding: 40px 24px;
      overflow-y: auto;
      @include custom-scrollbar;

      @include respond-to('not-desktop') {
        flex: 1;
      }
    }

    .form-container {
      width: 100%;
      max-width: 378px;
      display: flex;
      flex-direction: column;
      gap: 20px;
    }

    .header-sec {
      display: flex;
      flex-direction: column;
      gap: 6px;

      h3 {
        font-family: $font-serif;
        font-size: 22px;
        color: var(--indigo);
      }

      p {
        font-size: 13px;
        color: var(--ink-soft);
      }
    }

    .input-field {
      display: flex;
      flex-direction: column;
      gap: 6px;
      margin-bottom: 14px;

      label {
        font-size: 12.5px;
        font-weight: 600;
        color: var(--ink);
      }

      input {
        height: 40px;
        padding: 0 12px;
        border: 1px solid var(--line);
        border-radius: var(--radius-medium);
        font-family: $font-sans;
        font-size: 13px;
        background-color: var(--paper-card);
        color: var(--ink);
        @include transition-base;

        &:focus {
          border-color: var(--indigo);
        }
      }
    }

    .upload-zone {
      border: 2px dashed var(--line);
      border-radius: var(--radius-large);
      padding: 24px;
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      gap: 10px;
      cursor: pointer;
      @include transition-base;

      &:hover {
        border-color: var(--indigo);
        background-color: var(--line-soft);
      }

      svg {
        width: 24px;
        height: 24px;
        color: var(--ink-soft);
      }

      span {
        font-size: 12.5px;
        color: var(--ink-soft);
      }
    }

    .back-link {
      font-size: 12.5px;
      color: var(--ink-soft);
      text-align: center;
      margin-top: 10px;

      a {
        font-weight: 600;
      }
    }
  `]
})
export class RegisterCandidateComponent {
  @ViewChild('nameInput') nameInputRef!: ElementRef<HTMLInputElement>;
  @ViewChild('emailInput') emailInputRef!: ElementRef<HTMLInputElement>;
  @ViewChild('passwordInput') passwordInputRef!: ElementRef<HTMLInputElement>;

  uploadedFileName = signal<string>('');
  isSubmitting = signal<boolean>(false);

  nameError = signal<string | null>(null);
  emailError = signal<string | null>(null);
  passwordError = signal<string | null>(null);

  constructor(public state: AppStateService, private router: Router, private authService: AuthService) {}

  clearErrors() {
    this.nameError.set(null);
    this.emailError.set(null);
    this.passwordError.set(null);
  }

  /** Real resume upload/parsing is Module 5 (Resume) + Module 7 (Resume Parser) scope -- this stays a placeholder until then. */
  triggerUpload() {
    this.uploadedFileName.set('rahul_mehta_resume.pdf');
    this.state.showToast('Resume attached. It will be uploaded after you finish setting up your account.');
  }

  onSubmit(event: Event) {
    event.preventDefault();
    if (this.isSubmitting()) return;
    this.clearErrors();

    const fullName = this.nameInputRef?.nativeElement.value.trim() ?? '';
    const email = this.emailInputRef?.nativeElement.value.trim() ?? '';
    const password = this.passwordInputRef?.nativeElement.value ?? '';

    let valid = true;
    if (!fullName) {
      this.nameError.set('Enter your full name to continue.');
      valid = false;
    }
    if (!email || !isValidEmail(email)) {
      this.emailError.set('Enter a valid email address.');
      valid = false;
    }
    const passwordIssue = validatePassword(password);
    if (passwordIssue) {
      this.passwordError.set(passwordIssue);
      valid = false;
    }
    if (!valid) return;

    this.isSubmitting.set(true);

    this.authService.register({ fullName, email, password, userType: UserType.CANDIDATE }).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.state.showToast('Account created! Please verify your email before logging in — check your inbox for the link.');
        this.router.navigate(['/login'], { queryParams: { tab: 'candidate' } });
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.state.showToast(err?.error?.message ?? 'Could not create your account. Please try again.');
      },
    });
  }
}
