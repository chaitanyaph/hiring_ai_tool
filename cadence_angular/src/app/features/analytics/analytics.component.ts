import { Component, OnInit, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="analytics-viewport">

      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>Analytics</h1>
          <p>Last 90 days across all open roles</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-ghost" (click)="state.exportAnalyticsReport()">
            <svg viewBox="0 0 24 24" width="14" height="14" style="stroke:currentColor; fill:none; stroke-width:2.2; margin-right:6px;"><path d="M12 3v12M7 10l5 5 5-5"/><path d="M5 21h14"/></svg>
            Export
          </button>
        </div>
      </div>

      <!-- KPI Row -- no historical baseline exists on the backend for trend deltas
           (DashboardResponse has no prior-period comparison field), so the fabricated
           "+4% vs last quarter" style trend lines are dropped rather than faked. -->
      <div class="kpi-row" style="margin-bottom: 20px;">
        <div class="kpi-card">
          <div class="kpi-label">Offer acceptance</div>
          <div class="kpi-value">{{ pct(dashboard()?.offerAcceptanceRatePercent) }}</div>
          <div class="kpi-trend"><span class="muted">company-wide</span></div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Candidate drop-off</div>
          <div class="kpi-value">{{ pct(dashboard()?.candidateDropoffRatePercent) }}</div>
          <div class="kpi-trend"><span class="muted">company-wide</span></div>
        </div>
        <div class="kpi-card">
          <!-- diversityRatioPercent is always null on the backend -- no gender/diversity
               field exists on any event in the platform (confirmed in analytics-service's
               DashboardResponse javadoc); shown as "Not available" rather than a fabricated number. -->
          <div class="kpi-label">Diversity ratio</div>
          <div class="kpi-value">{{ dashboard()?.diversityRatioPercent != null ? pct(dashboard()?.diversityRatioPercent) : 'N/A' }}</div>
          <div class="kpi-trend"><span class="muted">not tracked by any service yet</span></div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Avg. time to hire</div>
          <div class="kpi-value">{{ dashboard()?.avgTimeToHireDays != null ? (dashboard()!.avgTimeToHireDays! | number:'1.0-0') + 'd' : '—' }}</div>
          <div class="kpi-trend"><span class="muted">company-wide</span></div>
        </div>
      </div>

      <!-- Row 2: Charts and breakdowns -->
      <div class="row-2">
        <!-- Left: Monthly Hiring bar chart -->
        <div class="card">
          <div class="card-head">
            <div>
              <h2>Monthly hiring</h2>
              <div class="card-sub">Offers accepted per month</div>
            </div>
          </div>
          <div style="height: 180px; padding: 10px 0;">
            <svg viewBox="0 0 560 180" width="100%" height="180" style="display:block; overflow:visible;">
              <line x1="0" y1="160" x2="560" y2="160" stroke="#E7E3D6" stroke-width="1"/>
              <g>
                <ng-container *ngFor="let bar of barChartData(); let i = index; let last = last">
                  <rect
                    [attr.x]="bar.x"
                    [attr.y]="bar.y"
                    [attr.width]="bar.width"
                    [attr.height]="bar.height"
                    rx="6"
                    [attr.fill]="last ? '#C79A2E' : '#372F84'"
                    [attr.opacity]="last ? '1' : '0.85'"
                  />
                  <text
                    [attr.x]="bar.x + bar.width/2"
                    y="174"
                    text-anchor="middle"
                    font-size="11"
                    fill="#615F72"
                    font-family="'Inter', sans-serif"
                  >
                    {{ bar.month }}
                  </text>
                  <text
                    [attr.x]="bar.x + bar.width/2"
                    [attr.y]="bar.y - 6"
                    text-anchor="middle"
                    font-size="11"
                    fill="#1C1B29"
                    font-family="'IBM Plex Mono', monospace"
                    font-weight="600"
                  >
                    {{ bar.value }}
                  </text>
                </ng-container>
              </g>
            </svg>
            <div class="empty-state" *ngIf="!barChartData().length">No hiring data yet.</div>
          </div>
        </div>

        <!-- Right: Offer Acceptance (donut) + Source breakdown (list) -->
        <div class="card">
          <div class="split-card-section" style="margin-bottom: 24px;">
            <div class="card-head" style="margin-bottom:12px;">
              <h2>Offer acceptance</h2>
            </div>
            <div class="donut-row">
              <svg width="80" height="80" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" fill="none" stroke="#EFEBDF" stroke-width="14"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#1F7A6C" stroke-width="14" [attr.stroke-dasharray]="offerAcceptanceArc()" transform="rotate(-90 50 50)"/>
              </svg>
              <div>
                <div class="kpi-value" style="font-size:22px; font-weight:600;">{{ pct(dashboard()?.offerAcceptanceRatePercent) }}</div>
                <div class="card-sub">of offers accepted</div>
              </div>
            </div>
          </div>
          <!-- "Skill demand" has no backing endpoint anywhere in analytics-service (no
               event on the platform carries per-skill demand data) -- this section is
               repurposed to the one real labeled-value breakdown the backend does provide,
               DashboardResponse.sourceBreakdown (application source channel). -->
          <div>
            <div class="card-head" style="margin-bottom:12px;">
              <h2>Source breakdown</h2>
            </div>
            <div class="source-list">
              <div class="source-item" *ngFor="let item of sourceBreakdown(); let i = index">
                <span>{{ item.label }}</span>
                <div class="source-track"><div class="source-fill" [style.width.%]="item.value" [style.background]="sourceColor(i)"></div></div>
                <span>{{ item.value | number:'1.0-0' }}%</span>
              </div>
              <div class="empty-state" *ngIf="!sourceBreakdown().length">No source data yet.</div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .analytics-viewport {
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0;
      font-family: $font-sans;
    }

    .muted {
      color: var(--ink-soft);
    }

    .empty-state {
      text-align: center;
      padding: 20px 0;
      color: var(--ink-soft);
      font-size: 12.5px;
    }
  `]
})
export class AnalyticsComponent implements OnInit {
  constructor(public state: AppStateService) {}

  ngOnInit() {
    this.state.loadAnalyticsDashboard();
  }

  dashboard = computed(() => this.state.analyticsDashboard());

  pct(value: number | null | undefined): string {
    return value != null ? `${Math.round(value)}%` : '—';
  }

  barChartData = computed(() => {
    const points = this.dashboard()?.monthlyHiring ?? [];
    if (!points.length) return [];
    const bw = 46;
    const gap = (560 - (bw * points.length)) / (points.length + 1);
    const maxVal = Math.max(...points.map((p) => p.value), 1);
    return points.map((p, i) => {
      const h = (p.value / maxVal) * 120;
      const x = gap + i * (bw + gap);
      const y = 160 - h;
      return { month: p.monthLabel, value: p.value, x, y, width: bw, height: h };
    });
  });

  offerAcceptanceArc = computed(() => {
    const pct = this.dashboard()?.offerAcceptanceRatePercent ?? 0;
    const circumference = 251.3;
    return `${(circumference * pct) / 100} ${circumference}`;
  });

  sourceBreakdown = computed(() => this.dashboard()?.sourceBreakdown ?? []);

  private sourceColors = ['#372F84', '#C79A2E', '#1F7A6C', '#372F84', '#C79A2E'];

  sourceColor(i: number): string {
    return this.sourceColors[i % this.sourceColors.length];
  }
}
