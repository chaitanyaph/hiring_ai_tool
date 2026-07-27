import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';

@Component({
  selector: 'app-settings',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="settings-viewport">

      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>Settings</h1>
          <p>Manage your workspace preferences, team members and billing configuration.</p>
        </div>
      </div>

      <!-- Settings Layout: sidebar nav + content -->
      <div class="settings-layout">

        <!-- Sidebar subnav -->
        <nav class="subnav">
          <button
            *ngFor="let tab of tabs"
            [class.active]="activeTab() === tab.key"
            (click)="activeTab.set(tab.key)">
            <svg viewBox="0 0 24 24" fill="none">
              <path [attr.d]="tab.icon" stroke="currentColor" stroke-width="1.6" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            {{ tab.label }}
          </button>
        </nav>

        <!-- Tab Content -->
        <div class="settings-content">

          <!-- Team & Roles -->
          <ng-container *ngIf="activeTab() === 'team'">
            <div class="card">
              <div class="card-head">
                <h2>Role permissions</h2>
                <a class="card-link" (click)="state.openModal('invite')">Invite teammate</a>
              </div>
              <table class="table perm-table">
                <thead>
                  <tr>
                    <th>Permission</th>
                    <th>Company Admin</th>
                    <th>Recruiter</th>
                    <th>Hiring Manager</th>
                    <th>Interviewer</th>
                  </tr>
                </thead>
                <tbody>
                  <tr>
                    <td>Create &amp; publish jobs</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                    <td class="check-no">–</td>
                    <td class="check-no">–</td>
                  </tr>
                  <tr>
                    <td>View all candidates</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                    <td class="check-no">–</td>
                  </tr>
                  <tr>
                    <td>Schedule interviews</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                    <td class="check-no">–</td>
                  </tr>
                  <tr>
                    <td>Submit interview feedback</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                  </tr>
                  <tr>
                    <td>Send offers</td>
                    <td class="check-yes">✓</td>
                    <td class="check-yes">✓</td>
                    <td class="check-no">–</td>
                    <td class="check-no">–</td>
                  </tr>
                  <tr>
                    <td>Manage team &amp; billing</td>
                    <td class="check-yes">✓</td>
                    <td class="check-no">–</td>
                    <td class="check-no">–</td>
                    <td class="check-no">–</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </ng-container>

          <!-- Profile -->
          <ng-container *ngIf="activeTab() === 'profile'">
            <div class="card">
              <div class="card-head">
                <h2>Your profile</h2>
              </div>
              <div class="form-grid">
                <div class="field">
                  <label>Full name</label>
                  <input [value]="state.currentUser()?.name ?? ''" disabled>
                </div>
                <div class="field">
                  <label>Work email</label>
                  <input [value]="state.currentUser()?.email ?? ''" disabled>
                </div>
                <div class="field">
                  <label>Role</label>
                  <input value="Company Admin" disabled>
                </div>
                <div class="field">
                  <label>Phone</label>
                  <input value="+91 98xxxxx210" disabled>
                </div>
              </div>
              <!-- auth-service has no "update my profile" endpoint (only /auth/change-password) -- flagged, editing disabled rather than faking a save. -->
            </div>
          </ng-container>

          <!-- Company -->
          <ng-container *ngIf="activeTab() === 'company'">
            <div class="card" style="margin-bottom:16px;">
              <div class="card-head">
                <h2>Company details</h2>
              </div>
              <div class="form-grid">
                <div class="field">
                  <label>Company name</label>
                  <input [value]="state.company()?.companyName ?? ''" #companyNameInput>
                </div>
                <div class="field">
                  <label>Workspace URL</label>
                  <input [value]="'cadence.ai/' + (state.company()?.companySlug ?? '')" disabled>
                </div>
                <div class="field">
                  <label>Industry</label>
                  <input [value]="state.company()?.industry ?? ''" #industryInput>
                </div>
                <div class="field">
                  <label>Headquarters</label>
                  <input [value]="state.company()?.headquarters ?? ''" #headquartersInput>
                </div>
              </div>
              <button class="btn-primary-sm" style="margin-top:16px;" (click)="saveCompanyDetails(companyNameInput.value, industryInput.value, headquartersInput.value)">Save changes</button>
            </div>

            <div class="card" style="margin-bottom:16px;">
              <div class="card-head">
                <div>
                  <h2>Departments</h2>
                  <div class="card-sub">Used across jobs, invites and reporting</div>
                </div>
                <a class="card-link" (click)="state.openModal('department')">Add department</a>
              </div>
              <div style="display:flex; flex-wrap:wrap; gap:8px;">
                <span class="skill-pill" style="font-size:11.5px; padding:5px 11px;" *ngFor="let dept of state.departments()">
                  {{ dept.departmentName }}
                  <span class="row-link" style="margin-left:8px; font-size:11px;" (click)="state.removeDepartment(dept.id)">&times;</span>
                </span>
              </div>
            </div>

            <div class="card">
              <div class="card-head">
                <h2>Offices</h2>
                <a class="card-link" (click)="state.openModal('office')">Add office</a>
              </div>
              <table class="table">
                <thead>
                  <tr>
                    <th>Office</th>
                    <th>Address</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let office of state.offices()">
                    <td><b>{{ office.officeName }}</b></td>
                    <td class="muted">{{ office.address || '—' }}</td>
                    <td><span class="row-link" (click)="state.removeOffice(office.id)">Remove</span></td>
                  </tr>
                </tbody>
              </table>
            </div>
          </ng-container>

          <!-- Subscription -->
          <ng-container *ngIf="activeTab() === 'billing'">
            <div class="card">
              <div class="card-head">
                <h2>Subscription</h2>
              </div>
              <div class="plan-card">
                <div class="plan-name">Growth plan</div>
                <div class="plan-price">₹24,999 / month · billed annually</div>
                <div class="source-list" style="margin-bottom:16px; display:flex; flex-direction:column; gap:6px;">
                  <div style="font-size:12.5px; color:var(--ink-soft);">✓ Up to 25 active job postings</div>
                  <div style="font-size:12.5px; color:var(--ink-soft);">✓ Unlimited AI interviews</div>
                  <div style="font-size:12.5px; color:var(--ink-soft);">✓ 10 team seats included</div>
                </div>
                <button class="btn-ghost" (click)="state.showToast('Opening plan comparison…')">Change plan</button>
              </div>
            </div>
          </ng-container>

          <!-- Integrations -->
          <ng-container *ngIf="activeTab() === 'integrations'">
            <div class="card">
              <div class="card-head">
                <h2>Integrations</h2>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Google Calendar</div>
                  <div class="t-sub">Sync interview schedules automatically</div>
                </div>
                <div class="switch on" (click)="state.showToast('Google Calendar toggled')"></div>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Google Meet</div>
                  <div class="t-sub">Auto-generate meeting links for interviews</div>
                </div>
                <div class="switch on" (click)="state.showToast('Google Meet toggled')"></div>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">LinkedIn</div>
                  <div class="t-sub">Import candidate profiles &amp; post jobs</div>
                </div>
                <div class="switch" (click)="state.showToast('LinkedIn toggled')"></div>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Naukri</div>
                  <div class="t-sub">Sync resumes from Naukri database</div>
                </div>
                <div class="switch" (click)="state.showToast('Naukri toggled')"></div>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Slack</div>
                  <div class="t-sub">Get hiring notifications in Slack</div>
                </div>
                <div class="switch" (click)="state.showToast('Slack toggled')"></div>
              </div>
            </div>
          </ng-container>

          <!-- Audit Logs -->
          <ng-container *ngIf="activeTab() === 'audit'">
            <div class="card">
              <div class="card-head">
                <h2>Audit logs</h2>
              </div>
              <div class="audit-item">
                <span><span class="a-who">Ananya Rao</span> published <b>Backend Engineer</b></span>
                <span class="a-time">2h ago</span>
              </div>
              <div class="audit-item">
                <span><span class="a-who">Vikram Shah</span> sent an offer to <b>Rohan Mehta</b></span>
                <span class="a-time">5h ago</span>
              </div>
              <div class="audit-item">
                <span><span class="a-who">System</span> auto-shortlisted 14 candidates</span>
                <span class="a-time">Yesterday</span>
              </div>
              <div class="audit-item">
                <span><span class="a-who">Meera Nair</span> archived <b>Product Designer</b></span>
                <span class="a-time">2 days ago</span>
              </div>
            </div>
          </ng-container>

        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .subnav button {
      display: flex;
      align-items: center;
      gap: 10px;
      width: 100%;
      text-align: left;
      background: none;
      border: none;
      padding: 9px 12px;
      border-radius: 8px;
      font-size: 13px;
      color: var(--ink-soft);
      cursor: pointer;
      font-family: $font-sans;
      transition: background 0.15s, color 0.15s;
    }

    .subnav button:hover {
      background: rgba(28, 27, 41, 0.04);
      color: var(--ink);
    }

    .subnav button.active {
      background: var(--indigo-tint);
      color: var(--indigo);
      font-weight: 600;
    }

    .subnav button svg {
      width: 17px;
      height: 17px;
      stroke: currentColor;
      fill: none;
      stroke-width: 1.6;
      flex-shrink: 0;
    }
  `]
})
export class SettingsComponent {
  activeTab = signal<string>('team');

  tabs = [
    { key: 'team',          label: 'Team & roles', icon: 'M17 20h5v-2a3 3 0 00-5.356-1.857M17 20H7m10 0v-2c0-.656-.126-1.283-.356-1.857M7 20H2v-2a3 3 0 015.356-1.857M7 20v-2c0-.656.126-1.283.356-1.857m0 0a5.002 5.002 0 019.288 0M15 7a3 3 0 11-6 0 3 3 0 016 0zm6 3a2 2 0 11-4 0 2 2 0 014 0zM7 10a2 2 0 11-4 0 2 2 0 014 0z' },
    { key: 'profile',       label: 'Profile',      icon: 'M16 7a4 4 0 11-8 0 4 4 0 018 0zM12 14a7 7 0 00-7 7h14a7 7 0 00-7-7z' },
    { key: 'company',       label: 'Company',      icon: 'M19 21V5a2 2 0 00-2-2H7a2 2 0 00-2 2v16m14 0h2m-2 0h-5m-9 0H3m2 0h5M9 7h1m-1 4h1m4-4h1m-1 4h1m-5 10v-5a1 1 0 011-1h2a1 1 0 011 1v5' },
    { key: 'billing',       label: 'Subscription', icon: 'M3 10h18M7 15h1m4 0h1m-7 4h12a3 3 0 003-3V8a3 3 0 00-3-3H6a3 3 0 00-3-3v8a3 3 0 003 3z' },
    { key: 'integrations',   label: 'Integrations',  icon: 'M11 4a2 2 0 114 0v1a1 1 0 001 1h3a1 1 0 011 1v3a1 1 0 01-1 1h-1a2 2 0 100 4h1a1 1 0 011 1v3a1 1 0 01-1 1h-3a1 1 0 01-1-1v-1a2 2 0 10-4 0v1a1 1 0 01-1 1H7a1 1 0 01-1-1v-3a1 1 0 00-1-1H4a2 2 0 110-4h1a1 1 0 001-1V7a1 1 0 011-1h3a1 1 0 001-1V4z' },
    { key: 'audit',          label: 'Audit logs',   icon: 'M9 5H7a2 2 0 00-2 2v12a2 2 0 002 2h10a2 2 0 002-2V7a2 2 0 00-2-2h-2M9 5a2 2 0 002 2h2a2 2 0 002-2M9 5a2 2 0 012-2h2a2 2 0 012 2m-3 7h3m-3 4h3m-6-4h.01M9 16h.01' }
  ];

  constructor(public state: AppStateService) {}

  saveCompanyDetails(companyName: string, industry: string, headquarters: string) {
    const current = this.state.company();
    if (!companyName.trim()) {
      this.state.showToast('Company name is required.');
      return;
    }
    // updateCompany is a full PUT (no PATCH) -- carry forward the fields this
    // tab doesn't expose (website/email/phone/etc.) so they aren't wiped out.
    this.state.updateCompanyDetails({
      companyName: companyName.trim(),
      industry: industry.trim() || undefined,
      headquarters: headquarters.trim() || undefined,
      website: current?.website ?? undefined,
      companyEmail: current?.companyEmail ?? undefined,
      companyPhone: current?.companyPhone ?? undefined,
      description: current?.description ?? undefined,
      companyLogo: current?.companyLogo ?? undefined,
      subscriptionPlan: current?.subscriptionPlan ?? undefined,
    });
  }
}
