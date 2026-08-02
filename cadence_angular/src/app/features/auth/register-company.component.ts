import { Component, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AppButtonComponent } from '../../shared/components/app-button.component';
import { AuthService } from '../../core/services/auth.service';
import { UserType } from '../../core/models/auth.model';
import { isValidEmail, validatePassword } from '../../core/utils/validators';

@Component({
  selector: 'app-register-company',
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
          <h1>Hiring team platform.</h1>
          <p>Create a dedicated workspace for your hiring managers and start candidate matching in under 5 minutes.</p>
        </div>
      </div>

      <!-- Right Panel: Workspace Form -->
      <div class="form-panel">
        <div class="form-container">
          <div class="header-sec">
            <h3>Create a new workspace</h3>
            <p>Setup a recruiter pipeline for your hiring team.</p>
          </div>

          <form (submit)="onSubmit($event)">
            <!-- Workspace Name -->
            <div class="input-field">
              <label>Company name</label>
              <input 
                type="text" 
                placeholder="e.g. Acme Corp" 
                #companyInput
                (input)="onCompanyInput(companyInput.value)"
              >
            </div>

            <!-- Workspace Slug -->
            <div class="input-field">
              <label>Workspace URL</label>
              <div class="slug-wrapper">
                <span class="prefix">cadence.sh/</span>
                <input 
                  type="text" 
                  [value]="slug()" 
                  (input)="onSlugInput($event)"
                  placeholder="acme-corp"
                >
              </div>
            </div>

            <!-- Admin Email -->
            <div class="input-field">
              <label>Work email</label>
              <input type="email" placeholder="you@company.com" required #emailInput>
            </div>

            <!-- Password with strength checker -->
            <div class="input-field">
              <label>Password</label>
              <input 
                type="password" 
                placeholder="••••••••" 
                #passInput 
                (input)="checkPasswordStrength(passInput.value)"
                required
              >
              <!-- Strength meter bar -->
              <div class="strength-meter">
                <div class="strength-bar" [ngClass]="strengthClass" [style.width.%]="strengthPercent"></div>
              </div>
              <span class="strength-label">Password strength: <strong>{{ strengthLabel }}</strong></span>
            </div>

            <!-- Submit -->
            <app-button 
              label="Create workspace" 
              type="submit" 
              styleClass="primary" 
              [fullWidth]="true"
              [height]="42"
            ></app-button>
          </form>

          <div class="back-link">
            Already have a workspace? <a routerLink="/login">Log in</a>
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

      .slug-wrapper {
        display: flex;
        align-items: center;
        border: 1px solid var(--line);
        border-radius: var(--radius-medium);
        background-color: var(--paper-card);
        overflow: hidden;
        height: 40px;

        .prefix {
          padding: 0 8px 0 12px;
          font-size: 13px;
          color: var(--ink-soft);
          user-select: none;
        }

        input {
          flex: 1;
          border: none;
          padding: 0 12px 0 0;
          height: 100%;

          &:focus {
            box-shadow: none;
          }
        }
      }
    }

    // Password strength bar indicators
    .strength-meter {
      height: 4px;
      width: 100%;
      background-color: var(--line-soft);
      border-radius: 2px;
      overflow: hidden;
      margin-top: 4px;
    }

    .strength-bar {
      height: 100%;
      width: 0%;
      border-radius: 2px;
      @include transition-base;

      &.weak { background-color: var(--danger); }
      &.medium { background-color: var(--gold); }
      &.strong { background-color: var(--teal); }
    }

    .strength-label {
      font-size: 11px;
      color: var(--ink-soft);
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
export class RegisterCompanyComponent {
  @ViewChild('companyInput') companyInputRef!: ElementRef<HTMLInputElement>;
  @ViewChild('emailInput') emailInputRef!: ElementRef<HTMLInputElement>;
  @ViewChild('passInput') passInputRef!: ElementRef<HTMLInputElement>;

  slug = signal<string>('');
  strengthPercent = 0;
  strengthLabel = 'Empty';
  strengthClass = '';
  isSubmitting = signal<boolean>(false);

  constructor(public state: AppStateService, private router: Router, private authService: AuthService) {}

  onCompanyInput(val: string) {
    const clean = val.toLowerCase().replace(/[^a-z0-9]/g, '-').replace(/-+/g, '-').replace(/^-|-$/g, '');
    this.slug.set(clean);
  }

  onSlugInput(event: Event) {
    const input = event.target as HTMLInputElement;
    this.slug.set(input.value);
  }

  checkPasswordStrength(val: string) {
    if (!val) {
      this.strengthPercent = 0;
      this.strengthLabel = 'Empty';
      this.strengthClass = '';
      return;
    }

    let score = 0;
    if (val.length >= 6) score++;
    if (val.length >= 10) score++;
    if (/[A-Z]/.test(val)) score++;
    if (/[0-9]/.test(val)) score++;
    if (/[^A-Za-z0-9]/.test(val)) score++;

    if (score <= 2) {
      this.strengthPercent = 33;
      this.strengthLabel = 'Weak';
      this.strengthClass = 'weak';
    } else if (score <= 4) {
      this.strengthPercent = 66;
      this.strengthLabel = 'Medium';
      this.strengthClass = 'medium';
    } else {
      this.strengthPercent = 100;
      this.strengthLabel = 'Strong';
      this.strengthClass = 'strong';
    }
  }

  onSubmit(event: Event) {
    event.preventDefault();
    if (this.isSubmitting()) return;

    const companyName = this.companyInputRef?.nativeElement.value.trim() ?? '';
    const email = this.emailInputRef?.nativeElement.value.trim() ?? '';
    const password = this.passInputRef?.nativeElement.value ?? '';

    if (!companyName || !email || !isValidEmail(email)) {
      this.state.showToast('Please fill in every field with a valid value to continue.');
      return;
    }
    const passwordIssue = validatePassword(password);
    if (passwordIssue) {
      this.state.showToast(passwordIssue);
      return;
    }

    this.isSubmitting.set(true);
    // The Figma form collects a company/workspace name but not the registrant's own
    // full name, which auth-service's RegisterRequest requires -- derived here from
    // the company name since there's no field to add without changing the HTML.
    const fullName = `${companyName} Admin`;

    this.authService
      .register({ fullName, email, password, userType: UserType.COMPANY_ADMIN, companyName })
      .subscribe({
        next: () => {
          this.isSubmitting.set(false);
          this.state.showToast('Workspace created! Please verify your email before logging in — check your inbox for the link.');
          this.router.navigate(['/login'], { queryParams: { tab: 'team' } });
        },
        error: (err) => {
          this.isSubmitting.set(false);
          this.state.showToast(err?.error?.message ?? 'Could not create workspace. Please try again.');
        },
      });
  }
}
