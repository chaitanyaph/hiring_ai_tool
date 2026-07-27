import { Component, OnInit, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { AppStateService } from '../../core/services/app-state.service';
import { TemplateResponse } from '../../core/models/notification.model';

@Component({
  selector: 'app-notifications',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <div class="notifications-viewport">
      <div class="page-head">
        <div>
          <h1>Recruiter Notifications</h1>
          <p>Configure automatic triggers, manage email templates, and view delivery history logs.</p>
        </div>
      </div>

      <div class="settings-layout">
        <div class="subnav">
          <button [class.active]="activeTab() === 'templates'" (click)="selectTab('templates')">Email templates</button>
          <button [class.active]="activeTab() === 'logs'" (click)="selectTab('logs')">Email logs</button>
          <button [class.active]="activeTab() === 'scheduled'" (click)="selectTab('scheduled')">Scheduled sends</button>
          <button [class.active]="activeTab() === 'failed'" (click)="selectTab('failed')">Failed sends</button>
        </div>

        <div>
          <!-- EMAIL TEMPLATES -->
          <div class="settings-pane active" *ngIf="activeTab() === 'templates'">
            <div class="rec-card-grid">
              <div class="rec-card-lg" *ngFor="let tmpl of state.emailTemplates()">
                <div class="rec-card-lg-top">
                  <div class="resume-icon">
                    <svg viewBox="0 0 24 24"><path d="M4 4h16c1.1 0 2 .9 2 2v12c0 1.1-.9 2-2 2H4c-1.1 0-2-.9-2-2V6c0-1.1.9-2 2-2z"/><path d="M22 6l-10 7L2 6"/></svg>
                  </div>
                  <div>
                    <div class="rcl-name">{{ tmpl.name }}</div>
                    <div class="rcl-role">Trigger: {{ tmpl.triggerEvent }}</div>
                  </div>
                </div>
                <div style="font-size:12.5px; color:var(--ink-soft); line-height:1.4;">
                  Subject: <i>{{ tmpl.subject }}</i>
                </div>
                <div class="rec-card-lg-actions">
                  <button (click)="openEditModal(tmpl)">Edit template</button>
                </div>
              </div>
              <div class="empty-state" *ngIf="!state.emailTemplates().length"><p>No email templates yet.</p></div>
            </div>
          </div>

          <!-- EMAIL LOGS -->
          <div class="settings-pane active" *ngIf="activeTab() === 'logs'">
            <div class="card">
              <table class="table">
                <thead>
                  <tr>
                    <th>Recipient</th>
                    <th>Template</th>
                    <th>Subject</th>
                    <th>Status</th>
                    <th>Sent</th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of state.emailHistory()">
                    <td class="name" style="font-weight:600;">{{ item.recipientName || item.recipientEmail }}</td>
                    <td class="muted">{{ item.templateCategory }}</td>
                    <td class="muted">{{ item.subject }}</td>
                    <td>
                      <span class="badge" [ngClass]="item.status === 'OPENED' ? 'published' : (item.status === 'DELIVERED' || item.status === 'SENT' ? 'processing' : 'failed')">
                        {{ item.status }}
                      </span>
                    </td>
                    <td class="muted">{{ (item.sentAt || item.createdAt) | date: 'MMM d, h:mm a' }}</td>
                  </tr>
                </tbody>
              </table>
              <div class="empty-state" *ngIf="!state.emailHistory().length"><p>No email history yet.</p></div>
            </div>
          </div>

          <!-- SCHEDULED SENDS -->
          <div class="settings-pane active" *ngIf="activeTab() === 'scheduled'">
            <div class="card">
              <table class="table">
                <thead>
                  <tr>
                    <th>Recipient</th>
                    <th>Template</th>
                    <th>Send time</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of state.scheduledNotifications()">
                    <td class="name" style="font-weight:600;">{{ item.recipientName || item.recipientEmail }}</td>
                    <td class="muted">{{ item.templateCategory }}</td>
                    <td class="muted">{{ item.scheduledAt | date: 'MMM d, h:mm a' }}</td>
                    <td>
                      <span class="row-link danger" (click)="cancelSend(item.id)">Cancel</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div class="empty-state" *ngIf="!state.scheduledNotifications().length"><p>Nothing scheduled.</p></div>
            </div>
          </div>

          <!-- FAILED SENDS -->
          <div class="settings-pane active" *ngIf="activeTab() === 'failed'">
            <div class="card">
              <table class="table">
                <thead>
                  <tr>
                    <th>Recipient</th>
                    <th>Template</th>
                    <th>Reason for failure</th>
                    <th>Attempts</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  <tr *ngFor="let item of state.failedNotifications()">
                    <td class="name" style="font-weight:600; color:var(--danger);">{{ item.recipientName || item.recipientEmail }}</td>
                    <td class="muted">{{ item.templateCategory }}</td>
                    <td class="muted" style="color:var(--danger);">{{ item.failureReason || '—' }}</td>
                    <td class="muted">{{ item.attempts }}</td>
                    <td>
                      <span class="row-link" (click)="retryFailed(item.id)">Retry</span>
                    </td>
                  </tr>
                </tbody>
              </table>
              <div class="empty-state" *ngIf="!state.failedNotifications().length"><p>No failed sends.</p></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Edit Template Modal Overlay -->
      <div class="modal-overlay" [ngClass]="{ 'show': activeEditTemplate() }">
        <div class="modal" style="max-width:500px; width:95%;">
          <div class="modal-head">
            <h3>Edit Email Template</h3>
            <button class="close-btn" (click)="activeEditTemplate.set(null)">&times;</button>
          </div>
          <div class="modal-body" *ngIf="activeEditTemplate() as tmpl">
            <div class="field" style="margin-bottom:12px;">
              <label>Template Name</label>
              <input type="text" [(ngModel)]="editName" style="width:100%;">
            </div>
            <div class="field" style="margin-bottom:12px;">
              <label>Trigger Condition</label>
              <input type="text" [ngModel]="tmpl.triggerEvent" style="width:100%;" readonly>
            </div>
            <div class="field" style="margin-bottom:12px;">
              <label>Subject Line</label>
              <input type="text" [(ngModel)]="editSubject" style="width:100%;">
            </div>
            <div class="field" style="margin-bottom:16px;">
              <label>HTML Body</label>
              <textarea rows="6" [(ngModel)]="editBody" style="width:100%; font-family:monospace; font-size:12px; padding:8px; border:1px solid var(--line); border-radius:8px; background:var(--paper); color:var(--ink);"></textarea>
            </div>
            <div style="display:flex; justify-content:flex-end; gap:8px;">
              <button class="btn-ghost" (click)="activeEditTemplate.set(null)">Cancel</button>
              <button class="btn-primary" (click)="saveTemplate()">Save changes</button>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .notifications-viewport {
      display: flex;
      flex-direction: column;
      gap: 20px;
      font-family: $font-sans;
    }

    .rec-card-grid {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 16px;

      @include respond-to('mobile') {
        grid-template-columns: 1fr;
      }
    }

    .rec-card-lg {
      background: var(--paper-card);
      border: 1px solid var(--line);
      border-radius: 14px;
      padding: 20px;
      display: flex;
      flex-direction: column;
      gap: 14px;
      @include transition-base;

      .rec-card-lg-top {
        display: flex;
        gap: 12px;
        align-items: center;
      }

      .resume-icon {
        width: 40px;
        height: 40px;
        border-radius: 9px;
        background: var(--indigo-tint);
        color: var(--indigo);
        display: flex;
        align-items: center;
        justify-content: center;
        flex-shrink: 0;

        svg {
          width: 20px;
          height: 20px;
          stroke: currentColor;
          fill: none;
          stroke-width: 1.8;
        }
      }

      .rcl-name {
        font-family: $font-sans;
        font-size: 14px;
        font-weight: 600;
        color: var(--ink);
      }

      .rcl-role {
        font-size: 11.5px;
        color: var(--ink-soft);
        margin-top: 1px;
      }

      .rec-card-lg-actions {
        border-top: 1px solid var(--line-soft);
        padding-top: 14px;
        margin-top: auto;

        button {
          width: 100%;
          padding: 8px 0;
          border-radius: 8px;
          font-size: 13px;
          font-weight: 500;
          font-family: $font-sans;
          border: 1px solid var(--line);
          background: transparent;
          color: var(--ink-soft);
          cursor: pointer;
          @include transition-base;

          &:hover {
            border-color: var(--indigo);
            color: var(--indigo);
            background: var(--indigo-tint);
          }
        }
      }
    }

    /* Modal overlay */
    .modal-overlay {
      position: fixed;
      inset: 0;
      background: rgba(28, 27, 41, 0.45);
      display: flex;
      align-items: center;
      justify-content: center;
      z-index: 1000;
      opacity: 0;
      pointer-events: none;
      transition: opacity 0.2s ease;
      &.show { opacity: 1; pointer-events: auto; }
    }

    .modal {
      background: var(--paper-card);
      border: 1px solid var(--line);
      border-radius: 14px;
      box-shadow: 0 12px 36px rgba(28, 27, 41, 0.16);
      animation: modalSlide 0.22s cubic-bezier(0.1, 0.8, 0.2, 1) forwards;
    }

    .modal-head {
      padding: 14px 18px;
      border-bottom: 1px solid var(--line-soft);
      display: flex;
      align-items: center;
      justify-content: space-between;
      h3 { font-family: $font-serif; font-size: 15.5px; font-weight: 560; color: var(--ink); margin: 0; }
      .close-btn { background: none; border: none; font-size: 20px; color: var(--ink-soft); cursor: pointer; }
    }

    .modal-body { padding: 18px; }

    .empty-state {
      text-align: center;
      padding: 40px 0;
      color: var(--ink-soft);
      font-size: 13.5px;
    }

    .row-link.danger {
      color: var(--danger);
      &:hover {
        color: var(--danger);
        text-decoration: underline;
      }
    }
  `]
})
export class NotificationsComponent implements OnInit {
  activeTab = signal<string>('templates');
  activeEditTemplate = signal<TemplateResponse | null>(null);

  // Form binds
  editName = '';
  editSubject = '';
  editBody = '';

  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.selectTab('templates');
  }

  selectTab(tab: string) {
    this.activeTab.set(tab);
    if (tab === 'templates') this.state.loadEmailTemplates();
    if (tab === 'logs') this.state.loadEmailHistory();
    if (tab === 'scheduled') this.state.loadScheduledEmails();
    if (tab === 'failed') this.state.loadFailedEmails();
  }

  openEditModal(tmpl: TemplateResponse) {
    this.activeEditTemplate.set(tmpl);
    this.editName = tmpl.name;
    this.editSubject = tmpl.subject;
    this.editBody = tmpl.bodyHtml;
  }

  saveTemplate() {
    const tmpl = this.activeEditTemplate();
    if (tmpl) {
      this.state.updateEmailTemplate(tmpl.id, {
        name: this.editName,
        subject: this.editSubject,
        bodyHtml: this.editBody,
        variablesHint: tmpl.variablesHint,
        active: tmpl.active,
      });
      this.activeEditTemplate.set(null);
    }
  }

  cancelSend(id: string) {
    this.state.cancelScheduledEmail(id);
  }

  retryFailed(id: string) {
    this.state.retryEmail(id);
  }
}
