import { Component, OnInit, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppStateService } from '../../core/services/app-state.service';
import { EmploymentType, OfferStatus, SendMode } from '../../core/models/offer-management.model';

const BENEFIT_OPTIONS = ['Health insurance', 'Provident fund', 'Relocation assistance', 'Remote work stipend'];

@Component({
  selector: 'app-offers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="section active" id="sec-offers">
      <div class="page-head">
        <div><h1>Offers</h1><p>Draft, approve and track offer letters across every open role.</p></div>
        <div class="page-head-actions">
          <button class="btn-primary-sm" (click)="openCreateWizard()">
            <svg viewBox="0 0 24 24"><path d="M12 5v14M5 12h14" stroke="currentColor" stroke-width="2.2" fill="none"/></svg>
            Create offer
          </button>
        </div>
      </div>

      <div class="kpi-row">
        <div class="kpi-card"><div class="kpi-label">Offers sent</div><div class="kpi-value">{{ stats()?.offersSent ?? '—' }}</div><div class="kpi-trend"><span class="muted">all time</span></div></div>
        <div class="kpi-card"><div class="kpi-label">Acceptance rate</div><div class="kpi-value">{{ stats()?.acceptanceRatePercent != null ? (stats()!.acceptanceRatePercent | number:'1.0-0') + '%' : '—' }}</div><div class="kpi-trend"><span class="muted">last 90 days</span></div></div>
        <div class="kpi-card"><div class="kpi-label">Avg. time to accept</div><div class="kpi-value">{{ stats()?.avgTimeToAcceptDays != null ? (stats()!.avgTimeToAcceptDays | number:'1.1-1') + 'd' : '—' }}</div><div class="kpi-trend"><span class="muted">from sent to response</span></div></div>
        <div class="kpi-card"><div class="kpi-label">Pending approval</div><div class="kpi-value">{{ stats()?.pendingApprovalCount ?? '—' }}</div><div class="kpi-trend"><span class="muted">needs sign-off</span></div></div>
      </div>

      <div class="card">
        <div class="filter-row">
          <div class="filter-tabs">
            <button [class.active]="activeFilter() === 'all'" (click)="setFilter('all')">All {{ offers().length }}</button>
            <button [class.active]="activeFilter() === 'PENDING_APPROVAL'" (click)="setFilter(OfferStatusEnum.PENDING_APPROVAL)">Pending approval</button>
            <button [class.active]="activeFilter() === 'SENT'" (click)="setFilter(OfferStatusEnum.SENT)">Sent</button>
            <button [class.active]="activeFilter() === 'ACCEPTED'" (click)="setFilter(OfferStatusEnum.ACCEPTED)">Accepted</button>
            <button [class.active]="activeFilter() === 'DECLINED'" (click)="setFilter(OfferStatusEnum.DECLINED)">Declined</button>
          </div>
        </div>

        <table class="table" *ngIf="!state.offersLoading(); else loadingRows">
          <thead><tr><th>Candidate</th><th>Job</th><th>CTC</th><th>Status</th><th>Updated</th><th></th></tr></thead>
          <tbody>
            <tr *ngFor="let o of offers()">
              <td><div class="cand-cell"><div class="avatar">{{ initials(o.candidateName) }}</div><div><div class="name">{{ o.candidateName }}</div><div class="email">{{ o.candidateEmail }}</div></div></div></td>
              <td class="muted">{{ o.jobTitle }}</td>
              <td class="muted">₹{{ o.totalCtc }} LPA</td>
              <td><span class="badge" [ngClass]="statusBadgeClass(o.status)">{{ statusLabel(o.status) }}</span></td>
              <td class="muted">{{ o.updatedAt | date:'MMM d, y' }}</td>
              <td><span class="row-link" (click)="openDetail(o.id)">View</span></td>
            </tr>
          </tbody>
        </table>
        <ng-template #loadingRows>
          <div class="skeleton-card" style="margin-bottom:10px;" *ngFor="let i of [1,2,3]">
            <div class="skeleton sk-title"></div>
            <div class="skeleton sk-sub"></div>
          </div>
        </ng-template>
        <div class="empty-state" *ngIf="!state.offersLoading() && !offers().length">
          <svg viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="M21 21l-4-4"/></svg>
          <p>No offers match this filter.</p>
          <button (click)="setFilter('all')">Clear filter</button>
        </div>
      </div>
    </section>

    <!-- MODAL: CREATE OFFER (WIZARD) -->
    <div class="modal-overlay" [class.show]="state.activeModal() === 'offer'" (click)="state.closeModal()">
      <div class="modal wide" (click)="$event.stopPropagation()">
        <div class="modal-head">
          <div><h3>Create offer</h3><p>Step {{ wizardStep() }} of 4</p></div>
          <button class="modal-close" aria-label="Close dialog" (click)="state.closeModal()"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></button>
        </div>
        <div class="modal-body">
          <div class="wizard-steps">
            <div class="wstep" [class.active]="wizardStep() === 1" [class.done]="wizardStep() > 1">1 · Candidate &amp; role</div>
            <div class="wstep" [class.active]="wizardStep() === 2" [class.done]="wizardStep() > 2">2 · Compensation</div>
            <div class="wstep" [class.active]="wizardStep() === 3" [class.done]="wizardStep() > 3">3 · Approval &amp; letter</div>
            <div class="wstep" [class.active]="wizardStep() === 4">4 · Review &amp; send</div>
          </div>

          <!-- STEP 1 -->
          <div class="wizard-panel active" *ngIf="wizardStep() === 1">
            <div class="field-full">
              <label>Candidate</label>
              <select [ngModel]="selectedApplicationId()" (ngModelChange)="selectedApplicationId.set($event)">
                <option [ngValue]="null" disabled>Select a candidate…</option>
                <option *ngFor="let app of state.companyApplications()" [ngValue]="app.id">{{ app.candidateNameSnapshot }} — {{ app.jobTitleSnapshot }}</option>
              </select>
            </div>
            <div class="form-row-2">
              <div class="field-full"><label>Job position</label><input [value]="selectedApp()?.jobTitleSnapshot || '—'" disabled></div>
              <div class="field-full"><label>Department</label><input [ngModel]="department()" (ngModelChange)="department.set($event)" placeholder="e.g. Engineering"></div>
            </div>
            <div class="form-row-2">
              <div class="field-full"><label>Employment type</label>
                <select [ngModel]="employmentType()" (ngModelChange)="employmentType.set($event)">
                  <option [ngValue]="EmploymentTypeEnum.FULL_TIME">Full-time</option>
                  <option [ngValue]="EmploymentTypeEnum.CONTRACT">Contract</option>
                </select>
              </div>
              <div class="field-full"><label>Proposed start date</label><input type="date" [ngModel]="startDate()" (ngModelChange)="startDate.set($event)"></div>
            </div>
          </div>

          <!-- STEP 2 -->
          <div class="wizard-panel active" *ngIf="wizardStep() === 2">
            <div class="form-row-2">
              <div class="field-full"><label>Base salary (₹ LPA)</label><input type="number" [ngModel]="baseSalary()" (ngModelChange)="baseSalary.set($event)"></div>
              <div class="field-full"><label>Variable / bonus (₹ LPA)</label><input type="number" [ngModel]="variableBonus()" (ngModelChange)="variableBonus.set($event)"></div>
            </div>
            <div class="form-row-2">
              <div class="field-full"><label>ESOP / equity (₹ LPA equiv.)</label><input type="number" [ngModel]="esopEquity()" (ngModelChange)="esopEquity.set($event)"></div>
              <div class="field-full"><label>Total CTC (₹ LPA)</label><input type="number" [value]="totalCtc()" disabled></div>
            </div>
            <div class="field-full">
              <label>Benefits included</label>
              <div class="chip-group">
                <span class="select-chip" *ngFor="let b of benefitOptions" [class.on]="selectedBenefits().has(b)" (click)="toggleBenefit(b)">{{ b }}</span>
              </div>
            </div>
          </div>

          <!-- STEP 3 -->
          <div class="wizard-panel active" *ngIf="wizardStep() === 3">
            <!-- No interviewer/team-member directory endpoint exists anywhere on the platform (same gap
                 as the recruiter interview-panel picker), so the approver defaults to the recruiter
                 creating the offer rather than a fabricated team list. -->
            <div class="field-full">
              <label>Approver</label>
              <div class="chip-group"><span class="select-chip on">You ({{ state.currentUser()?.name || 'Recruiter' }})</span></div>
            </div>
            <div class="field-full"><label>Offer expiry date</label><input type="date" [ngModel]="expiryDate()" (ngModelChange)="expiryDate.set($event)"></div>
            <div class="field-full">
              <label>Offer letter</label>
              <div class="template-card" style="cursor:default;">
                <div class="template-card-top"><div class="template-icon"><svg viewBox="0 0 24 24"><rect x="3" y="5" width="18" height="14" rx="2"/><path d="M3 7l9 6 9-6"/></svg></div><div><div class="template-card-name">Offer Letter — Standard</div><div class="template-card-trigger">Generated after the offer is created, from role, compensation &amp; start date</div></div></div>
              </div>
            </div>
          </div>

          <!-- STEP 4 -->
          <div class="wizard-panel active" *ngIf="wizardStep() === 4">
            <div class="review-block">
              <div class="review-row"><span>Candidate</span><span>{{ selectedApp()?.candidateNameSnapshot || '—' }}</span></div>
              <div class="review-row"><span>Job position</span><span>{{ selectedApp()?.jobTitleSnapshot || '—' }}</span></div>
              <div class="review-row"><span>Start date</span><span>{{ startDate() || 'Not set' }}</span></div>
              <div class="review-row"><span>Total CTC</span><span>₹{{ totalCtc() }} LPA</span></div>
              <div class="review-row"><span>Approver</span><span>{{ state.currentUser()?.name || 'You' }}</span></div>
              <div class="review-row"><span>Offer expiry</span><span>{{ expiryDate() || 'Not set' }}</span></div>
            </div>
            <div class="radio-card-row">
              <div class="radio-card" [class.selected]="sendMode() === SendModeEnum.DRAFT" (click)="sendMode.set(SendModeEnum.DRAFT)"><div class="rc-title">Save as draft</div><div class="rc-sub">Finish later</div></div>
              <div class="radio-card" [class.selected]="sendMode() === SendModeEnum.APPROVAL" (click)="sendMode.set(SendModeEnum.APPROVAL)"><div class="rc-title">Send for approval</div><div class="rc-sub">Route to approver first</div></div>
              <div class="radio-card" [class.selected]="sendMode() === SendModeEnum.SEND" (click)="sendMode.set(SendModeEnum.SEND)"><div class="rc-title">Send to candidate</div><div class="rc-sub">Skip approval, send now</div></div>
            </div>
          </div>
        </div>
        <div class="modal-foot">
          <button class="btn-text" [style.visibility]="wizardStep() === 1 ? 'hidden' : 'visible'" (click)="wizardBack()">Back</button>
          <div class="foot-right">
            <button class="btn-ghost" (click)="state.closeModal()">Cancel</button>
            <button class="btn-primary-sm" [disabled]="wizardStep() === 1 && !selectedApplicationId()" (click)="wizardNext()">{{ wizardStep() === 4 ? 'Confirm' : 'Continue' }}</button>
          </div>
        </div>
      </div>
    </div>

    <!-- DRAWER: OFFER DETAIL -->
    <div class="drawer-overlay" [ngClass]="{ show: state.offerDetail() }" (click)="closeDetail()">
      <div class="drawer" (click)="$event.stopPropagation()" *ngIf="state.offerDetail() as d">
        <div class="drawer-head">
          <div><h3>{{ d.candidateName }}</h3><p>{{ d.jobTitle }} · Offer</p></div>
          <button class="modal-close" aria-label="Close panel" (click)="closeDetail()"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></button>
        </div>
        <div class="drawer-body">
          <section style="display:flex; align-items:center; justify-content:space-between;">
            <div style="font-size:12.5px; color:var(--ink-soft);">Start date: <b style="color:var(--ink);">{{ d.startDate | date:'MMM d, y' }}</b></div>
            <span class="badge" [ngClass]="statusBadgeClass(d.status)">{{ statusLabel(d.status) }}</span>
          </section>
          <section>
            <div class="drawer-section-title">Compensation</div>
            <div class="review-block">
              <div class="review-row"><span>Base salary</span><span>₹{{ d.baseSalary }} LPA</span></div>
              <div class="review-row"><span>Variable / bonus</span><span>₹{{ d.variableBonus ?? 0 }} LPA</span></div>
              <div class="review-row"><span>ESOP / equity</span><span>₹{{ d.esopEquity ?? 0 }} LPA</span></div>
              <div class="review-row"><span>Total CTC</span><span><b>₹{{ d.totalCtc }} LPA</b></span></div>
              <div class="review-row"><span>Offer expiry</span><span>{{ d.expiryDate ? (d.expiryDate | date:'MMM d, y') : '—' }}</span></div>
            </div>
          </section>
          <section *ngIf="d.negotiations.length">
            <div class="drawer-section-title">Negotiation requests</div>
            <div class="review-block" *ngFor="let n of d.negotiations">
              <div class="review-row"><span>Proposed CTC</span><span>{{ n.proposedCtc != null ? '₹' + n.proposedCtc + ' LPA' : '—' }}</span></div>
              <div class="review-row"><span>Message</span><span>{{ n.message || '—' }}</span></div>
              <div class="review-row"><span>Status</span><span>{{ n.status }}</span></div>
            </div>
          </section>
          <section>
            <div class="drawer-section-title">Approval timeline</div>
            <div class="timeline">
              <div class="tl-track"></div>
              <div class="tl-item" *ngFor="let t of d.timeline">
                <div class="tl-node done"><svg viewBox="0 0 24 24" style="width:12px;height:12px;stroke:white;stroke-width:3;fill:none;"><path d="M5 13l4 4L19 7"/></svg></div>
                <div class="tl-content">
                  <div class="tl-top"><span class="tl-title">{{ t.eventType }}</span><span class="tl-date">{{ t.occurredAt | date:'MMM d, y' }}</span></div>
                  <div class="tl-card" *ngIf="t.details"><span class="tl-note">{{ t.details }}</span></div>
                </div>
              </div>
              <div class="empty-state" *ngIf="!d.timeline.length"><p>No activity yet.</p></div>
            </div>
          </section>
        </div>
        <div class="drawer-foot">
          <button class="btn-ghost" (click)="downloadLetter(d.id)">Download letter</button>
          <div class="foot-right" [ngSwitch]="d.status">
            <ng-container *ngSwitchCase="'PENDING_APPROVAL'">
              <button class="btn-ghost" style="color:var(--danger);" (click)="approve(d.id, false)">Reject</button>
              <button class="btn-primary-sm" (click)="approve(d.id, true)">Approve &amp; send</button>
            </ng-container>
            <ng-container *ngSwitchCase="'SENT'">
              <button class="btn-ghost" style="color:var(--danger);" (click)="withdraw(d.id)">Withdraw</button>
              <button class="btn-primary-sm" (click)="resend(d.id)">Resend reminder</button>
            </ng-container>
            <ng-container *ngSwitchCase="'DRAFT'">
              <button class="btn-ghost" style="color:var(--danger);" (click)="deleteDraft(d.id)">Delete draft</button>
            </ng-container>
            <ng-container *ngSwitchDefault>
              <button class="btn-primary-sm" (click)="closeDetail()">Close</button>
            </ng-container>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: []
})
export class OffersComponent implements OnInit {
  OfferStatusEnum = OfferStatus;
  EmploymentTypeEnum = EmploymentType;
  SendModeEnum = SendMode;
  benefitOptions = BENEFIT_OPTIONS;

  activeFilter = signal<OfferStatus | 'all'>('all');

  wizardStep = signal(1);
  selectedApplicationId = signal<string | null>(null);
  department = signal('');
  employmentType = signal<EmploymentType>(EmploymentType.FULL_TIME);
  startDate = signal('');
  baseSalary = signal(0);
  variableBonus = signal(0);
  esopEquity = signal(0);
  selectedBenefits = signal<Set<string>>(new Set());
  expiryDate = signal('');
  sendMode = signal<SendMode>(SendMode.DRAFT);

  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadCompanyApplications();
    this.state.loadOfferStats();
    this.loadForFilter();
  }

  offers = computed(() => this.state.offersList());
  stats = computed(() => this.state.offerStats());

  selectedApp = computed(() =>
    this.state.companyApplications().find((a) => a.id === this.selectedApplicationId()) || null
  );

  totalCtc = computed(() => {
    const total = (this.baseSalary() || 0) + (this.variableBonus() || 0) + (this.esopEquity() || 0);
    return Math.round(total * 10) / 10;
  });

  setFilter(filter: OfferStatus | 'all') {
    this.activeFilter.set(filter);
    this.loadForFilter();
  }

  private loadForFilter() {
    const f = this.activeFilter();
    this.state.loadOffers(f === 'all' ? undefined : f);
  }

  openCreateWizard() {
    this.wizardStep.set(1);
    this.selectedApplicationId.set(null);
    this.department.set('');
    this.employmentType.set(EmploymentType.FULL_TIME);
    this.startDate.set('');
    this.baseSalary.set(0);
    this.variableBonus.set(0);
    this.esopEquity.set(0);
    this.selectedBenefits.set(new Set());
    this.expiryDate.set('');
    this.sendMode.set(SendMode.DRAFT);
    this.state.openModal('offer');
  }

  toggleBenefit(b: string) {
    const set = new Set(this.selectedBenefits());
    if (set.has(b)) set.delete(b);
    else set.add(b);
    this.selectedBenefits.set(set);
  }

  wizardBack() {
    if (this.wizardStep() > 1) this.wizardStep.update((s) => s - 1);
  }

  wizardNext() {
    if (this.wizardStep() === 1 && !this.selectedApplicationId()) return;
    if (this.wizardStep() < 4) {
      this.wizardStep.update((s) => s + 1);
      return;
    }
    const app = this.selectedApp();
    if (!app) return;
    this.state.createOffer({
      applicationId: app.id,
      jobId: app.jobId,
      candidateId: app.candidateId,
      department: this.department() || undefined,
      employmentType: this.employmentType(),
      startDate: this.startDate(),
      baseSalary: this.baseSalary(),
      variableBonus: this.variableBonus() || undefined,
      esopEquity: this.esopEquity() || undefined,
      benefits: Array.from(this.selectedBenefits()),
      expiryDate: this.expiryDate() || undefined,
      sendMode: this.sendMode(),
    });
  }

  openDetail(id: string) {
    this.state.loadOfferDetail(id);
  }

  closeDetail() {
    this.state.clearOfferDetail();
  }

  approve(id: string, approve: boolean) {
    this.state.approveOffer(id, approve);
  }

  resend(id: string) {
    this.state.sendOffer(id);
  }

  withdraw(id: string) {
    this.state.withdrawOffer(id);
  }

  deleteDraft(id: string) {
    if (confirm('Delete this draft offer? This cannot be undone.')) {
      this.state.deleteDraftOffer(id);
      this.closeDetail();
    }
  }

  downloadLetter(id: string) {
    this.state.downloadOfferLetter(id);
  }

  initials(name: string): string {
    return name.split(' ').map((n) => n[0]).join('').toUpperCase().substring(0, 2);
  }

  statusLabel(status: OfferStatus): string {
    switch (status) {
      case OfferStatus.PENDING_APPROVAL: return 'Pending approval';
      case OfferStatus.SENT: return 'Sent — awaiting response';
      case OfferStatus.ACCEPTED: return 'Accepted';
      case OfferStatus.DECLINED: return 'Declined';
      case OfferStatus.WITHDRAWN: return 'Withdrawn';
      case OfferStatus.EXPIRED: return 'Expired';
      default: return 'Draft';
    }
  }

  statusBadgeClass(status: OfferStatus): string {
    switch (status) {
      case OfferStatus.PENDING_APPROVAL: return 'draft';
      case OfferStatus.SENT: return 'processing';
      case OfferStatus.ACCEPTED: return 'published';
      case OfferStatus.DECLINED: return 'failed';
      case OfferStatus.WITHDRAWN: return 'archived';
      case OfferStatus.EXPIRED: return 'archived';
      default: return 'draft';
    }
  }
}
