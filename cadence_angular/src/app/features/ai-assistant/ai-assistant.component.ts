import { Component, signal, ElementRef, ViewChild, AfterViewChecked } from '@angular/core';
import { CommonModule } from '@angular/common';
import { AppStateService } from '../../core/services/app-state.service';
import { ChatMessage } from '../../core/models/models';

@Component({
  selector: 'app-ai-assistant',
  standalone: true,
  imports: [CommonModule],
  template: `
    <section class="section active" id="sec-ai">
      <div class="page-head">
        <div>
          <h1>AI HR Assistant</h1>
          <p>Ask anything about your candidates and hiring pipeline in plain English.</p>
        </div>
      </div>

      <div class="card chat-card">
        <div class="chat-area" #chatArea>
          <!-- Pre-populated / Initial Chat Conversation to match HTML -->
          <div class="bubble-user">Show Java developers with Spring Boot, Kafka and Redis</div>
          
          <div class="bubble-ai">
            <div class="ai-text">Found 8 candidates matching all four skills, sorted by match score:</div>
            <div class="ai-results">
              <div class="rec-item">
                <div class="rec-top">
                  <span class="who">Arjun Verma</span>
                  <span class="match-badge">94%</span>
                </div>
                <div class="rec-skills">
                  <span class="skill-pill">Java</span>
                  <span class="skill-pill">Kafka</span>
                  <span class="skill-pill">Redis</span>
                </div>
              </div>
              <div class="rec-item">
                <div class="rec-top">
                  <span class="who">Neha Kapoor</span>
                  <span class="match-badge">89%</span>
                </div>
                <div class="rec-skills">
                  <span class="skill-pill">Java</span>
                  <span class="skill-pill">AWS</span>
                </div>
              </div>
              <div class="rec-item">
                <div class="rec-top">
                  <span class="who">Karthik Iyer</span>
                  <span class="match-badge">86%</span>
                </div>
                <div class="rec-skills">
                  <span class="skill-pill">Kafka</span>
                  <span class="skill-pill">Docker</span>
                </div>
              </div>
            </div>
          </div>

          <div class="bubble-user">Which of these can join within 30 days?</div>
          
          <div class="bubble-ai">
            <div class="ai-text">2 of the 8 have a notice period under 30 days - <b>Arjun Verma</b> (15 days) and <b>Karthik Iyer</b> (30 days). Want me to schedule AI interviews for both?</div>
          </div>

          <!-- Dynamic Chat History -->
          <ng-container *ngFor="let msg of state.recruiterChatHistory()">
            <div [className]="msg.sender === 'user' ? 'bubble-user' : 'bubble-ai'">
              <div class="ai-text" *ngIf="msg.sender === 'ai'">{{ msg.text }}</div>
              <span *ngIf="msg.sender === 'user'">{{ msg.text }}</span>
              
              <!-- Render AI results inside the dynamic message if any exist -->
              <div class="ai-results" *ngIf="msg.sender === 'ai' && msg.aiResults && msg.aiResults.length > 0">
                <div class="rec-item" *ngFor="let r of msg.aiResults">
                  <div class="rec-top">
                    <span class="who">{{ r.name }}</span>
                    <span class="match-badge">{{ r.score }}</span>
                  </div>
                  <div class="rec-skills">
                    <span class="skill-pill" *ngFor="let skill of r.skills.split(',')">{{ skill.trim() }}</span>
                  </div>
                </div>
              </div>
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
          <span class="chip" (click)="fillChat('Show all Java developers')">Show all Java developers</span>
          <span class="chip" (click)="fillChat('Who can join immediately?')">Who can join immediately?</span>
          <span class="chip" (click)="fillChat('Any Django developers?')">Any Django developers?</span>
        </div>

        <div class="chat-input-row">
          <input 
            #chatInput
            id="chat-input" 
            placeholder="Ask your AI recruiting assistant anything…"
            (keyup.enter)="sendMsg(chatInput.value); chatInput.value = ''"
          >
          <button class="btn-primary-sm" (click)="sendMsg(chatInput.value); chatInput.value = ''">Ask</button>
        </div>
      </div>
    </section>
  `,
  styles: [`
    @use 'variables' as *;

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
      grid-template-columns: repeat(3, 1fr);
      gap: 10px;
      width: 100%;
      margin-top: 4px;
    }

    @media (max-width: 768px) {
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
      display: flex;
      flex-wrap: wrap;
      gap: 4px;
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
      margin-top: 10px;
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
export class AiAssistantComponent implements AfterViewChecked {
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
    this.state.sendRecruiterChatMessage(text);
    
    // show typing indicator
    this.isTyping.set(true);
    setTimeout(() => {
      this.isTyping.set(false);
    }, 1000);
  }
}
