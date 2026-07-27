import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppStateService } from '../../core/services/app-state.service';
import { CandidateOfferResponse, DeclineReason, OfferStatus } from '../../core/models/offer-management.model';

@Component({
  selector: 'app-candidate-offers',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="section csection active" id="csec-offers">
      <div class="page-head"><div><h1>{{ state.myOffers().length > 1 ? 'My offers' : 'My offer' }}</h1><p>Review the details below before you respond.</p></div></div>

      <div class="card" *ngIf="state.myOffersLoading()">
        <div class="skeleton-card"><div class="skeleton sk-title"></div><div class="skeleton sk-sub"></div></div>
      </div>

      <div class="empty-state" *ngIf="!state.myOffersLoading() && !state.myOffers().length">
        <svg viewBox="0 0 24 24"><path d="M20 12v6a2 2 0 01-2 2H6a2 2 0 01-2-2v-6"/><path d="M2 7h20v5H2z"/></svg>
        <p>You don't have any offers yet.</p>
      </div>

      <div class="card" style="margin-bottom:16px;" *ngFor="let offer of state.myOffers()">
        <section style="display:flex; align-items:center; gap:14px; margin-bottom:18px;">
          <div class="mark" style="width:44px; height:44px; font-size:16px; flex-shrink:0;">{{ offer.companyName.charAt(0) }}</div>
          <div style="flex:1;">
            <div style="font-family:'Fraunces',serif; font-weight:600; font-size:16px;">{{ offer.jobTitle }}</div>
            <div style="font-size:12.5px; color:var(--ink-soft); margin-top:2px;">{{ offer.companyName }}</div>
          </div>
          <span class="badge" [ngClass]="statusBadgeClass(offer.status)">{{ statusLabel(offer.status) }}</span>
        </section>

        <div class="review-block" style="margin-bottom:18px;">
          <div class="review-row"><span>Base salary</span><span>₹{{ offer.baseSalary }} LPA</span></div>
          <div class="review-row"><span>Variable / bonus</span><span>₹{{ offer.variableBonus ?? 0 }} LPA</span></div>
          <div class="review-row"><span>ESOP / equity</span><span>₹{{ offer.esopEquity ?? 0 }} LPA</span></div>
          <div class="review-row"><span>Total CTC</span><span><b>₹{{ offer.totalCtc }} LPA</b></span></div>
          <div class="review-row"><span>Proposed start date</span><span>{{ offer.startDate | date:'MMM d, y' }}</span></div>
          <div class="review-row" *ngIf="offer.expiryDate"><span>Offer expires</span><span>{{ offer.expiryDate | date:'MMM d, y' }} <span class="muted" *ngIf="offer.daysUntilExpiry != null">({{ offer.daysUntilExpiry }} days left)</span></span></div>
        </div>

        <div style="display:flex; gap:10px; flex-wrap:wrap;" *ngIf="offer.status === OfferStatusEnum.SENT">
          <button class="btn-primary-sm" (click)="openAccept(offer)">Accept offer</button>
          <button class="btn-ghost" style="color:var(--danger);" (click)="openDecline(offer)">Decline offer</button>
          <button class="btn-ghost" (click)="openNegotiate(offer)">Request negotiation</button>
          <button class="btn-ghost" (click)="download(offer.id)">Download offer letter</button>
        </div>
        <div style="display:flex; gap:10px; flex-wrap:wrap;" *ngIf="offer.status !== OfferStatusEnum.SENT">
          <button class="btn-ghost" (click)="download(offer.id)">Download offer letter</button>
        </div>

        <div style="margin-top:18px;" *ngIf="offer.status === OfferStatusEnum.ACCEPTED">
          <div class="recommendation-banner proceed">
            <svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg>
            <div><b>You've accepted this offer</b><p>Welcome to {{ offer.companyName }}! Your onboarding team will reach out with next steps before your start date.</p></div>
          </div>
        </div>
        <div style="margin-top:18px;" *ngIf="offer.status === OfferStatusEnum.DECLINED">
          <div class="recommendation-banner reject">
            <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            <div><b>You've declined this offer</b><p>Thanks for letting us know — we wish you the best.</p></div>
          </div>
        </div>
      </div>
    </section>

    <!-- MODAL: ACCEPT OFFER -->
    <div class="modal-overlay" [class.show]="acceptTarget()" (click)="acceptTarget.set(null)">
      <div class="modal" (click)="$event.stopPropagation()" *ngIf="acceptTarget() as offer">
        <div class="modal-head"><div><h3>Accept this offer?</h3><p>You'll digitally sign and confirm your start date.</p></div><button class="modal-close" aria-label="Close dialog" (click)="acceptTarget.set(null)"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></button></div>
        <div class="modal-body">
          <div class="review-block">
            <div class="review-row"><span>Role</span><span>{{ offer.jobTitle }}</span></div>
            <div class="review-row"><span>Total CTC</span><span>₹{{ offer.totalCtc }} LPA</span></div>
            <div class="review-row"><span>Start date</span><span>{{ offer.startDate | date:'MMM d, y' }}</span></div>
          </div>
          <label class="agree-checkbox-row" style="margin-top:14px;">
            <input type="checkbox" [ngModel]="acceptAgreed()" (ngModelChange)="acceptAgreed.set($event)">
            <span>I confirm the details above are correct and accept this offer.</span>
          </label>
        </div>
        <div class="modal-foot"><span></span><div class="foot-right"><button class="btn-ghost" (click)="acceptTarget.set(null)">Cancel</button><button class="btn-primary-sm" [disabled]="!acceptAgreed()" (click)="confirmAccept()">Accept offer</button></div></div>
      </div>
    </div>

    <!-- MODAL: DECLINE OFFER -->
    <div class="modal-overlay" [class.show]="declineTarget()" (click)="declineTarget.set(null)">
      <div class="modal" (click)="$event.stopPropagation()" *ngIf="declineTarget()">
        <div class="modal-head"><div><h3>Decline this offer?</h3><p>This can't be undone — let the team know why.</p></div><button class="modal-close" aria-label="Close dialog" (click)="declineTarget.set(null)"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></button></div>
        <div class="modal-body">
          <div class="field-full">
            <label>Reason (optional)</label>
            <select [ngModel]="declineReason()" (ngModelChange)="declineReason.set($event)">
              <option [ngValue]="DeclineReasonEnum.OTHER_OFFER_ACCEPTED">Accepted a different offer</option>
              <option [ngValue]="DeclineReasonEnum.COMPENSATION_MISMATCH">Compensation doesn't meet expectations</option>
              <option [ngValue]="DeclineReasonEnum.ROLE_NOT_FIT">Role isn't the right fit</option>
              <option [ngValue]="DeclineReasonEnum.PERSONAL_REASONS">Personal reasons</option>
              <option [ngValue]="DeclineReasonEnum.OTHER">Other</option>
            </select>
          </div>
        </div>
        <div class="modal-foot"><span></span><div class="foot-right"><button class="btn-ghost" (click)="declineTarget.set(null)">Cancel</button><button class="btn-primary-sm" style="background:var(--danger);" (click)="confirmDecline()">Decline offer</button></div></div>
      </div>
    </div>

    <!-- MODAL: REQUEST NEGOTIATION (no Figma coverage for this action -- built in the same
         visual language since the backend's request-negotiation endpoint is a required
         candidate capability with no existing screen to integrate into). -->
    <div class="modal-overlay" [class.show]="negotiateTarget()" (click)="negotiateTarget.set(null)">
      <div class="modal" (click)="$event.stopPropagation()" *ngIf="negotiateTarget()">
        <div class="modal-head"><div><h3>Request a negotiation</h3><p>Propose a different package — the recruiter will follow up.</p></div><button class="modal-close" aria-label="Close dialog" (click)="negotiateTarget.set(null)"><svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg></button></div>
        <div class="modal-body">
          <div class="field-full"><label>Proposed total CTC (₹ LPA)</label><input type="number" [ngModel]="negotiateCtc()" (ngModelChange)="negotiateCtc.set($event)"></div>
          <div class="field-full"><label>Message (optional)</label><textarea [ngModel]="negotiateMessage()" (ngModelChange)="negotiateMessage.set($event)" placeholder="Explain what you're looking for…"></textarea></div>
        </div>
        <div class="modal-foot"><span></span><div class="foot-right"><button class="btn-ghost" (click)="negotiateTarget.set(null)">Cancel</button><button class="btn-primary-sm" (click)="confirmNegotiate()">Send request</button></div></div>
      </div>
    </div>
  `,
  styles: []
})
export class CandidateOffersComponent implements OnInit {
  OfferStatusEnum = OfferStatus;
  DeclineReasonEnum = DeclineReason;

  acceptTarget = signal<CandidateOfferResponse | null>(null);
  acceptAgreed = signal(false);
  declineTarget = signal<CandidateOfferResponse | null>(null);
  declineReason = signal<DeclineReason>(DeclineReason.OTHER);
  negotiateTarget = signal<CandidateOfferResponse | null>(null);
  negotiateCtc = signal<number | null>(null);
  negotiateMessage = signal('');

  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadMyOffers();
  }

  openAccept(offer: CandidateOfferResponse) {
    this.acceptAgreed.set(false);
    this.acceptTarget.set(offer);
  }

  confirmAccept() {
    const offer = this.acceptTarget();
    if (!offer) return;
    this.state.acceptMyOffer(offer.id);
    this.acceptTarget.set(null);
  }

  openDecline(offer: CandidateOfferResponse) {
    this.declineReason.set(DeclineReason.OTHER);
    this.declineTarget.set(offer);
  }

  confirmDecline() {
    const offer = this.declineTarget();
    if (!offer) return;
    this.state.rejectMyOffer(offer.id, { reason: this.declineReason() });
    this.declineTarget.set(null);
  }

  openNegotiate(offer: CandidateOfferResponse) {
    this.negotiateCtc.set(offer.totalCtc);
    this.negotiateMessage.set('');
    this.negotiateTarget.set(offer);
  }

  confirmNegotiate() {
    const offer = this.negotiateTarget();
    if (!offer) return;
    this.state.requestMyOfferNegotiation(offer.id, {
      proposedCtc: this.negotiateCtc() ?? undefined,
      message: this.negotiateMessage() || undefined,
    });
    this.negotiateTarget.set(null);
  }

  download(offerId: string) {
    this.state.downloadMyOfferLetter(offerId);
  }

  statusLabel(status: OfferStatus): string {
    switch (status) {
      case OfferStatus.SENT: return 'Offer received';
      case OfferStatus.ACCEPTED: return 'Accepted';
      case OfferStatus.DECLINED: return 'Declined';
      case OfferStatus.WITHDRAWN: return 'Withdrawn';
      case OfferStatus.EXPIRED: return 'Expired';
      default: return 'Draft';
    }
  }

  statusBadgeClass(status: OfferStatus): string {
    switch (status) {
      case OfferStatus.SENT: return 'processing';
      case OfferStatus.ACCEPTED: return 'published';
      case OfferStatus.DECLINED: return 'archived';
      case OfferStatus.WITHDRAWN: return 'archived';
      case OfferStatus.EXPIRED: return 'archived';
      default: return 'draft';
    }
  }
}
