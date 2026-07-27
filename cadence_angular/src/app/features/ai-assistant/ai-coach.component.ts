import { Component, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { ChatMessage } from '../../core/models/models';

@Component({
  selector: 'app-ai-coach',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section csection active" id="csec-ai">
      <div class="page-head">
        <div>
          <h1>AI Career Coach</h1>
          <p>Improve your profile, resume and interview readiness with AI guidance.</p>
        </div>
      </div>
      
      <div class="row-2">
        <div class="card chat-card">
          <div class="chat-area" #chatArea>
            <!-- Preloaded / Initial messages -->
            <div class="bubble-ai">
              <div class="ai-text">Hi Rahul — your resume score is 86%. Here's what would push it higher:</div>
              <div class="ai-results">
                <div class="rec-item">
                  <div class="rec-top">
                    <span class="who">Add quantifiable impact</span>
                    <span class="match-badge">+4%</span>
                  </div>
                  <div class="rec-skills">
                    "Reduced API latency by 30%" reads stronger than "improved performance".
                  </div>
                </div>
                <div class="rec-item">
                  <div class="rec-top">
                    <span class="who">Add a missing skill</span>
                    <span class="match-badge">+3%</span>
                  </div>
                  <div class="rec-skills">
                    <span class="skill-pill">Kubernetes</span> appears in 70% of roles you view but isn't on your profile.
                  </div>
                </div>
              </div>
            </div>

            <!-- Rest of chat history -->
            <ng-container *ngFor="let msg of state.candidateChatHistory()">
              <div [className]="msg.sender === 'user' ? 'bubble-user' : 'bubble-ai'">
                <div class="ai-text" *ngIf="msg.sender === 'ai'">{{ msg.text }}</div>
                <span *ngIf="msg.sender === 'user'">{{ msg.text }}</span>
              </div>
            </ng-container>

            <!-- Typing indicator -->
            <div class="bubble-ai" *ngIf="isTyping()">
              <div class="ai-text typing-dots">
                <span></span><span></span><span></span>
              </div>
            </div>
          </div>

          <div class="chat-suggest">
            <span class="chip" (click)="fillChat('Review my resume')">Review my resume</span>
            <span class="chip" (click)="fillChat('Start a mock interview')">Start a mock interview</span>
            <span class="chip" (click)="fillChat('What skills am I missing for Senior roles?')">What skills am I missing for Senior roles?</span>
          </div>

          <div class="chat-input-row">
            <input 
              #chatInput 
              id="chat-input" 
              placeholder="Ask your AI career coach anything…"
              (keyup.enter)="sendMsg(chatInput.value); chatInput.value = ''"
            >
            <button class="btn-primary-sm" (click)="sendMsg(chatInput.value); chatInput.value = ''">Ask</button>
          </div>
        </div>

        <div class="sidebar-cards">
          <div class="card card-margin">
            <div class="card-head">
              <h2>Missing skills</h2>
            </div>
            <div class="chip-group">
              <span class="skill-pill">Kubernetes</span>
              <span class="skill-pill">GraphQL</span>
              <span class="skill-pill">Terraform</span>
            </div>
            <p class="card-footer-text">Recommended based on roles you've viewed.</p>
          </div>

          <div class="card">
            <div class="card-head">
              <h2>Mock interview</h2>
            </div>
            <p class="card-body-text">Practice a 15-minute AI-led mock interview tailored to Backend Engineer roles.</p>
            <button class="btn-primary-sm full-width" (click)="startMock()">Start mock interview</button>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: [`
    @use 'variables' as *;

    .csection {
      padding: 0;
    }

    .row-2 {
      display: grid;
      grid-template-columns: 2fr 1fr;
      gap: 16px;
      align-items: start;
    }

    @media (max-width: 992px) {
      .row-2 {
        grid-template-columns: 1fr;
      }
    }

    .chat-card {
      max-width: 900px;
      display: flex;
      flex-direction: column;
      height: calc(100vh - 180px);
      min-height: 500px;
    }

    .chat-area {
      flex: 1;
      display: flex;
      flex-direction: column;
      gap: 18px;
      padding: 6px 4px 22px;
      overflow-y: auto;
      @include custom-scrollbar;
    }

    .bubble-user {
      align-self: flex-end;
      background: var(--indigo);
      color: var(--paper);
      padding: 10px 14px;
      border-radius: 12px 12px 2px 12px;
      max-width: 70%;
      font-size: 13.5px;
      line-height: 1.5;
    }

    .bubble-ai {
      align-self: flex-start;
      max-width: 88%;
      display: flex;
      flex-direction: column;
      align-items: flex-start;
    }

    .bubble-ai .ai-text {
      background: var(--line-soft);
      padding: 10px 14px;
      border-radius: 2px 12px 12px 12px;
      font-size: 13.5px;
      margin-bottom: 10px;
      display: inline-block;
      color: var(--ink);
      line-height: 1.5;
    }

    .ai-results {
      display: grid;
      grid-template-columns: repeat(2, 1fr);
      gap: 10px;
      width: 100%;
      margin-top: 4px;
    }

    @media (max-width: 600px) {
      .ai-results {
        grid-template-columns: 1fr;
      }
    }

    .rec-item {
      padding: 12px;
      border: 1px solid var(--line);
      border-radius: 11px;
      background: var(--paper-card);
    }

    .rec-top {
      display: flex;
      justify-content: space-between;
      align-items: center;
      margin-bottom: 6px;
    }

    .rec-top .who {
      font-size: 13px;
      font-weight: 600;
      color: var(--ink);
    }

    .rec-top .match-badge {
      font-family: $font-mono;
      font-size: 11px;
      font-weight: 600;
      background: var(--teal-tint);
      color: var(--teal);
      padding: 2px 6px;
      border-radius: 4px;
    }

    .rec-skills {
      font-size: 12px;
      color: var(--ink-soft);
      line-height: 1.4;
    }

    .skill-pill {
      font-size: 10.5px;
      background: var(--line-soft);
      color: var(--ink-soft);
      padding: 2px 6px;
      border-radius: 4px;
      font-weight: 500;
    }

    .chat-suggest {
      display: flex;
      gap: 8px;
      margin-bottom: 14px;
      flex-wrap: wrap;
    }

    .chip {
      font-size: 11.5px;
      color: var(--ink-soft);
      background: var(--paper);
      border: 1px solid var(--line);
      padding: 5px 10px;
      border-radius: 999px;
      cursor: pointer;
      @include transition-base;

      &:hover {
        border-color: var(--indigo);
        color: var(--indigo);
        background: var(--paper-card);
      }
    }

    .chat-input-row {
      display: flex;
      gap: 10px;
      border-top: 1px solid var(--line);
      padding-top: 16px;
      flex-shrink: 0;
    }

    .chat-input-row input {
      flex: 1;
      font-size: 13.5px;
      padding: 11px 14px;
      border: 1px solid var(--line);
      border-radius: 9px;
      outline: none;
      background: var(--paper);
      color: var(--ink);
      font-family: $font-sans;

      &:focus {
        border-color: var(--indigo);
        box-shadow: 0 0 0 3px rgba(55, 47, 132, 0.12);
        background: var(--paper-card);
      }
    }

    .sidebar-cards {
      display: flex;
      flex-direction: column;
      gap: 16px;
    }

    .card-margin {
      margin-bottom: 0px;
    }

    .card-head {
      margin-bottom: 12px;
      h2 {
        font-size: 14.5px;
        font-weight: 600;
        margin: 0;
        color: var(--ink);
      }
    }

    .chip-group {
      display: flex;
      flex-wrap: wrap;
      gap: 6px;
    }

    .card-footer-text {
      font-size: 12px;
      color: var(--ink-soft);
      margin-top: 10px;
    }

    .card-body-text {
      font-size: 12.5px;
      color: var(--ink-soft);
      margin-bottom: 12px;
      line-height: 1.5;
    }

    .full-width {
      width: 100%;
      justify-content: center;
    }

    // Typing dots animation
    .typing-dots {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      height: 20px;
      padding: 10px 20px !important;

      span {
        width: 6px;
        height: 6px;
        background: var(--ink-soft);
        border-radius: 50%;
        animation: typing 1.2s infinite;

        &:nth-child(2) { animation-delay: 0.2s; }
        &:nth-child(3) { animation-delay: 0.4s; }
      }
    }

    @keyframes typing {
      0%, 60%, 100% { transform: translateY(0); }
      30% { transform: translateY(-4px); }
    }
  `],
  preserveWhitespaces: false
})
export class AiCoachComponent implements AfterViewChecked {
  @ViewChild('chatArea') private chatArea!: ElementRef;

  isTyping = signal<boolean>(false);

  constructor(public state: AppStateService) {}

  ngAfterViewChecked() {
    this.scrollToBottom();
  }

  scrollToBottom(): void {
    try {
      this.chatArea.nativeElement.scrollTop = this.chatArea.nativeElement.scrollHeight;
    } catch(err) {}
  }

  fillChat(text: string) {
    this.sendMsg(text);
  }

  sendMsg(text: string) {
    if (!text.trim()) return;
    this.state.sendCandidateChatMessage(text);
    
    // show typing indicator
    this.isTyping.set(true);
    setTimeout(() => {
      this.isTyping.set(false);
    }, 1000);
  }

  startMock() {
    this.state.showToast('Starting mock interview…');
  }
}
