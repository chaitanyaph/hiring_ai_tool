import { Component, Input, HostListener, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { AuthService } from '../../core/services/auth.service';

@Component({
  selector: 'app-sidebar',
  standalone: true,
  imports: [CommonModule, RouterLink, RouterLinkActive],
  template: `
    <div class="sidebar">
      <!-- Workspace Switcher -->
      <div class="workspace-switch">
        <div class="mark">{{ isCandidate ? 'C' : (state.company()?.companyName?.charAt(0)?.toUpperCase() || 'C') }}</div>
        <div>
          <div class="ws-name">{{ isCandidate ? 'Cadence' : (state.company()?.companyName || 'Your workspace') }}</div>
          <div class="ws-sub">{{ isCandidate ? 'Candidate Portal' : 'Company Admin' }}</div>
        </div>
        <svg viewBox="0 0 24 24" style="margin-left:auto;width:14px;height:14px;stroke:rgba(250,249,244,0.6);fill:none;stroke-width:2;">
          <path d="M6 9l6 6 6-6"/>
        </svg>
      </div>

      <!-- Navigation -->
      <nav class="navlist" *ngIf="isCandidate; else recruiterNav">
        <a class="navlink" routerLink="/candidate/dashboard" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="9" rx="1.5"/><rect x="14" y="3" width="7" height="5" rx="1.5"/><rect x="14" y="12" width="7" height="9" rx="1.5"/><rect x="3" y="16" width="7" height="5" rx="1.5"/></svg>
          Dashboard
        </a>
        <a class="navlink" routerLink="/candidate/profile" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="8" r="4"/><path d="M4 21c0-4.4 3.6-7 8-7s8 2.6 8 7"/></svg>
          My Profile
        </a>
        <a class="navlink" routerLink="/candidate/resumes" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><path d="M14 2v6h6"/><path d="M9 13h6M9 17h6"/></svg>
          My Resumes
        </a>
        <a class="navlink" routerLink="/candidate/browse-jobs" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
          Browse Jobs
        </a>
        <a class="navlink" routerLink="/candidate/applications" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87m-4-12a4 4 0 010 7.75"/></svg>
          My Applications
        </a>
        <a class="navlink" routerLink="/candidate/interviews" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
          My Interviews
        </a>

        <div class="nav-section-label">AI Tools</div>
        <a class="navlink" routerLink="/candidate/ai-coach" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><path d="M12 2l2.4 6.6L21 11l-6.6 2.4L12 20l-2.4-6.6L3 11l6.6-2.4z"/></svg>
          AI Career Coach
        </a>
        <a class="navlink" routerLink="/candidate/coding-assessments" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><path d="M9 3h6l1 4H8l1-4z"/><rect x="4" y="7" width="16" height="14" rx="2"/><path d="M9 12l2 2-2 2M14 16h2"/></svg>
          Coding Assessments
        </a>
        <a class="navlink" routerLink="/candidate/offers" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><path d="M20 12v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6"/><path d="M2 7h20v5H2z"/><path d="M12 22V7M12 7a2.5 2.5 0 010-5C13.5 2 12 5 12 7zM12 7a2.5 2.5 0 000-5C10.5 2 12 5 12 7z"/></svg>
          Offers
        </a>

        <div class="nav-section-label">Account</div>
        <a class="navlink" routerLink="/candidate/notifications" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 01-3.4 0"/></svg>
          Notifications
        </a>
        <a class="navlink" routerLink="/candidate/settings" routerLinkActive="active">
          <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06A1.65 1.65 0 004.6 9a1.65 1.65 0 00-1.51-1H3a2 2 0 010-4h.09A1.65 1.65 0 004.6 9"/></svg>
          Settings
        </a>
      </nav>

      <ng-template #recruiterNav>
        <nav class="navlist">
          <a class="navlink" routerLink="/recruiter/dashboard" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="9"/><rect x="14" y="3" width="7" height="5"/><rect x="14" y="12" width="7" height="9"/><rect x="3" y="16" width="7" height="5"/></svg>
            Dashboard
          </a>
          <a class="navlink" routerLink="/recruiter/jobs" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M9 12h6m-6 4h6m2 5H7a2 2 0 01-2-2V5a2 2 0 012-2h5.586a1 1 0 01.707.293l5.414 5.414a1 1 0 01.293.707V19a2 2 0 01-2 2z"/></svg>
            Jobs
          </a>
          <a class="navlink" routerLink="/recruiter/candidates" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M17 21v-2a4 4 0 00-4-4H5a4 4 0 00-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M23 21v-2a4 4 0 00-3-3.87m-4-12a4 4 0 010 7.75"/></svg>
            Candidates
          </a>
          <a class="navlink" routerLink="/recruiter/interviews" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><rect x="3" y="4" width="18" height="18" rx="2" ry="2"/><line x1="16" y1="2" x2="16" y2="6"/><line x1="8" y1="2" x2="8" y2="6"/><line x1="3" y1="10" x2="21" y2="10"/></svg>
            Interviews
          </a>
          <a class="navlink" routerLink="/recruiter/assessments" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-6 9l2 2 4-4"/></svg>
            Assessments
          </a>
          <a class="navlink" routerLink="/recruiter/analytics" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M4 20V10M12 20V4M20 20v-7"/></svg>
            Analytics
          </a>

          <div class="nav-section-label">AI TOOLS</div>
          <a class="navlink" routerLink="/recruiter/ai-assistant" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M12 2l2.4 6.6L21 11l-6.6 2.4L12 20l-2.4-6.6L3 11l6.6-2.4z"/></svg>
            AI Assistant
          </a>
          <a class="navlink" routerLink="/recruiter/parsing" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><path d="M14 2v6h6"/><path d="M9 13h6M9 17h6"/></svg>
            Resume Parsing
          </a>
          <a class="navlink" routerLink="/recruiter/matching" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M8 12l2.5 2.5L16 9"/></svg>
            Resume Matching
          </a>
          <a class="navlink" routerLink="/recruiter/shortlisting" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M9 11l3 3L22 4"/><path d="M21 12v7a2 2 0 01-2 2H5a2 2 0 01-2-2V5a2 2 0 012-2h11"/></svg>
            Shortlisting
          </a>
          <a class="navlink" routerLink="/recruiter/recommendations" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M12 2l2.4 6.6L21 11l-6.6 2.4L12 20l-2.4-6.6L3 11l6.6-2.4z"/><path d="M19 3v4M21 5h-4"/></svg>
            Recommendations
          </a>
          <a class="navlink" routerLink="/recruiter/ai-interviews" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M12 15a3 3 0 003-3V6a3 3 0 00-6 0v6a3 3 0 003 3z"/><path d="M19 11a7 7 0 01-14 0M12 19v3"/></svg>
            AI Interviews
          </a>
          <a class="navlink" routerLink="/recruiter/coding-assessments" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M9 3h6l1 4H8l1-4z"/><rect x="4" y="7" width="16" height="14" rx="2"/><path d="M9 12l2 2-2 2M14 16h2"/></svg>
            Coding Assessments
          </a>
          <a class="navlink" routerLink="/recruiter/question-bank" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M4 19.5A2.5 2.5 0 016.5 17H20"/><path d="M6.5 2H20v20H6.5A2.5 2.5 0 014 19.5v-15A2.5 2.5 0 016.5 2z"/></svg>
            Question Bank
          </a>
          <a class="navlink" routerLink="/recruiter/offers" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M20 12v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6"/><path d="M2 7h20v5H2z"/><path d="M12 22V7M12 7a2.5 2.5 0 010-5C13.5 2 12 5 12 7zM12 7a2.5 2.5 0 000-5C10.5 2 12 5 12 7z"/></svg>
            Offers
          </a>
          <a class="navlink" routerLink="/recruiter/notifications" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><path d="M18 8a6 6 0 10-12 0c0 7-3 9-3 9h18s-3-2-3-9"/><path d="M13.7 21a2 2 0 01-3.4 0"/></svg>
            Notifications
          </a>

          <div class="nav-section-label">WORKSPACE</div>
          <a class="navlink" routerLink="/recruiter/settings" routerLinkActive="active">
            <svg viewBox="0 0 24 24"><circle cx="12" cy="12" r="3"/><path d="M19.4 15a1.65 1.65 0 00.33 1.82l.06.06a2 2 0 11-2.83 2.83l-.06-.06a1.65 1.65 0 00-1.82-.33 1.65 1.65 0 00-1 1.51V21a2 2 0 01-4 0v-.09A1.65 1.65 0 009 19.4a1.65 1.65 0 00-1.82.33l-.06.06a2 2 0 11-2.83-2.83l.06-.06A1.65 1.65 0 004.6 9"/></svg>
            Settings
          </a>
        </nav>
      </ng-template>

      <!-- Footer user -->
      <div class="sidebar-footer" (click)="toggleDropdown($event)">
        <div class="avatar-sm">{{ userInitials }}</div>
        <div>
          <div class="name">{{ userName }}</div>
          <div class="role">{{ isCandidate ? 'Candidate' : 'Company Admin' }}</div>
        </div>
        <!-- Logout menu -->
        <div class="footer-menu" [ngClass]="{ 'show': showLogoutDropdown() }">
          <button (click)="logout()">Sign out</button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .sidebar {
      width: 232px;
      flex-shrink: 0;
      background: radial-gradient(140% 120% at 10% 0%, var(--indigo-mid) 0%, var(--indigo-deep) 65%);
      color: var(--paper);
      display: flex;
      flex-direction: column;
      padding: 20px 16px;
      position: sticky;
      top: 0;
      height: 100vh;
      overflow-y: auto;
      @include custom-scrollbar;
    }

    .workspace-switch {
      display: flex;
      align-items: center;
      gap: 9px;
      padding: 8px 10px;
      border-radius: 9px;
      background: rgba(250, 249, 244, 0.07);
      cursor: pointer;
      margin-bottom: 22px;

      .mark {
        width: 24px;
        height: 24px;
        border-radius: 6px;
        background: var(--gold);
        display: flex;
        align-items: center;
        justify-content: center;
        color: var(--indigo-deep);
        font-weight: 700;
        font-size: 12px;
        flex-shrink: 0;
      }

      .ws-name {
        font-size: 13px;
        font-weight: 600;
        color: var(--paper);
      }

      .ws-sub {
        font-size: 10.5px;
        color: rgba(250, 249, 244, 0.55);
      }
    }

    .navlist {
      display: flex;
      flex-direction: column;
      gap: 2px;
      flex: 1;
    }

    .nav-section-label {
      font-size: 10.5px;
      text-transform: uppercase;
      letter-spacing: 0.06em;
      color: rgba(250, 249, 244, 0.38);
      margin: 18px 11px 6px;
    }

    .navlink {
      display: flex;
      align-items: center;
      gap: 11px;
      padding: 9px 11px;
      border-radius: 8px;
      font-size: 13.5px;
      color: rgba(250, 249, 244, 0.68);
      text-decoration: none;
      cursor: pointer;
      border: none;
      background: none;
      width: 100%;
      text-align: left;
      font-family: $font-sans;
      @include transition-base;

      svg {
        width: 17px;
        height: 17px;
        stroke: currentColor;
        fill: none;
        stroke-width: 1.7;
        flex-shrink: 0;
      }

      &:hover {
        background: rgba(250, 249, 244, 0.06);
        color: var(--paper);
        text-decoration: none;
      }

      &.active {
        background: rgba(250, 249, 244, 0.12);
        color: var(--paper);
        font-weight: 500;
      }
    }

    .sidebar-footer {
      margin-top: auto;
      display: flex;
      align-items: center;
      gap: 10px;
      padding: 10px;
      border-top: 1px solid rgba(250, 249, 244, 0.12);
      cursor: pointer;
      position: relative;
      border-radius: 10px;
      @include transition-base;

      &:hover {
        background: rgba(250, 249, 244, 0.06);
      }

      .name {
        font-size: 12.5px;
        font-weight: 600;
        color: var(--paper);
      }

      .role {
        font-size: 10.5px;
        color: rgba(250, 249, 244, 0.55);
      }
    }

    .avatar-sm {
      width: 30px;
      height: 30px;
      border-radius: 50%;
      background: var(--gold);
      color: var(--indigo-deep);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 12px;
      font-weight: 700;
      flex-shrink: 0;
    }

    .footer-menu {
      position: absolute;
      bottom: 58px;
      left: 8px;
      right: 8px;
      background: var(--paper-card);
      border: 1px solid var(--line);
      border-radius: 10px;
      box-shadow: 0 10px 28px rgba(28, 27, 41, 0.22);
      padding: 6px;
      display: none;
      z-index: 40;

      &.show { display: block; }

      button {
        width: 100%;
        text-align: left;
        background: none;
        border: none;
        padding: 9px 11px;
        border-radius: 7px;
        font-size: 12.5px;
        color: var(--danger);
        cursor: pointer;
        font-family: $font-sans;
        @include transition-base;

        &:hover { background: var(--danger-tint); }
      }
    }
  `]
})
export class SidebarComponent {
  @Input() isCandidate: boolean = false;
  showLogoutDropdown = signal<boolean>(false);

  constructor(public state: AppStateService, private router: Router, private authService: AuthService) {}

  toggleDropdown(event: MouseEvent) {
    event.stopPropagation();
    this.showLogoutDropdown.set(!this.showLogoutDropdown());
  }

  logout() {
    this.showLogoutDropdown.set(false);
    // Best-effort server-side revocation of the refresh token; the local session
    // is cleared either way so the user is signed out immediately regardless of
    // whether this network call succeeds.
    this.authService.logout().subscribe({
      complete: () => this.finishLogout(),
      error: () => this.finishLogout(),
    });
  }

  private finishLogout() {
    this.authService.clearLocalSession();
    this.state.logout();
    this.router.navigate(['/login']);
  }

  get userName(): string {
    return this.state.currentUser()?.name ?? '';
  }

  get userInitials(): string {
    return this.userName.split(' ').map(n => n[0]).join('').toUpperCase();
  }

  @HostListener('document:click')
  closeDropdown() {
    this.showLogoutDropdown.set(false);
  }
}
