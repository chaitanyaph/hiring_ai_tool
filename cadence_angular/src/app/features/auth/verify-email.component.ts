import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-verify-email',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="split-viewport">
      <div class="brand-panel">
        <div class="brand-header">
          <div class="logo">C</div>
          <h2>Cadence</h2>
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
              <h3>Verifying your email…</h3>
              <p>This will just take a moment.</p>
            </div>
            <div *ngSwitchCase="'success'" class="header-sec">
              <h3>Email verified</h3>
              <p>Your account is ready. You can now sign in.</p>
            </div>
            <div *ngSwitchCase="'error'" class="header-sec">
              <h3>Verification failed</h3>
              <p>{{ errorMessage() }}</p>
            </div>
          </ng-container>

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
  status = signal<'verifying' | 'success' | 'error'>('verifying');
  errorMessage = signal<string>('That link is invalid or expired.');

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
}
