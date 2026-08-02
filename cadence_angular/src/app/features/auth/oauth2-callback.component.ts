import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AuthService } from '../../core/services/auth.service';
import { UserType } from '../../core/models/auth.model';
import { mapUserResponseToUserModel } from '../../core/utils/user.mapper';

/** Lands here after Google redirects back through the backend's OAuth2 callback,
 * which hands the browser a one-time code (see AuthService.exchangeOAuthCode) --
 * this page just redeems it and routes into the app like any other login. */
@Component({
  selector: 'app-oauth2-callback',
  standalone: true,
  imports: [CommonModule, RouterLink],
  template: `
    <div class="split-viewport">
      <div class="form-panel">
        <div class="form-container">
          <ng-container *ngIf="!failed(); else errorState">
            <h3>Signing you in…</h3>
            <p>Hang tight while we finish connecting your Google account.</p>
          </ng-container>
          <ng-template #errorState>
            <h3>Sign-in failed</h3>
            <p>{{ errorMessage() }}</p>
            <a routerLink="/login">← Back to login</a>
          </ng-template>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .split-viewport { display:flex; align-items:center; justify-content:center; width:100vw; height:100vh; background-color:var(--paper); }
    .form-container { max-width:378px; text-align:center; display:flex; flex-direction:column; gap:10px; }
    h3 { font-family:'Playfair Display', serif; font-size:22px; color:var(--indigo); }
    p { font-size:13px; color:var(--ink-soft); }
    a { font-size:12.5px; color:var(--ink-soft); font-weight:500; }
  `]
})
export class Oauth2CallbackComponent implements OnInit {
  failed = () => this._failed;
  errorMessage = () => this._errorMessage;
  private _failed = false;
  private _errorMessage = 'That sign-in link is invalid or expired.';

  constructor(
    private state: AppStateService,
    private route: ActivatedRoute,
    private router: Router,
    private authService: AuthService
  ) {}

  ngOnInit() {
    const code = this.route.snapshot.queryParamMap.get('code');
    if (!code) {
      this._failed = true;
      return;
    }

    this.authService.exchangeOAuthCode(code).subscribe({
      next: (res) => {
        const auth = res.data;
        if (!auth.user) {
          this._failed = true;
          return;
        }
        this.state.currentUser.set(mapUserResponseToUserModel(auth.user));
        this.state.showToast(`Welcome, ${auth.user.fullName}`);
        if (auth.user.userType === UserType.CANDIDATE) {
          this.router.navigate(['/candidate/dashboard']);
        } else {
          this.router.navigate(['/recruiter/dashboard']);
        }
      },
      error: (err) => {
        this._failed = true;
        this._errorMessage = err?.error?.message ?? 'That sign-in link is invalid or expired.';
      },
    });
  }
}
