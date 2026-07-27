import { Component, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AppButtonComponent } from '../../shared/components/app-button.component';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-forgot-password',
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
          <h1>Security.</h1>
          <p>Request a secure password override link. We will send you instructions shortly.</p>
        </div>
      </div>

      <!-- Right Panel: Password Form -->
      <div class="form-panel">
        <div class="form-container">
          <div class="header-sec">
            <h3>Forgot your password?</h3>
            <p>Enter your email and we'll send you a link to reset it.</p>
          </div>

          <form (submit)="onSubmit($event)">
            <!-- Email -->
            <div class="input-field">
              <label>Work or personal email</label>
              <input type="email" placeholder="you@company.com" required #emailInput>
            </div>

            <!-- Action -->
            <app-button 
              label="Send reset link" 
              type="submit" 
              styleClass="primary" 
              [fullWidth]="true"
              [height]="42"
            ></app-button>
          </form>

          <div class="back-link">
            <a routerLink="/login">← Back to login</a>
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

    .back-link {
      text-align: center;
      font-size: 12.5px;

      a {
        color: var(--ink-soft);
        font-weight: 500;
      }
    }
  `]
})
export class ForgotPasswordComponent {
  @ViewChild('emailInput') emailInputRef!: ElementRef<HTMLInputElement>;
  isSubmitting = signal<boolean>(false);

  constructor(public state: AppStateService, private router: Router, private authService: AuthService) {}

  onSubmit(event: Event) {
    event.preventDefault();
    if (this.isSubmitting()) return;

    const email = this.emailInputRef?.nativeElement.value.trim() ?? '';
    if (!email || !email.includes('@')) {
      this.state.showToast('Enter a valid email to continue.');
      return;
    }

    this.isSubmitting.set(true);
    // Backend always returns success here regardless of whether the account exists
    // (avoids leaking which emails are registered), so the toast/navigation is the
    // same either way -- that's a deliberate security choice, not a missing check.
    this.authService.forgotPassword(email).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.state.showToast('If an account exists, instructions have been sent to your email.');
        this.router.navigate(['/reset-password']);
      },
      error: () => {
        this.isSubmitting.set(false);
        this.state.showToast('Something went wrong. Please try again.');
      },
    });
  }
}
