import { Component, computed, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { Candidate } from '../../core/models/models';

@Component({
  selector: 'app-recruiter-dashboard',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="dashboard-wrap">
      <!-- Page Heading -->
      <div class="page-head">
        <div>
          <h1>Good morning, Ananya</h1>
          <p>Here's how hiring is trending across Acme Corp today.</p>
        </div>
      </div>

      <!-- KPI Row -->
      <div class="kpi-row">
        <div class="kpi-card">
          <div class="kpi-label">Total applications</div>
          <div class="kpi-value">1,204</div>
          <div class="kpi-trend up">
            <svg viewBox="0 0 24 24" width="12" height="12" style="stroke:currentColor; fill:none; stroke-width:2.4; margin-right:4px;"><path d="M4 17l6-6 4 4 6-8"/></svg>
            +12% <span class="muted">vs last month</span>
          </div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Open positions</div>
          <div class="kpi-value">18</div>
          <div class="kpi-trend"><span class="muted">across 4 departments</span></div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Candidates in pipeline</div>
          <div class="kpi-value">342</div>
          <div class="kpi-trend up">
            <svg viewBox="0 0 24 24" width="12" height="12" style="stroke:currentColor; fill:none; stroke-width:2.4; margin-right:4px;"><path d="M4 17l6-6 4 4 6-8"/></svg>
            +8% <span class="muted">this week</span>
          </div>
        </div>
        <div class="kpi-card">
          <div class="kpi-label">Avg. time to hire</div>
          <div class="kpi-value">19d</div>
          <div class="kpi-trend up">
            <svg viewBox="0 0 24 24" width="12" height="12" style="stroke:currentColor; fill:none; stroke-width:2.4; margin-right:4px;"><path d="M4 7l6 6 4-4 6 8"/></svg>
            -3d <span class="muted">vs last quarter</span>
          </div>
        </div>
      </div>

      <!-- Interview funnel card -->
      <div class="card funnel-card" style="margin-bottom: 20px;">
        <div class="card-head">
          <div>
            <h2>Interview funnel</h2>
            <div class="card-sub">Backend Engineer &middot; Senior DevOps &middot; Frontend Engineer</div>
          </div>
          <a class="card-link" (click)="state.showToast('Opening funnel report…')">View report</a>
        </div>
        <div class="funnel">
          <div class="funnel-row">
            <div class="stage-name">Applicants</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" style="width:100%;">1,204</div>
            </div>
            <div class="conv">&mdash;</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">AI Shortlisted <small>match score &ge; 70</small></div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" style="width:64%;">318</div>
            </div>
            <div class="conv"><b>26%</b> of applicants</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Interview pending</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" style="width:44%;">96</div>
            </div>
            <div class="conv"><b>30%</b> of shortlisted</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Technical round</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" style="width:34%;">61</div>
            </div>
            <div class="conv"><b>64%</b> of interviewed</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">HR round</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" style="width:26%;">40</div>
            </div>
            <div class="conv"><b>66%</b> of technical</div>
          </div>
          <div class="funnel-row">
            <div class="stage-name">Offer released</div>
            <div class="funnel-bar-track">
              <div class="funnel-bar-fill" style="width:16%;">22</div>
            </div>
            <div class="conv"><b>55%</b> of HR round</div>
          </div>
        </div>
      </div>

      <!-- Row 2: Charts and breakdowns -->
      <div class="row-2" style="margin-bottom: 20px;">
        <!-- Left: Hiring analytics -->
        <div class="card">
          <div class="card-head">
            <div>
              <h2>Hiring analytics</h2>
              <div class="card-sub">Applications received over time</div>
            </div>
            <div class="period-toggle">
              <button [class.active]="activePeriod() === '7d'" (click)="activePeriod.set('7d')">7D</button>
              <button [class.active]="activePeriod() === '30d'" (click)="activePeriod.set('30d')">30D</button>
              <button [class.active]="activePeriod() === '90d'" (click)="activePeriod.set('90d')">90D</button>
            </div>
          </div>
          <div class="chart-wrap">
            <svg viewBox="0 0 560 180" preserveAspectRatio="none" style="display:block; width:100%; height:180px;">
              <defs>
                <linearGradient id="areaFill" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#372F84" stop-opacity="0.22"/>
                  <stop offset="100%" stop-color="#372F84" stop-opacity="0"/>
                </linearGradient>
              </defs>
              <line x1="0" y1="140" x2="560" y2="140" stroke="#E7E3D6" stroke-width="1"/>
              <line x1="0" y1="90" x2="560" y2="90" stroke="#E7E3D6" stroke-width="1"/>
              <line x1="0" y1="40" x2="560" y2="40" stroke="#E7E3D6" stroke-width="1"/>
              <path [attr.d]="chartData().area" fill="url(#areaFill)"/>
              <path [attr.d]="chartData().line" fill="none" stroke="#372F84" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round"/>
              <g>
                <circle *ngFor="let p of filteredPoints()" [attr.cx]="p[0]" [attr.cy]="p[1]" r="3.2" fill="#372F84"/>
              </g>
            </svg>
          </div>
        </div>

        <!-- Right: Resume screening + Source Analysis -->
        <div class="card">
          <div class="split-card-section">
            <div class="card-head" style="margin-bottom:12px;">
              <h2>Resume screening</h2>
            </div>
            <div class="donut-row">
              <svg width="92" height="92" viewBox="0 0 100 100">
                <circle cx="50" cy="50" r="40" fill="none" stroke="#EFEBDF" stroke-width="14"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#1F7A6C" stroke-width="14" stroke-dasharray="105.6 251.3" transform="rotate(-90 50 50)"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#C79A2E" stroke-width="14" stroke-dasharray="77.9 251.3" stroke-dashoffset="-105.6" transform="rotate(-90 50 50)"/>
                <circle cx="50" cy="50" r="40" fill="none" stroke="#B3412C" stroke-width="14" opacity="0.55" stroke-dasharray="67.9 251.3" stroke-dashoffset="-183.5" transform="rotate(-90 50 50)"/>
              </svg>
              <div class="legend">
                <div class="legend-item"><span class="legend-dot" style="background:#1F7A6C;"></span>Recommended<b>42%</b></div>
                <div class="legend-item"><span class="legend-dot" style="background:#C79A2E;"></span>Consider<b>31%</b></div>
                <div class="legend-item"><span class="legend-dot" style="background:#B3412C; opacity:.55;"></span>Reject<b>27%</b></div>
              </div>
            </div>
          </div>
          <div>
            <div class="card-head" style="margin-bottom:12px;">
              <h2>Source analysis</h2>
            </div>
            <div class="source-list">
              <div class="source-item"><span>LinkedIn</span><div class="source-track"><div class="source-fill" style="width:34%;"></div></div><span>34%</span></div>
              <div class="source-item"><span>Careers page</span><div class="source-track"><div class="source-fill" style="width:28%; background:#C79A2E;"></div></div><span>28%</span></div>
              <div class="source-item"><span>Referral</span><div class="source-track"><div class="source-fill" style="width:19%; background:#1F7A6C;"></div></div><span>19%</span></div>
              <div class="source-item"><span>Naukri</span><div class="source-track"><div class="source-fill" style="width:12%;"></div></div><span>12%</span></div>
              <div class="source-item"><span>CSV import</span><div class="source-track"><div class="source-fill" style="width:7%;"></div></div><span>7%</span></div>
            </div>
          </div>
        </div>
      </div>

      <!-- Row 3: Stack lists -->
      <div class="row-3">
        <!-- Upcoming interviews -->
        <div class="card">
          <div class="card-head">
            <h2>Upcoming interviews</h2>
            <a class="card-link" (click)="router.navigate(['/recruiter/interviews'])">Calendar</a>
          </div>
          <div class="list-card">
            <div class="interview-item">
              <div class="avatar">PK</div>
              <div class="meta">
                <div class="who">Priya Kulkarni</div>
                <div class="role">Backend Engineer</div>
                <span class="tag ai">AI Interview</span>
              </div>
              <div class="when">Today<br>3:30 PM</div>
            </div>
            <div class="interview-item">
              <div class="avatar">RM</div>
              <div class="meta">
                <div class="who">Rohan Mehta</div>
                <div class="role">Senior DevOps Engineer</div>
                <span class="tag tech">Technical round</span>
              </div>
              <div class="when">Today<br>5:00 PM</div>
            </div>
            <div class="interview-item">
              <div class="avatar">SI</div>
              <div class="meta">
                <div class="who">Sneha Iyer</div>
                <div class="role">Frontend Engineer</div>
                <span class="tag hr">HR round</span>
              </div>
              <div class="when">Tomorrow<br>11:00 AM</div>
            </div>
          </div>
        </div>

        <!-- AI Recommendations -->
        <div class="card">
          <div class="card-head">
            <h2>AI recommendations</h2>
            <a class="card-link" (click)="router.navigate(['/recruiter/ai-assistant'])">See all</a>
          </div>
          <div class="list-card">
            <div class="rec-item">
              <div class="rec-top">
                <span class="who">Arjun Verma</span>
                <span class="match-badge">94% match</span>
              </div>
              <div class="rec-skills">
                <span class="skill-pill">Java</span>
                <span class="skill-pill">Spring Boot</span>
                <span class="skill-pill">Kafka</span>
                <span class="skill-pill">Redis</span>
              </div>
              <div class="rec-actions">
                <button class="view" style="font-family:'Inter',sans-serif;" (click)="viewProfile('cand-1')">View profile</button>
                <button class="shortlist" style="font-family:'Inter',sans-serif;" (click)="state.showToast('Added to shortlist')">Shortlist</button>
              </div>
            </div>
            <div class="rec-item">
              <div class="rec-top">
                <span class="who">Neha Kapoor</span>
                <span class="match-badge">89% match</span>
              </div>
              <div class="rec-skills">
                <span class="skill-pill">Java</span>
                <span class="skill-pill">Microservices</span>
                <span class="skill-pill">AWS</span>
              </div>
              <div class="rec-actions">
                <button class="view" style="font-family:'Inter',sans-serif;" (click)="viewProfile('cand-5')">View profile</button>
                <button class="shortlist" style="font-family:'Inter',sans-serif;" (click)="state.showToast('Added to shortlist')">Shortlist</button>
              </div>
            </div>
          </div>
        </div>

        <!-- Recent Activity -->
        <div class="card">
          <div class="card-head">
            <h2>Recent activity</h2>
          </div>
          <div class="list-card">
            <div class="activity-item">
              <div class="activity-dot"></div>
              <div>
                <div class="atext">AI Matching shortlisted <b>14 candidates</b> for Backend Engineer</div>
                <div class="atime">12 min ago</div>
              </div>
            </div>
            <div class="activity-item">
              <div class="activity-dot" style="background:#1F7A6C;"></div>
              <div>
                <div class="atext">Interview completed — Priya K. scored <b>8.7 / 10</b></div>
                <div class="atime">40 min ago</div>
              </div>
            </div>
            <div class="activity-item">
              <div class="activity-dot" style="background:#372F84;"></div>
              <div>
                <div class="atext">Offer generated for <b>Rohan Mehta</b></div>
                <div class="atime">2h ago</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  `,
  styles: [`
    @use 'variables' as *;

    .dashboard-wrap {
      padding: 0;
      display: flex;
      flex-direction: column;
      gap: 0;
      font-family: $font-sans;
      color: var(--ink);
    }

    .muted {
      color: var(--ink-soft);
    }
  `]
})
export class RecruiterDashboardComponent {
  activePeriod = signal<'7d' | '30d' | '90d'>('7d');

  private datasets = {
    '7d': [40, 58, 49, 72, 66, 90, 81],
    '30d': [30, 42, 38, 55, 48, 63, 58, 70, 64, 80, 74, 88, 82, 95, 90, 60, 52, 47, 63, 58, 70, 66, 80, 75, 90, 85, 96, 92, 100, 94],
    '90d': [20, 28, 25, 34, 30, 40, 36, 45, 42, 50, 47, 55, 52, 60, 57, 64, 60, 68, 65, 72, 69, 76, 72, 80, 77, 84, 80, 88, 85, 92, 88, 95, 91, 98, 94, 100, 96, 90, 85, 80, 75, 70, 66, 62, 58, 55, 52, 50, 48, 46, 44, 42, 40, 44, 48, 52, 56, 60, 64, 68, 72, 76, 80, 84, 88, 92, 96, 100, 94, 88, 82, 76, 70, 64, 58, 52, 46, 40, 44, 48, 52, 56, 60, 64, 68, 72, 76, 80, 84, 88]
  };

  chartData = computed(() => {
    const values = this.datasets[this.activePeriod()];
    const w = 560, h = 180, pad = 10;
    const max = Math.max(...values), min = Math.min(...values);
    const stepX = w / (values.length - 1);
    
    const points: [number, number][] = values.map((v, i) => {
      const x = i * stepX;
      const y = h - pad - ((v - min) / (max - min || 1)) * (h - pad * 2);
      return [x, y];
    });

    let line = `M ${points[0][0]} ${points[0][1]}`;
    for (let i = 1; i < points.length; i++) {
      line += ` L ${points[i][0]} ${points[i][1]}`;
    }
    
    const area = `${line} L ${points[points.length - 1][0]} ${h} L 0 ${h} Z`;
    
    return { line, area, points };
  });

  filteredPoints = computed(() => {
    const points = this.chartData().points;
    const showEvery = points.length > 20 ? Math.round(points.length / 10) : 1;
    return points.filter((p, i) => i % showEvery === 0 || i === points.length - 1);
  });

  constructor(public state: AppStateService, public router: Router) {}

  viewProfile(candId: string) {
    const cand = this.state.candidates().find(c => c.id === candId);
    if (cand) {
      this.state.selectCandidate(cand);
      this.router.navigate(['/recruiter/candidates', candId]);
    }
  }
}
