import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AuthService } from '../../core/services/auth.service';
import { AppButtonComponent } from '../../shared/components/app-button.component';
import { isValidEmail } from '../../core/utils/validators';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterLink, AppButtonComponent],
  template: `
    <div class="split-viewport">
      <div class="brand-panel">
        <div class="brand-header">
          <div class="logo">H</div>
          <h2>HirePilot</h2>
        </div>
        <div class="brand-content">
          <h1>Almost there.</h1>
          <p>Confirming your email address so you can start using your workspace.</p>
        </div>
      </div>

      <div class="form-panel">
        <div class="form-container">
          <ng-container [ngSwitch]="status()">
            <div *ngSwitchCase="'verifying'" class="header-sec">
              <div class="spinner-lg"></div>
              <h3>Verifying your email…</h3>
              <p>This will just take a moment. Please don't close this page.</p>
            </div>
            <div *ngSwitchCase="'success'" class="header-sec">
              <div class="status-icon success">
                <svg viewBox="0 0 24 24" style="width:22px;height:22px;stroke:white;stroke-width:3;fill:none;"><path d="M5 13l4 4L19 7"/></svg>
              </div>
              <h3>Email verified</h3>
              <p>Your account is ready to go. Sign in to start using your workspace.</p>
            </div>
            <div *ngSwitchCase="'error'" class="header-sec">
              <div class="status-icon error">
                <svg viewBox="0 0 24 24" style="width:22px;height:22px;stroke:white;stroke-width:3;fill:none;"><path d="M6 6l12 12M18 6L6 18"/></svg>
              </div>
              <h3>Verification failed</h3>
              <p>{{ errorMessage() }}</p>
              <p class="hint">If your link expired, you can request a new one below.</p>
            </div>
          </ng-container>

          <form *ngIf="status() === 'error'" class="resend-form" (submit)="onResend($event)">
            <div class="input-field">
              <label>Email address</label>
              <input type="email" placeholder="you@email.com" required #resendEmailInput (input)="clearResendMessage()">
            </div>
            <app-button
              [label]="isResending() ? 'Sending…' : 'Resend verification email'"
              type="submit"
              styleClass="primary"
              [fullWidth]="true"
              [height]="40"
              [disabled]="isResending()"
            ></app-button>
            <p class="resend-message" *ngIf="resendMessage()" [class.error]="resendError()">{{ resendMessage() }}</p>
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

      p.hint {
        margin-top: 4px;
      }
    }

    .spinner-lg {
      width: 30px;
      height: 30px;
      border: 3px solid var(--line);
      border-top-color: var(--indigo);
      border-radius: 50%;
      animation: spin 0.8s linear infinite;
      margin-bottom: 4px;
    }

    @keyframes spin {
      to { transform: rotate(360deg); }
    }

    .status-icon {
      width: 40px;
      height: 40px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 4px;

      &.success { background-color: var(--teal); }
      &.error { background-color: var(--danger); }
    }

    .resend-form {
      display: flex;
      flex-direction: column;
      gap: 14px;
      margin-top: 6px;

      .input-field {
        display: flex;
        flex-direction: column;
        gap: 6px;

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

          &:focus {
            border-color: var(--indigo);
          }
        }
      }
    }

    .resend-message {
      font-size: 12.5px;
      color: var(--teal);
      margin: 0;

      &.error {
        color: var(--danger);
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
export class VerifyEmailComponent implements OnInit {
  @ViewChild('resendEmailInput') resendEmailInput!: ElementRef<HTMLInputElement>;

  status = signal<'verifying' | 'success' | 'error'>('verifying');
  errorMessage = signal<string>('That link is invalid or expired.');

  isResending = signal<boolean>(false);
  resendMessage = signal<string | null>(null);
  resendError = signal<boolean>(false);

  constructor(
    public state: AppStateService,
    private route: ActivatedRoute,
    private authService: AuthService
  ) {}

  ngOnInit() {
    const token = this.route.snapshot.queryParamMap.get('token');
    if (!token) {
      this.status.set('error');
      return;
    }

    this.authService.verifyEmail(token).subscribe({
      next: () => {
        this.status.set('success');
        this.state.showToast('Email verified! You can now log in.');
      },
      error: (err) => {
        this.status.set('error');
        this.errorMessage.set(err?.error?.message ?? 'That link is invalid or expired.');
      },
    });
  }

  clearResendMessage() {
    this.resendMessage.set(null);
  }

  onResend(event: Event) {
    event.preventDefault();
    if (this.isResending()) return;

    const email = this.resendEmailInput?.nativeElement.value.trim() ?? '';
    if (!isValidEmail(email)) {
      this.resendError.set(true);
      this.resendMessage.set('Enter a valid email address.');
      return;
    }

    this.isResending.set(true);
    this.authService.resendVerification(email).subscribe({
      next: () => {
        this.isResending.set(false);
        this.resendError.set(false);
        this.resendMessage.set('Verification email sent — check your inbox.');
      },
      error: (err) => {
        this.isResending.set(false);
        this.resendError.set(true);
        this.resendMessage.set(err?.error?.message ?? 'Could not resend the email. Please try again.');
      },
    });
  }
}
