import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

@Component({
  selector: 'status-badge',
  standalone: true,
  imports: [CommonModule],
  template: `
    <span class="badge" [ngClass]="typeClass">
      {{ label }}
    </span>
  `,
  styles: [`
    @use 'variables' as *;

    .badge {
      display: inline-flex;
      align-items: center;
      padding: 4px 10px;
      font-size: 11px;
      font-weight: 600;
      border-radius: var(--radius-small);
      text-transform: capitalize;
      white-space: nowrap;

      &.stage { background-color: var(--indigo-tint); color: var(--indigo); }
      &.match-high { background-color: var(--teal-tint); color: var(--teal); font-family: 'IBM Plex Mono', monospace; }
      &.match-mid { background-color: var(--gold-tint); color: #8A6A1F; font-family: 'IBM Plex Mono', monospace; }
      &.match-low { background-color: var(--line-soft); color: var(--ink-soft); font-family: 'IBM Plex Mono', monospace; }
      &.published { background-color: var(--teal-tint); color: var(--teal); }
      &.draft { background-color: var(--gold-tint); color: #8A6A1F; }
      &.archived { background-color: var(--line-soft); color: var(--ink-faint); }
      &.active { background-color: var(--indigo-tint); color: var(--indigo); }
      &.offer { background-color: var(--teal-tint); color: var(--teal); }
      &.rejected { background-color: var(--danger-tint); color: var(--danger); }
    }
  `]
})
export class StatusBadgeComponent {
  @Input() label: string = '';
  @Input() type: string = 'stage';

  get typeClass(): string {
    const t = this.type.toLowerCase();
    if (t.includes('shortlist') || t.includes('publish') || t.includes('high') || t.includes('offer')) {
      return 'match-high';
    }
    if (t.includes('pending') || t.includes('mid') || t.includes('consider')) {
      return 'match-mid';
    }
    if (t.includes('reject') || t.includes('archive') || t.includes('low')) {
      return 'match-low';
    }
    return t;
  }
}
