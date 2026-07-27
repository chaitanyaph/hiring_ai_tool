import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AppButtonComponent } from '../../shared/components/app-button.component';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-reset-password',
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
          <p>Reset your credentials. Provide a strong password to re-enable workspace access.</p>
        </div>
      </div>

      <!-- Right Panel: Password Reset Form -->
      <div class="form-panel">
        <div class="form-container">
          <div class="header-sec">
            <h3>Reset your password</h3>
            <p>Define a new secure password for your account.</p>
          </div>

          <form (submit)="onSubmit($event)">
            <!-- Temporary Code -->
            <div class="input-field">
              <label>Temporary code</label>
              <input type="text" placeholder="e.g. 123456" required #tokenInput [value]="prefillToken()">
            </div>

            <!-- New Password -->
            <div class="input-field">
              <label>New password</label>
              <input type="password" placeholder="••••••••" required #newPasswordInput>
            </div>

            <!-- Confirm Password -->
            <div class="input-field">
              <label>Confirm new password</label>
              <input type="password" placeholder="••••••••" required #confirmPasswordInput>
            </div>

            <!-- Action -->
            <app-button 
              label="Reset password" 
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
export class ResetPasswordComponent implements OnInit {
  @ViewChild('tokenInput') tokenInputRef!: ElementRef<HTMLInputElement>;
  @ViewChild('newPasswordInput') newPasswordInputRef!: ElementRef<HTMLInputElement>;
  @ViewChild('confirmPasswordInput') confirmPasswordInputRef!: ElementRef<HTMLInputElement>;

  /** If the reset link carried ?token=..., prefill the code field; it stays editable either way. */
  prefillToken = signal<string>('');
  isSubmitting = signal<boolean>(false);

  constructor(
    public state: AppStateService,
    private router: Router,
    private route: ActivatedRoute,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.prefillToken.set(this.route.snapshot.queryParamMap.get('token') ?? '');
  }

  onSubmit(event: Event) {
    event.preventDefault();
    if (this.isSubmitting()) return;

    const token = this.tokenInputRef?.nativeElement.value.trim() ?? '';
    const newPassword = this.newPasswordInputRef?.nativeElement.value ?? '';
    const confirmPassword = this.confirmPasswordInputRef?.nativeElement.value ?? '';

    if (!token || !newPassword) {
      this.state.showToast('Please fill in every field to continue.');
      return;
    }
    if (newPassword !== confirmPassword) {
      this.state.showToast('Passwords do not match.');
      return;
    }

    this.isSubmitting.set(true);
    this.authService.resetPassword(token, newPassword).subscribe({
      next: () => {
        this.isSubmitting.set(false);
        this.state.showToast('Password updated successfully!');
        this.router.navigate(['/login']);
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.state.showToast(err?.error?.message ?? 'That code is invalid or expired.');
      },
    });
  }
}
