import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { ColorTone } from '../../core/models/notification.model';

@Component({
  selector: 'app-candidate-notifications',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-notifications">
      <div class="page-head">
        <div>
          <h1>Notifications</h1>
          <p>Updates on your applications, interviews and profile.</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-ghost" (click)="state.markAllNotificationsRead()">Mark all as read</button>
        </div>
      </div>
      <div class="card">
        <div class="list-card" style="max-height:none;">
          <div
            class="activity-item"
            *ngFor="let notif of state.candidateNotifications()"
            [style.cursor]="'pointer'"
            (click)="notif.status === 'UNREAD' && state.markNotificationRead(notif.id)"
          >
            <div class="activity-dot" [style.background]="dotColor(notif.colorTone)"></div>
            <div>
              <div class="atext" [style.fontWeight]="notif.status === 'UNREAD' ? 600 : 400">{{ notif.title }}</div>
              <div class="atext" style="margin-top:2px;">{{ notif.message }}</div>
              <div class="atime">{{ notif.createdAt | date: 'MMM d, h:mm a' }}</div>
            </div>
          </div>
          <div class="empty-state" *ngIf="!state.candidateNotifications().length">
            <p>No notifications yet.</p>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    .empty-state {
      text-align: center;
      padding: 40px 0;
      color: var(--ink-soft);
      font-size: 13.5px;
    }
  `]
})
export class CandidateNotificationsComponent implements OnInit {
  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadNotifications();
  }

  dotColor(tone: ColorTone): string {
    switch (tone) {
      case ColorTone.SUCCESS: return '#1F7A6C';
      case ColorTone.WARNING: return '#8A6A1F';
      case ColorTone.DANGER: return '#B3412C';
      default: return '#372F84';
    }
  }
}
