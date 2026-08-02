import { Component, OnInit, signal, ViewChild, ElementRef } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AuthService } from '../../core/services/auth.service';
import { UserType } from '../../core/models/auth.model';
import { mapUserResponseToUserModel } from '../../core/utils/user.mapper';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div id="auth-view">
      <div class="app">

        <!-- ================= BRAND PANEL ================= -->
        <aside class="brand-panel">
          <div class="brandmark"><span class="mark">C</span> Cadence</div>

          <div class="brand-copy">
            <h1>Every candidate,<br>one <em>live</em> pipeline.</h1>
            <p>Resume screening, AI interviews and scheduling — synced across your whole hiring team in real time.</p>

            <div class="pipeline">
              <div class="pipeline-track"></div>
              <div class="particle" style="animation-delay:0s"></div>
              <div class="particle" style="animation-delay:1.1s"></div>
              <div class="particle" style="animation-delay:2.2s"></div>

              <div class="stage">
                <div class="stage-node"><svg viewBox="0 0 24 24"><path d="M4 12h16M4 12l6-6M4 12l6 6"/></svg></div>
                <div class="stage-label">Applicants</div>
                <div class="stage-count" id="c1">1,204</div>
              </div>
              <div class="stage">
                <div class="stage-node"><svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="8"/></svg></div>
                <div class="stage-label">AI Shortlisted</div>
                <div class="stage-count" id="c2">318</div>
              </div>
              <div class="stage">
                <div class="stage-node"><svg viewBox="0 0 24 24"><path d="M4 5h16v14H4z"/><path d="M4 9h16"/></svg></div>
                <div class="stage-label">Interviewed</div>
                <div class="stage-count" id="c3">96</div>
              </div>
              <div class="stage">
                <div class="stage-node hired"><svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg></div>
                <div class="stage-label">Offer released</div>
                <div class="stage-count" id="c4">22</div>
              </div>
            </div>
          </div>

          <div class="brand-stat">
            <strong>38% faster</strong> average time-to-hire reported across Cadence workspaces this quarter.
          </div>
        </aside>

        <!-- ================= FORM PANEL ================= -->
        <main class="form-panel">
          <div class="form-card">
            <div class="mobile-mark"><span class="mark">C</span> Cadence</div>

            <!-- ---------------- LOGIN ---------------- -->
            <section class="view active" id="view-login">
              <div class="tabs" role="tablist" aria-label="Sign in as">
                <button class="tab" role="tab" [attr.aria-selected]="activeTab() === 'team'" id="tab-team" (click)="activeTab.set('team')">Team</button>
                <button class="tab" role="tab" [attr.aria-selected]="activeTab() === 'candidate'" id="tab-candidate" (click)="activeTab.set('candidate')">Candidate</button>
              </div>

              <!-- TEAM LOGIN -->
              <div id="login-team" *ngIf="activeTab() === 'team'">
                <h1 class="title">Sign in to your workspace</h1>
                <p class="subtitle">For recruiters, hiring managers and admins.</p>

                <form id="team-login-form" (submit)="onSubmit($event)" novalidate>
                  <div class="field">
                    <label for="team-email">Work email</label>
                    <div class="input-wrap has-icon">
                      <svg class="icon-left" viewBox="0 0 24 24"><path d="M3 6l9 6 9-6"/><rect x="3" y="5" width="18" height="14" rx="2"/></svg>
                      <input id="team-email" type="email" placeholder="you@company.com" (input)="clearErrors()" autocomplete="email" [class.field-error]="emailError()" #teamEmailInput>
                    </div>
                    <div class="field-error-msg" [class.show]="emailError()">{{ emailError() || 'Enter your work email to continue.' }}</div>
                  </div>
                  <div class="field">
                    <div class="field-row">
                      <label for="team-pass">Password</label>
                      <button type="button" class="link" style="background:none;border:none;color:var(--indigo);font-size:12px;cursor:pointer;padding:0;font-family:'Inter',sans-serif;" (click)="forgotPassword()">Forgot password?</button>
                    </div>
                    <div class="input-wrap">
                      <input id="team-pass" [type]="showPassword() ? 'text' : 'password'" placeholder="••••••••" (input)="clearErrors()" autocomplete="current-password" [class.field-error]="passwordError()" #teamPasswordInput>
                      <button type="button" class="toggle-eye" (click)="showPassword.set(!showPassword())" aria-label="Show password">
                        <svg viewBox="0 0 24 24"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"/><circle cx="12" cy="12" r="3"/></svg>
                      </button>
                    </div>
                    <div class="field-error-msg" [class.show]="passwordError()">{{ passwordError() || 'Enter your password to continue.' }}</div>
                  </div>
                  <label class="checkbox-row"><input type="checkbox" #rememberMeInput> Keep me signed in on this device</label>
                  <button class="btn-primary" type="submit">Continue</button>
                </form>

                <p class="footer-note">New company? <button type="button" class="link" (click)="registerCompany()">Create a workspace</button></p>
              </div>

              <!-- CANDIDATE LOGIN -->
              <div id="login-candidate" *ngIf="activeTab() === 'candidate'">
                <h1 class="title">Welcome back</h1>
                <p class="subtitle">Track applications, take interviews and manage offers.</p>

                <form id="cand-login-form" (submit)="onSubmit($event)" novalidate>
                  <div class="field">
                    <label for="cand-email">Email</label>
                    <div class="input-wrap has-icon">
                      <svg class="icon-left" viewBox="0 0 24 24"><path d="M3 6l9 6 9-6"/><rect x="3" y="5" width="18" height="14" rx="2"/></svg>
                      <input id="cand-email" type="email" placeholder="you@email.com" (input)="clearErrors()" autocomplete="email" [class.field-error]="emailError()" #candEmailInput>
                    </div>
                    <div class="field-error-msg" [class.show]="emailError()">{{ emailError() || 'Enter your email to continue.' }}</div>
                  </div>
                  <div class="field">
                    <div class="field-row">
                      <label for="cand-pass">Password</label>
                      <button type="button" style="background:none;border:none;color:var(--indigo);font-size:12px;cursor:pointer;padding:0;font-family:'Inter',sans-serif;" (click)="forgotPassword()">Forgot password?</button>
                    </div>
                    <div class="input-wrap">
                      <input id="cand-pass" [type]="showPassword() ? 'text' : 'password'" placeholder="••••••••" (input)="clearErrors()" autocomplete="current-password" [class.field-error]="passwordError()" #candPasswordInput>
                      <button type="button" class="toggle-eye" (click)="showPassword.set(!showPassword())" aria-label="Show password">
                        <svg viewBox="0 0 24 24"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"/><circle cx="12" cy="12" r="3"/></svg>
                      </button>
                    </div>
                    <div class="field-error-msg" [class.show]="passwordError()">{{ passwordError() || 'Enter your password to continue.' }}</div>
                  </div>
                  <button class="btn-primary" type="submit" style="margin-top:4px;">Sign in</button>
                </form>

                <div class="divider">or continue with</div>
                <button type="button" class="btn-secondary" (click)="authService.startGoogleLogin()">
                  <svg viewBox="0 0 24 24" width="16" height="16"><path fill="#4285F4" d="M23.5 12.3c0-.8-.1-1.6-.2-2.3H12v4.5h6.5c-.3 1.5-1.1 2.7-2.4 3.6v3h3.9c2.3-2.1 3.5-5.2 3.5-8.8z"/><path fill="#34A853" d="M12 24c3.2 0 5.9-1.1 7.9-2.9l-3.9-3c-1.1.7-2.4 1.1-4 1.1-3.1 0-5.7-2.1-6.6-4.9H1.4v3.1C3.4 21.4 7.4 24 12 24z"/><path fill="#FBBC05" d="M5.4 14.3c-.2-.7-.4-1.5-.4-2.3s.1-1.6.4-2.3V6.6H1.4C.5 8.3 0 10.1 0 12s.5 3.7 1.4 5.4l4-3.1z"/><path fill="#EA4335" d="M12 4.8c1.7 0 3.3.6 4.5 1.8l3.4-3.4C17.9 1.2 15.2 0 12 0 7.4 0 3.4 2.6 1.4 6.6l4 3.1C6.3 6.9 8.9 4.8 12 4.8z"/></svg>
                  Google
                </button>

                <p class="footer-note">First time applying? <button type="button" class="link" (click)="registerCandidate()">Create an account</button></p>
              </div>
            </section>
          </div>
        </main>
      </div>
    </div>
  `,
  styles: [`
    #auth-view {
      display: flex;
      flex-direction: column;
      min-height: 100vh;
      width: 100%;
    }
    .app {
      flex: 1;
      display: grid;
      grid-template-columns: 42% 58%;
      min-height: 640px;
    }
    @media (max-width: 860px) {
      .app {
        grid-template-columns: 1fr;
      }
    }
  `]
})
export class LoginComponent implements OnInit {
  @ViewChild('teamEmailInput') teamEmailInput!: ElementRef<HTMLInputElement>;
  @ViewChild('teamPasswordInput') teamPasswordInput!: ElementRef<HTMLInputElement>;
  @ViewChild('candEmailInput') candEmailInput!: ElementRef<HTMLInputElement>;
  @ViewChild('candPasswordInput') candPasswordInput!: ElementRef<HTMLInputElement>;
  @ViewChild('rememberMeInput') rememberMeInput!: ElementRef<HTMLInputElement>;

  activeTab = signal<'team' | 'candidate'>('team');
  showPassword = signal<boolean>(false);
  isSubmitting = signal<boolean>(false);

  emailError = signal<string | null>(null);
  passwordError = signal<string | null>(null);

  constructor(
    public state: AppStateService,
    private router: Router,
    private route: ActivatedRoute,
    public authService: AuthService
  ) {}

  ngOnInit() {
    if (this.route.snapshot.queryParamMap.get('tab') === 'candidate') {
      this.activeTab.set('candidate');
    }
  }

  clearErrors() {
    this.emailError.set(null);
    this.passwordError.set(null);
  }

  onSubmit(event: Event) {
    event.preventDefault();
    this.clearErrors();

    let email = '';
    let password = '';

    if (this.activeTab() === 'team') {
      email = this.teamEmailInput?.nativeElement.value.trim() ?? '';
      password = this.teamPasswordInput?.nativeElement.value.trim() ?? '';
    } else {
      email = this.candEmailInput?.nativeElement.value.trim() ?? '';
      password = this.candPasswordInput?.nativeElement.value.trim() ?? '';
    }

    let valid = true;

    if (!email || !email.includes('@')) {
      this.emailError.set(this.activeTab() === 'team' ? 'Enter your work email to continue.' : 'Enter your email to continue.');
      valid = false;
    }
    if (!password) {
      this.passwordError.set('Enter your password to continue.');
      valid = false;
    }

    if (!valid || this.isSubmitting()) {
      return;
    }

    this.isSubmitting.set(true);
    const rememberMe = this.rememberMeInput?.nativeElement.checked ?? false;

    this.authService.login({ email, password, rememberMe }).subscribe({
      next: (res) => {
        this.isSubmitting.set(false);
        const auth = res.data;

        if (auth.mfaRequired) {
          this.router.navigate(['/mfa'], { queryParams: { email, mfaSessionToken: auth.mfaSessionToken } });
          return;
        }

        if (!auth.user) {
          this.emailError.set('Something went wrong signing you in. Please try again.');
          return;
        }

        this.state.currentUser.set(mapUserResponseToUserModel(auth.user));
        this.state.showToast(`Welcome back, ${auth.user.fullName}`);

        if (auth.user.userType === UserType.CANDIDATE) {
          this.router.navigate(['/candidate/dashboard']);
        } else {
          this.router.navigate(['/recruiter/dashboard']);
        }
      },
      error: (err) => {
        this.isSubmitting.set(false);
        this.passwordError.set(err?.error?.message ?? 'Invalid email or password.');
      },
    });
  }

  forgotPassword() {
    this.router.navigate(['/forgot-password']);
  }

  registerCompany() {
    this.router.navigate(['/register-company']);
  }

  registerCandidate() {
    this.router.navigate(['/register-candidate']);
  }
}
