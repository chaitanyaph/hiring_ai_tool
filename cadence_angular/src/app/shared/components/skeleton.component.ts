import { Component, Input } from '@angular/core';
import { CommonModule } from '@angular/common';

/**
 * Generic shimmering placeholder block. Used in place of real content while a
 * page's initial data fetch is in flight -- shape/size is left to the caller
 * via width/height/radius rather than trying to pixel-match every page's final
 * layout, so one component covers list rows, cards, and KPI tiles alike.
 */
@Component({
  selector: 'app-skeleton',
  standalone: true,
  imports: [CommonModule],
  template: `<div class="skeleton-block" [style.width]="width" [style.height]="height" [style.border-radius]="radius"></div>`,
  styles: [`
    .skeleton-block {
      background: linear-gradient(90deg, var(--line-soft) 25%, var(--line) 37%, var(--line-soft) 63%);
      background-size: 400% 100%;
      animation: skeleton-shimmer 1.4s ease infinite;
    }
    @keyframes skeleton-shimmer {
      0% { background-position: 100% 50%; }
      100% { background-position: 0 50%; }
    }
  `]
})
export class SkeletonComponent {
  @Input() width: string = '100%';
  @Input() height: string = '14px';
  @Input() radius: string = '6px';
}
