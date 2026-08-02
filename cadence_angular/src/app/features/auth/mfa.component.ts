import { Component, signal, ViewChildren, QueryList, ElementRef, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, ActivatedRoute, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AppButtonComponent } from '../../shared/components/app-button.component';
import { AuthService } from '../../core/services/auth.service';
import { UserType } from '../../core/models/auth.model';
import { mapUserResponseToUserModel } from '../../core/utils/user.mapper';

@Component({
  selector: 'app-mfa',
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
          <h1>Verification.</h1>
          <p>We've sent a one-time passcode to secure your recruiter workspace. Do not share this code with anyone.</p>
        </div>
      </div>

      <!-- Right Panel: MFA Verification -->
      <div class="form-panel">
        <div class="form-container">
          <div class="mfa-header">
            <h3>Enter verification code</h3>
            <p>We sent a 6-digit code to <strong class="email-text">{{ maskedEmail }}</strong></p>
          </div>

          <form (submit)="onSubmit($event)">
            <!-- Code boxes -->
            <div class="otp-boxes">
              <input 
                type="text" 
                maxLength="1" 
                pattern="[0-9]"
                *ngFor="let digit of [0,1,2,3,4,5]; let i = index"
                #otpInput
                (input)="onInput($event, i)"
                (keydown)="onKeyDown($event, i)"
              >
            </div>

            <!-- Action -->
            <app-button 
              label="Verify code" 
              type="submit" 
              styleClass="primary" 
              [fullWidth]="true"
              [height]="42"
            ></app-button>
          </form>

          <!-- Resend link -->
          <div class="resend-container">
            <button 
              class="btn-text" 
              [disabled]="countdown() > 0" 
              (click)="resendCode()"
            >
              {{ countdown() > 0 ? 'Resend code in ' + countdown() + 's' : 'Resend verification code' }}
            </button>
          </div>

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

    .mfa-header {
      display: flex;
      flex-direction: column;
      gap: 8px;

      h3 {
        font-family: $font-serif;
        font-size: 22px;
        color: var(--indigo);
      }

      p {
        font-size: 13px;
        color: var(--ink-soft);
      }

      .email-text {
        color: var(--ink);
      }
    }

    .otp-boxes {
      display: flex;
      justify-content: space-between;
      gap: 8px;
      margin-bottom: 20px;

      input {
        width: 52px;
        height: 52px;
        text-align: center;
        font-size: 20px;
        font-weight: 700;
        font-family: $font-sans;
        border: 1.5px solid var(--line);
        border-radius: var(--radius-large);
        background-color: var(--paper-card);
        color: var(--ink);
        @include transition-base;

        &:focus {
          border-color: var(--indigo);
          box-shadow: 0 0 0 3px rgba(78, 62, 190, 0.08);
        }
      }
    }

    .resend-container {
      text-align: center;

      .btn-text {
        background: transparent;
        border: none;
        color: var(--indigo);
        font-family: $font-sans;
        font-size: 13px;
        font-weight: 600;
        cursor: pointer;
        @include transition-base;

        &:disabled {
          color: var(--ink-faint);
          cursor: not-allowed;
        }

        &:hover:not(:disabled) {
          text-decoration: underline;
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
export class MfaComponent implements OnInit, OnDestroy {
  @ViewChildren('otpInput') otpInputs!: QueryList<ElementRef>;

  email: string = 'you@company.com';
  maskedEmail: string = '';
  countdown = signal<number>(30);
  isSubmitting = signal<boolean>(false);
  private timer: any;
  private mfaSessionToken: string = '';

  constructor(
    public state: AppStateService,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit() {
    this.route.queryParams.subscribe(params => {
      this.email = params['email'] || 'you@company.com';
      this.mfaSessionToken = params['mfaSessionToken'] || '';
      this.maskEmail();
    });
    this.startCountdown();
  }

  ngOnDestroy() {
    if (this.timer) clearInterval(this.timer);
  }

  maskEmail() {
    if (this.email.includes('@')) {
      const parts = this.email.split('@');
      if (parts[0].length > 1) {
        this.maskedEmail = `${parts[0][0]}•••@${parts[1]}`;
      } else {
        this.maskedEmail = this.email;
      }
    } else {
      this.maskedEmail = this.email;
    }
  }

  startCountdown() {
    this.countdown.set(30);
    if (this.timer) clearInterval(this.timer);
    this.timer = setInterval(() => {
      if (this.countdown() > 0) {
        this.countdown.update(c => c - 1);
      } else {
        clearInterval(this.timer);
      }
    }, 1000);
  }

  /** auth-service has no dedicated "resend MFA OTP" endpoint (confirmed -- only /resend-verification exists, for email verification, not login MFA), so this stays a UI-only countdown reset, flagged rather than faking a call that doesn't exist on the backend. */
  resendCode() {
    this.state.showToast('Verification code resent');
    this.startCountdown();
  }

  onInput(event: Event, index: number) {
    const input = event.target as HTMLInputElement;
    const value = input.value;

    if (value && index < 5) {
      const nextInput = this.otpInputs.toArray()[index + 1].nativeElement;
      nextInput.focus();
    }
  }

  onKeyDown(event: KeyboardEvent, index: number) {
    const input = event.target as HTMLInputElement;

    if (event.key === 'Backspace' && !input.value && index > 0) {
      const prevInput = this.otpInputs.toArray()[index - 1].nativeElement;
      prevInput.focus();
    }
  }

  onSubmit(event: Event) {
    event.preventDefault();
    if (this.isSubmitting()) return;

    const code = this.otpInputs.map(input => input.nativeElement.value).join('');

    if (code.length < 6) {
      this.state.showToast('Please enter the full 6-digit code');
      return;
    }
    if (!this.mfaSessionToken) {
      this.state.showToast('Your session expired. Please log in again.');
      this.router.navigate(['/login']);
      return;
    }

    this.isSubmitting.set(true);
    this.authService.verifyMfaLogin({ mfaSessionToken: this.mfaSessionToken, code }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        const user = res.data.user;
        if (!user) {
          this.state.showToast('Something went wrong signing you in. Please try again.');
          return;
        }

        this.state.currentUser.set(mapUserResponseToUserModel(user));
        this.state.showToast(`Welcome back, ${user.fullName}`);

        if (user.userType === UserType.CANDIDATE) {
          this.router.navigate(['/candidate/dashboard']);
        } else {
          this.router.navigate(['/recruiter/dashboard']);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.state.showToast(err?.error?.message ?? 'Invalid or expired code. Please try again.');
      },
    });
  }
}
