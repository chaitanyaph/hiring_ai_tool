import { Component, OnInit, OnDestroy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { AppStateService } from '../../core/services/app-state.service';
import { InterviewMode, InterviewSessionStatus } from '../../core/models/ai-interview.model';

interface ChatBubble {
  who: 'ai' | 'user';
  text: string;
}

/**
 * The candidate-facing live AI interview. ai-interview-service's /answer endpoint only
 * ever accepts a text answerText -- there is no audio/video upload endpoint anywhere on
 * the platform. So Voice/Video modes use the browser's real getUserMedia (camera preview,
 * mic access) and the Web Speech API for genuine speech-to-text, but what's actually sent
 * to the backend is always the resulting text -- never a fabricated transcript.
 */
@Component({
  selector: 'app-candidate-ai-interview',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div id="interview-view" class="show">

      <!-- LOADING -->
      <div class="iv-intro-screen" *ngIf="!details() && !loadError()">
        <div class="iv-intro-card">
          <div class="skeleton-card">
            <div class="skeleton sk-icon"></div>
            <div class="skeleton sk-title"></div>
            <div class="skeleton sk-sub"></div>
          </div>
        </div>
      </div>

      <!-- ERROR / SESSION EXPIRED -->
      <div class="iv-intro-screen" *ngIf="loadError()">
        <div class="iv-intro-card" style="text-align:center;">
          <div class="mark" style="width:48px; height:48px; font-size:20px; background:var(--danger-tint); color:var(--danger);">!</div>
          <h1>{{ loadError() }}</h1>
          <p>Please return to your dashboard and try again, or contact the recruiter if this keeps happening.</p>
          <button class="btn-primary" style="width:100%;" (click)="exitInterview('/candidate/dashboard')">Back to dashboard</button>
        </div>
      </div>

      <ng-container *ngIf="details() as d">
        <!-- INTRO / SETUP -->
        <div class="iv-intro-screen" *ngIf="viewState() === 'intro'">
          <div class="iv-intro-card">
            <div class="mark" style="width:48px; height:48px; font-size:20px;">AI</div>
            <h1>AI Interview — {{ d.jobTitle }}</h1>
            <p>Estimated {{ d.estimatedDurationMinutes }} minutes · {{ d.totalQuestions }} questions</p>

            <ng-container *ngIf="d.status === 'COMPLETED'; else notCompleted">
              <p style="color:var(--ink-soft);">You've already completed this interview.</p>
              <button class="btn-primary" style="width:100%;" (click)="viewPastResult()">View my result</button>
              <button class="btn-ghost" style="width:100%; margin-top:8px;" (click)="exitInterview('/candidate/applications')">Back to applications</button>
            </ng-container>
            <ng-template #notCompleted>
              <ng-container *ngIf="d.status === 'EXPIRED'; else canStart">
                <p style="color:var(--danger);">This interview invitation has expired.</p>
                <button class="btn-ghost" style="width:100%;" (click)="exitInterview('/candidate/applications')">Back to applications</button>
              </ng-container>
              <ng-template #canStart>
                <ul class="iv-tips">
                  <li>Find a quiet space with a stable internet connection</li>
                  <li>You can answer by chat, voice or video — pick what's comfortable</li>
                  <li>There's no need to rush; take a moment to think before answering</li>
                </ul>
                <div class="iv-mode-select">
                  <button class="iv-mode-card" [class.active]="selectedMode() === 'CHAT'" (click)="selectedMode.set(InterviewModeEnum.CHAT)">💬<br>Chat</button>
                  <button class="iv-mode-card" [class.active]="selectedMode() === 'VOICE'" (click)="selectedMode.set(InterviewModeEnum.VOICE)">🎙️<br>Voice</button>
                  <button class="iv-mode-card" [class.active]="selectedMode() === 'VIDEO'" (click)="selectedMode.set(InterviewModeEnum.VIDEO)">🎥<br>Video</button>
                </div>
                <p *ngIf="permissionError()" style="color:var(--danger); font-size:12px; margin-bottom:10px;">{{ permissionError() }}</p>
                <button class="btn-primary" style="width:100%;" [disabled]="starting()" (click)="beginInterview()">{{ starting() ? 'Starting…' : 'Start interview' }}</button>
                <button class="btn-ghost" style="width:100%; margin-top:8px;" (click)="exitInterview('/candidate/dashboard')">Not now</button>
              </ng-template>
            </ng-template>
          </div>
        </div>

        <!-- LIVE INTERVIEW -->
        <div class="iv-live-screen" style="display:flex;" *ngIf="viewState() === 'live'">
          <div class="iv-header">
            <span class="badge stage">Question {{ question()?.questionOrder }} of {{ question()?.totalQuestions }}</span>
            <div class="iv-progress-track"><div class="iv-progress-fill" [style.width.%]="progressPercent()"></div></div>
            <div class="iv-header-right">
              <span class="recording-indicator"><span class="rec-dot"></span>REC {{ timerLabel() }}</span>
              <button class="btn-ghost" style="padding:7px 14px; font-size:12px;" (click)="confirmEndInterview()">End interview</button>
            </div>
          </div>

          <div class="iv-body">
            <!-- CHAT MODE -->
            <div class="iv-panel" *ngIf="selectedMode() === 'CHAT'">
              <div class="chat-area" style="flex:1; margin-bottom:14px;">
                <ng-container *ngFor="let msg of chatMessages()">
                  <div class="bubble-ai" *ngIf="msg.who === 'ai'"><div class="ai-text">{{ msg.text }}</div></div>
                  <div class="bubble-user" *ngIf="msg.who === 'user'">{{ msg.text }}</div>
                </ng-container>
              </div>
              <div class="chat-input-row">
                <input [value]="chatInput()" (input)="chatInput.set($any($event.target).value)" placeholder="Type your answer…" (keydown.enter)="submitChatAnswer()">
                <button class="btn-primary-sm" [disabled]="submitting()" (click)="submitChatAnswer()">Send</button>
              </div>
            </div>

            <!-- VOICE MODE -->
            <div class="iv-panel" *ngIf="selectedMode() === 'VOICE'">
              <div class="iv-question-card"><div class="ai-text">{{ question()?.questionText }}</div></div>
              <div class="iv-voice-stage">
                <div class="waveform" [class.active]="recording()">
                  <span *ngFor="let b of waveBars"></span>
                </div>
                <button class="mic-button" [class.recording]="recording()" (click)="toggleVoiceRecording()" aria-label="Start recording answer">
                  <svg viewBox="0 0 24 24"><path d="M12 15a3 3 0 003-3V6a3 3 0 00-6 0v6a3 3 0 003 3z"/><path d="M19 11a7 7 0 01-14 0M12 19v3"/></svg>
                </button>
                <div class="iv-voice-status">{{ voiceStatus() }}</div>
              </div>
              <div class="iv-live-transcript">
                <div class="lt-line" *ngFor="let line of voiceTranscriptLines()"><b>You:</b> {{ line }}</div>
              </div>
            </div>

            <!-- VIDEO MODE -->
            <div class="iv-panel" *ngIf="selectedMode() === 'VIDEO'">
              <div class="iv-video-grid">
                <div class="iv-video-self">
                  <span class="recording-indicator" style="position:absolute; top:12px; left:12px;" *ngIf="videoStreamActive()"><span class="rec-dot"></span>REC</span>
                  <video #selfVideo autoplay muted playsinline style="width:100%; height:100%; border-radius:12px; object-fit:cover;" [style.display]="videoStreamActive() ? 'block' : 'none'"></video>
                  <ng-container *ngIf="!videoStreamActive()">
                    <svg viewBox="0 0 24 24"><rect x="2" y="6" width="15" height="12" rx="2"/><path d="M17 10l5-3v10l-5-3"/></svg>
                    <span>{{ permissionError() ? 'Camera unavailable' : 'Your camera' }}</span>
                  </ng-container>
                </div>
                <div class="iv-question-card" style="margin-bottom:0; display:flex; flex-direction:column; justify-content:center;">
                  <div class="badge stage" style="margin-bottom:8px; width:fit-content;">AI Interviewer</div>
                  <div class="ai-text">{{ question()?.questionText }}</div>
                </div>
              </div>
              <div class="iv-video-controls">
                <button class="btn-ghost" (click)="toggleMute()">{{ muted() ? 'Unmute' : 'Mute' }}</button>
                <button class="btn-primary-sm" [disabled]="submitting()" (click)="submitVideoAnswer()">Next question</button>
              </div>
            </div>
          </div>
        </div>

        <!-- COMPLETION -->
        <div class="iv-completion-screen" style="display:flex;" *ngIf="viewState() === 'completion'">
          <div class="iv-intro-card" style="text-align:center;" *ngIf="!showTranscript()">
            <div class="sent-icon"><svg viewBox="0 0 24 24"><path d="M5 13l4 4L19 7"/></svg></div>
            <h1>Interview completed</h1>
            <p>{{ result()?.message || "Thanks for taking the time. Our AI is analyzing your responses — you'll see your status update within a few hours." }}</p>
            <button class="btn-primary" style="width:100%; margin-bottom:8px;" (click)="showTranscript.set(true)">View my transcript</button>
            <button class="btn-ghost" style="width:100%;" (click)="exitInterview('/candidate/applications')">Back to applications</button>
          </div>

          <div class="iv-intro-card" style="max-width:560px;" *ngIf="showTranscript()">
            <h1 style="text-align:center;">Interview transcript</h1>
            <div class="iv-live-transcript" style="max-height:340px; overflow-y:auto; border-top:none; padding-top:0;">
              <div class="lt-line" *ngFor="let turn of result()?.transcript"><b>{{ turn.speaker === 'AI' ? 'AI' : 'You' }}:</b> {{ turn.text }}</div>
              <div class="empty-state" *ngIf="!result()?.transcript?.length"><p>No transcript available.</p></div>
            </div>
            <button class="btn-ghost" style="width:100%; margin-top:16px;" (click)="showTranscript.set(false)">Back</button>
            <button class="btn-primary" style="width:100%; margin-top:8px;" (click)="exitInterview('/candidate/applications')">Back to applications</button>
          </div>
        </div>
      </ng-container>
    </div>
  `,
  styles: []
})
export class CandidateAiInterviewComponent implements OnInit, OnDestroy {
  InterviewModeEnum = InterviewMode;

  applicationId = '';
  viewState = signal<'intro' | 'live' | 'completion'>('intro');
  selectedMode = signal<InterviewMode>(InterviewMode.CHAT);
  loadError = signal<string | null>(null);
  permissionError = signal<string | null>(null);
  starting = signal(false);
  submitting = signal(false);
  showTranscript = signal(false);

  chatMessages = signal<ChatBubble[]>([]);
  chatInput = signal('');

  recording = signal(false);
  voiceStatus = signal('Tap to answer');
  voiceTranscriptLines = signal<string[]>([]);
  private voiceAnswerText = '';

  videoStreamActive = signal(false);
  muted = signal(false);
  private videoAnswerText = '';

  timerSeconds = signal(0);
  private timerHandle: ReturnType<typeof setInterval> | null = null;
  private mediaStream: MediaStream | null = null;
  private speechRecognition: any = null;

  waveBars = Array.from({ length: 10 });

  details = computed(() => this.state.candidateInterviewDetails());
  question = computed(() => this.state.candidateInterviewQuestion());
  result = computed(() => this.state.candidateInterviewResult());

  progressPercent = computed(() => {
    const q = this.question();
    if (!q) return 0;
    return (q.questionOrder / q.totalQuestions) * 100;
  });

  timerLabel = computed(() => {
    const s = this.timerSeconds();
    const m = Math.floor(s / 60).toString().padStart(2, '0');
    const sec = (s % 60).toString().padStart(2, '0');
    return `${m}:${sec}`;
  });

  constructor(public state: AppStateService, private route: ActivatedRoute, private router: Router) {}

  ngOnInit() {
    this.applicationId = this.route.snapshot.paramMap.get('applicationId') || '';
    this.state.clearCandidateInterviewState();
    if (!this.applicationId) {
      this.loadError.set('No interview was specified.');
      return;
    }
    this.state.loadCandidateInterviewDetails(this.applicationId);
  }

  ngOnDestroy() {
    this.stopTimer();
    this.stopMediaStream();
    this.stopSpeechRecognition();
  }

  private startTimer() {
    this.timerSeconds.set(0);
    this.stopTimer();
    this.timerHandle = setInterval(() => this.timerSeconds.update((s) => s + 1), 1000);
  }

  private stopTimer() {
    if (this.timerHandle) {
      clearInterval(this.timerHandle);
      this.timerHandle = null;
    }
  }

  private stopMediaStream() {
    this.mediaStream?.getTracks().forEach((t) => t.stop());
    this.mediaStream = null;
    this.videoStreamActive.set(false);
  }

  private stopSpeechRecognition() {
    try {
      this.speechRecognition?.stop();
    } catch {
      /* already stopped */
    }
    this.speechRecognition = null;
  }

  async beginInterview() {
    this.permissionError.set(null);
    const mode = this.selectedMode();

    if (mode === InterviewMode.VIDEO) {
      try {
        this.mediaStream = await navigator.mediaDevices.getUserMedia({ video: true, audio: true });
        this.videoStreamActive.set(true);
      } catch {
        this.permissionError.set('Camera/microphone access was denied. You can still continue in Chat mode.');
      }
    } else if (mode === InterviewMode.VOICE) {
      try {
        this.mediaStream = await navigator.mediaDevices.getUserMedia({ audio: true });
      } catch {
        this.permissionError.set('Microphone access was denied. You can still continue in Chat mode.');
        return;
      }
    }

    this.starting.set(true);
    this.state.startCandidateInterview({ applicationId: this.applicationId, mode }).subscribe({
      next: (res) => {
        this.starting.set(false);
        this.viewState.set('live');
        this.chatMessages.set([{ who: 'ai', text: res.data.questionText }]);
        this.startTimer();
        if (mode === InterviewMode.VIDEO) this.attachVideoStream();
      },
      error: (err) => {
        this.starting.set(false);
        this.state.showToast(err?.error?.message ?? 'Could not start the interview.');
      },
    });
  }

  private attachVideoStream() {
    setTimeout(() => {
      const video = document.querySelector('video') as HTMLVideoElement | null;
      if (video && this.mediaStream) video.srcObject = this.mediaStream;
    });
  }

  submitChatAnswer() {
    const text = this.chatInput().trim();
    if (!text || this.submitting()) return;
    this.chatMessages.update((list) => [...list, { who: 'user', text }]);
    this.chatInput.set('');
    this.sendAnswer(text);
  }

  toggleVoiceRecording() {
    const nowRecording = !this.recording();
    this.recording.set(nowRecording);

    if (nowRecording) {
      this.voiceStatus.set('Recording… tap to stop');
      this.voiceAnswerText = '';
      this.startSpeechRecognition((finalText) => {
        this.voiceAnswerText = finalText;
      });
    } else {
      this.voiceStatus.set('Tap to answer');
      this.stopSpeechRecognition();
      const text = this.voiceAnswerText.trim();
      if (!text) {
        this.state.showToast('No speech was detected — please try again.');
        return;
      }
      this.voiceTranscriptLines.update((lines) => [...lines, text]);
      this.sendAnswer(text);
    }
  }

  toggleMute() {
    if (!this.mediaStream) return;
    const next = !this.muted();
    this.muted.set(next);
    this.mediaStream.getAudioTracks().forEach((t) => (t.enabled = !next));
  }

  submitVideoAnswer() {
    if (this.submitting()) return;
    this.stopSpeechRecognition();
    const text = this.videoAnswerText.trim();
    this.videoAnswerText = '';
    if (!text) {
      this.state.showToast('No speech was detected for this answer — please try again.');
      if (this.selectedMode() === InterviewMode.VIDEO) this.startVideoSpeechCapture();
      return;
    }
    this.sendAnswer(text);
  }

  private startVideoSpeechCapture() {
    this.startSpeechRecognition((finalText) => {
      this.videoAnswerText = finalText;
    });
  }

  private startSpeechRecognition(onResult: (text: string) => void) {
    const SpeechRecognitionCtor = (window as any).SpeechRecognition || (window as any).webkitSpeechRecognition;
    if (!SpeechRecognitionCtor) {
      this.state.showToast('Voice transcription is not supported in this browser — try Chrome, or switch to Chat mode.');
      return;
    }
    const recognition = new SpeechRecognitionCtor();
    recognition.continuous = true;
    recognition.interimResults = true;
    recognition.lang = 'en-US';
    let finalTranscript = '';
    recognition.onresult = (event: any) => {
      let interim = '';
      for (let i = event.resultIndex; i < event.results.length; i++) {
        const transcript = event.results[i][0].transcript;
        if (event.results[i].isFinal) finalTranscript += transcript + ' ';
        else interim += transcript;
      }
      onResult(finalTranscript || interim);
    };
    recognition.onerror = () => {
      /* left to the user to retry via the mic button */
    };
    recognition.start();
    this.speechRecognition = recognition;
  }

  private sendAnswer(answerText: string) {
    const q = this.question();
    if (!q) return;
    this.submitting.set(true);
    this.state.submitCandidateAnswer({
      applicationId: this.applicationId,
      questionId: q.questionId,
      answerText,
      responseTimeSeconds: this.timerSeconds(),
    }).subscribe({
      next: (res) => {
        this.submitting.set(false);
        if (res.data.interviewCompleted) {
          this.finishInterview();
        } else if (this.selectedMode() === InterviewMode.CHAT) {
          this.chatMessages.update((list) => [...list, { who: 'ai', text: res.data.questionText }]);
        } else if (this.selectedMode() === InterviewMode.VIDEO) {
          this.startVideoSpeechCapture();
        }
      },
      error: (err) => {
        this.submitting.set(false);
        this.state.showToast(err?.error?.message ?? 'Could not submit your answer.');
      },
    });
  }

  confirmEndInterview() {
    if (confirm('End this interview now? Your progress on the remaining questions will be lost.')) {
      this.state.finishCandidateInterviewEarly(this.applicationId).subscribe({
        next: () => {
          this.state.showToast('Interview ended early');
          this.finishInterview();
        },
        error: () => {
          this.state.showToast('Interview ended early');
          this.finishInterview();
        },
      });
    }
  }

  private finishInterview() {
    this.stopTimer();
    this.stopMediaStream();
    this.stopSpeechRecognition();
    this.state.loadCandidateInterviewResult(this.applicationId);
    this.viewState.set('completion');
  }

  viewPastResult() {
    this.state.loadCandidateInterviewResult(this.applicationId);
    this.viewState.set('completion');
  }

  exitInterview(destination: string) {
    this.stopTimer();
    this.stopMediaStream();
    this.stopSpeechRecognition();
    this.router.navigateByUrl(destination);
  }
}
