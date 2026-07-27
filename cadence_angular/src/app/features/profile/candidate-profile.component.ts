import { Component, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';

@Component({
  selector: 'app-candidate-profile',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-profile">
      <div class="page-head">
        <div>
          <h1>My profile</h1>
          <p>This is what recruiters see when you apply.</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-primary-sm" (click)="state.openModal('profile-wizard')">Edit profile</button>
        </div>
      </div>
      
      <div class="row-2">
        <!-- Main details card -->
        <div class="card">
          <div class="cd-header">
            <div class="avatar-xl">{{ initials() }}</div>
            <div>
              <div class="cd-name">{{ profile()?.fullName ?? '—' }}</div>
              <div class="cd-sub">{{ profile()?.headline ?? 'No headline yet' }}</div>
              <div class="cd-meta-row">
                <span>{{ profile()?.email ?? '—' }}</span>
                <span>{{ profile()?.phone ?? '—' }}</span>
                <span>Current: <b>{{ profile()?.currentCompany ?? '—' }}</b></span>
                <span>Notice period: <b>{{ profile()?.noticePeriodDays ? profile()!.noticePeriodDays + ' days' : '—' }}</b></span>
              </div>
            </div>
          </div>

          <div class="chip-group" style="margin-top:16px;">
            <span class="skill-pill" *ngFor="let skill of profile()?.skills ?? []">{{ skill }}</span>
          </div>

          <div class="card-head" style="margin-top:20px;">
            <h2>Experience</h2>
          </div>
          <div class="list-card" style="max-height:none;">
            <div class="interview-item" *ngFor="let exp of profile()?.experience ?? []">
              <div class="avatar">{{ (exp.companyName || '??').slice(0, 2).toUpperCase() }}</div>
              <div class="meta">
                <div class="who">{{ exp.jobTitle }}</div>
                <div class="role">{{ exp.companyName }} · {{ exp.startDate ?? '—' }} - {{ exp.currentlyWorking ? 'Present' : (exp.endDate ?? '—') }}</div>
              </div>
            </div>
            <div class="muted" style="font-size:12.5px;" *ngIf="!(profile()?.experience?.length)">No experience added yet.</div>
          </div>
        </div>

        <!-- Profile Health card -->
        <div class="card">
          <div class="card-head">
            <h2>Profile health</h2>
          </div>
          <div style="display:flex; align-items:center; gap:16px; margin-bottom:14px;">
            <div class="profile-ring">
              <svg viewBox="0 0 64 64">
                <circle class="ring-bg" cx="32" cy="32" r="28"/>
                <circle class="ring-fg" cx="32" cy="32" r="28" stroke-dasharray="175.9" [attr.stroke-dashoffset]="ringOffset()"/>
              </svg>
              <div class="ring-label">{{ profile()?.profileCompletionPercent ?? 0 }}%</div>
            </div>
            <div style="font-size:12px; color:var(--ink-soft);">Add certifications and a portfolio link to reach 100%.</div>
          </div>

          <div class="toggle-row">
            <div>
              <div class="t-name">Basic info</div>
              <div class="t-sub">Name, location, contact</div>
            </div>
            <span class="badge" [ngClass]="profile()?.fullName ? 'published' : 'draft'">{{ profile()?.fullName ? 'Done' : 'Pending' }}</span>
          </div>
          <div class="toggle-row">
            <div>
              <div class="t-name">Resume</div>
              <div class="t-sub">{{ profile()?.resumeFilename ?? 'Not uploaded yet' }}</div>
            </div>
            <span class="badge" [ngClass]="profile()?.resumeUrl ? 'published' : 'draft'">{{ profile()?.resumeUrl ? 'Done' : 'Pending' }}</span>
          </div>
          <div class="toggle-row">
            <div>
              <div class="t-name">Education</div>
              <div class="t-sub">{{ profile()?.education?.[0]?.degree ?? 'Not added yet' }}</div>
            </div>
            <span class="badge" [ngClass]="profile()?.education?.length ? 'published' : 'draft'">{{ profile()?.education?.length ? 'Done' : 'Pending' }}</span>
          </div>
          <div class="toggle-row">
            <div>
              <div class="t-name">Experience</div>
              <div class="t-sub">{{ profile()?.experience?.length ?? 0 }} roles added</div>
            </div>
            <span class="badge" [ngClass]="profile()?.experience?.length ? 'published' : 'draft'">{{ profile()?.experience?.length ? 'Done' : 'Pending' }}</span>
          </div>
          <div class="toggle-row">
            <div>
              <div class="t-name">Skills</div>
              <div class="t-sub">{{ profile()?.skills?.length ?? 0 }} skills added</div>
            </div>
            <span class="badge" [ngClass]="profile()?.skills?.length ? 'published' : 'draft'">{{ profile()?.skills?.length ? 'Done' : 'Pending' }}</span>
          </div>
          <div class="toggle-row">
            <div>
              <div class="t-name">Certifications</div>
              <div class="t-sub">{{ profile()?.certifications?.length ? profile()!.certifications.length + ' added' : 'Not added yet' }}</div>
            </div>
            <span class="badge" [ngClass]="profile()?.certifications?.length ? 'published' : 'draft'">{{ profile()?.certifications?.length ? 'Done' : 'Pending' }}</span>
          </div>
          <div class="toggle-row">
            <div>
              <div class="t-name">Portfolio</div>
              <div class="t-sub">{{ profile()?.portfolio?.githubUrl || profile()?.portfolio?.linkedinUrl ? 'Added' : 'Not added yet' }}</div>
            </div>
            <span class="badge" [ngClass]="(profile()?.portfolio?.githubUrl || profile()?.portfolio?.linkedinUrl) ? 'published' : 'draft'">{{ (profile()?.portfolio?.githubUrl || profile()?.portfolio?.linkedinUrl) ? 'Done' : 'Pending' }}</span>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateProfileComponent {
  constructor(public state: AppStateService) {
    this.state.loadCandidateProfile();
  }

  profile = computed(() => this.state.candidateProfile());

  initials = computed(() => {
    const name = this.profile()?.fullName ?? '';
    return name
      .split(' ')
      .filter(Boolean)
      .slice(0, 2)
      .map((part) => part[0]?.toUpperCase())
      .join('') || '—';
  });

  /** Ring circumference (r=28) is ~175.9; offset = circumference * (1 - percent/100). */
  ringOffset = computed(() => {
    const pct = this.profile()?.profileCompletionPercent ?? 0;
    return (175.9 * (1 - pct / 100)).toFixed(1);
  });
}
