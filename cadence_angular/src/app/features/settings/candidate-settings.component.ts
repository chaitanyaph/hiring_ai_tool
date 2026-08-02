import { Component, OnInit, effect, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { PreferenceCategory } from '../../core/models/notification.model';

@Component({
  selector: 'app-candidate-settings',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-settings">
      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>Settings</h1>
          <p>Manage your account, privacy and notification preferences.</p>
        </div>
      </div>

      <div class="settings-layout">
        <!-- Sidebar tabs -->
        <div class="subnav">
          <button
            [class.active]="activeTab() === 'profile'"
            (click)="activeTab.set('profile')">
            Account
          </button>
          <button
            [class.active]="activeTab() === 'privacy'"
            (click)="activeTab.set('privacy')">
            Privacy
          </button>
          <button
            [class.active]="activeTab() === 'notifications'"
            (click)="activeTab.set('notifications')">
            Notifications
          </button>
          <button
            [class.active]="activeTab() === 'security'"
            (click)="activeTab.set('security')">
            Password &amp; security
          </button>
          <button
            [class.active]="activeTab() === 'danger'"
            (click)="activeTab.set('danger')">
            Delete account
          </button>
        </div>

        <!-- Tab contents -->
        <div>
          <!-- Profile / Account details -->
          <div class="csettings-pane active" *ngIf="activeTab() === 'profile'">
            <div class="card">
              <div class="card-head"><h2>Account details</h2></div>
              <div class="form-grid">
                <div class="field">
                  <label>Full name</label>
                  <input value="Rahul Mehta">
                </div>
                <div class="field">
                  <label>Email</label>
                  <input value="rahul.mehta@email.com">
                </div>
                <div class="field">
                  <label>Phone</label>
                  <input value="+91 98xxxxx112">
                </div>
                <div class="field">
                  <label>Location</label>
                  <input value="Pune, Maharashtra">
                </div>
              </div>
              <button class="btn-primary-sm" style="margin-top:16px;" (click)="state.showToast('Account details updated')">
                Save changes
              </button>
            </div>
          </div>

          <!-- Privacy -->
          <div class="csettings-pane active" *ngIf="activeTab() === 'privacy'">
            <div class="card">
              <div class="card-head"><h2>Profile visibility</h2></div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Visible to recruiters</div>
                  <div class="t-sub">Let companies discover your profile in search</div>
                </div>
                <div class="switch" [class.on]="recruiterVisible()" (click)="recruiterVisible.set(!recruiterVisible())"></div>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Hide current employer</div>
                  <div class="t-sub">Blur current company name from recruiters</div>
                </div>
                <div class="switch" [class.on]="hideEmployer()" (click)="hideEmployer.set(!hideEmployer())"></div>
              </div>
            </div>
          </div>

          <!-- Notifications -->
          <div class="csettings-pane active" *ngIf="activeTab() === 'notifications'">
            <div class="card">
              <div class="card-head"><h2>Notification preferences</h2></div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Application status updates</div>
                  <div class="t-sub">Email + in-app when a stage changes</div>
                </div>
                <div class="switch" [class.on]="notifAppStatus()" (click)="togglePreference('appStatus')"></div>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Interview reminders</div>
                  <div class="t-sub">1 hour before every scheduled interview</div>
                </div>
                <div class="switch" [class.on]="notifReminders()" (click)="togglePreference('reminders')"></div>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Recommended jobs</div>
                  <div class="t-sub">Weekly digest of AI-matched openings</div>
                </div>
                <div class="switch" [class.on]="notifRecJobs()" (click)="togglePreference('recJobs')"></div>
              </div>
              <div class="toggle-row">
                <div>
                  <div class="t-name">Marketing emails</div>
                  <div class="t-sub">Product updates and tips</div>
                </div>
                <div class="switch" [class.on]="notifMarketing()" (click)="togglePreference('marketing')"></div>
              </div>
            </div>
          </div>

          <!-- Password & security -->
          <div class="csettings-pane active" *ngIf="activeTab() === 'security'">
            <div class="card">
              <div class="card-head"><h2>Change password</h2></div>
              <div class="form-grid">
                <div class="field">
                  <label>Current password</label>
                  <input type="password" placeholder="••••••••">
                </div>
                <div class="field"></div>
                <div class="field">
                  <label>New password</label>
                  <input type="password" placeholder="••••••••">
                </div>
                <div class="field">
                  <label>Confirm new password</label>
                  <input type="password" placeholder="••••••••">
                </div>
              </div>
              <button class="btn-primary-sm" style="margin-top:16px;" (click)="state.showToast('Password updated')">
                Update password
              </button>
            </div>
          </div>

          <!-- Delete account -->
          <div class="csettings-pane active" *ngIf="activeTab() === 'danger'">
            <div class="card">
              <div class="card-head"><h2>Delete account</h2></div>
              <p style="font-size:12.5px; color:var(--ink-soft); margin-bottom:14px;">
                This permanently removes your profile, resume and application history. This cannot be undone.
              </p>
              <button class="btn-ghost" style="border-color:var(--danger); color:var(--danger);" (click)="deleteAccount()">
                Delete my account
              </button>
            </div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateSettingsComponent implements OnInit {
  activeTab = signal<string>('profile');

  // Reactive state switches matching prototype
  recruiterVisible = signal<boolean>(true);
  hideEmployer = signal<boolean>(false);

  notifAppStatus = signal<boolean>(true);
  notifReminders = signal<boolean>(true);
  notifRecJobs = signal<boolean>(true);
  notifMarketing = signal<boolean>(false);

  constructor(public state: AppStateService) {
    effect(() => this.syncFromPreferences());
  }

  ngOnInit() {
    this.state.loadNotificationPreferences();
  }

  private syncFromPreferences() {
    for (const pref of this.state.notificationPreferences()) {
      if (pref.category === PreferenceCategory.APPLICATION_STATUS_UPDATES) this.notifAppStatus.set(pref.enabled);
      if (pref.category === PreferenceCategory.INTERVIEW_REMINDERS) this.notifReminders.set(pref.enabled);
      if (pref.category === PreferenceCategory.RECOMMENDED_JOBS) this.notifRecJobs.set(pref.enabled);
      if (pref.category === PreferenceCategory.MARKETING_EMAILS) this.notifMarketing.set(pref.enabled);
    }
  }

  togglePreference(which: 'appStatus' | 'reminders' | 'recJobs' | 'marketing') {
    if (which === 'appStatus') this.notifAppStatus.set(!this.notifAppStatus());
    if (which === 'reminders') this.notifReminders.set(!this.notifReminders());
    if (which === 'recJobs') this.notifRecJobs.set(!this.notifRecJobs());
    if (which === 'marketing') this.notifMarketing.set(!this.notifMarketing());

    this.state.updateNotificationPreferences([
      { category: PreferenceCategory.APPLICATION_STATUS_UPDATES, enabled: this.notifAppStatus() },
      { category: PreferenceCategory.INTERVIEW_REMINDERS, enabled: this.notifReminders() },
      { category: PreferenceCategory.RECOMMENDED_JOBS, enabled: this.notifRecJobs() },
      { category: PreferenceCategory.MARKETING_EMAILS, enabled: this.notifMarketing() },
    ]);
  }

  deleteAccount() {
    if (confirm('Permanently delete your Cadence account?')) {
      this.state.showToast('Account deletion requested');
    }
  }
}
