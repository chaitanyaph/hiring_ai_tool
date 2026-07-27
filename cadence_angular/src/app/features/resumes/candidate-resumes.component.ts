import { Component, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';
import { FormsModule } from '@angular/forms';
import { AppStateService } from '../../core/services/app-state.service';
import { ResumeResponse } from '../../core/models/resume.model';

function formatFileSize(bytes: number): string {
  return bytes > 1024 * 1024 ? (bytes / (1024 * 1024)).toFixed(1) + ' MB' : Math.round(bytes / 1024) + ' KB';
}

function formatUploadedDate(iso: string): string {
  return new Date(iso).toLocaleDateString('en-US', { month: 'short', day: 'numeric', year: 'numeric' });
}

@Component({
  selector: 'app-candidate-resumes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  template: `
    <section class="section csection active" id="csec-resumes">
      <!-- Page Head -->
      <div class="page-head">
        <div>
          <h1>My resumes</h1>
          <p>Upload up to 3 resumes — recruiters always see your default resume first.</p>
        </div>
        <div class="page-head-actions">
          <button class="btn-primary-sm" [disabled]="state.candidateResumes().length >= 3" (click)="openUploadModal()">
            <svg viewBox="0 0 24 24" style="width:14px; height:14px; stroke:currentColor; stroke-width:2.2; fill:none; margin-right:4px;"><path d="M12 5v14M5 12h14"/></svg>
            Upload resume
          </button>
        </div>
      </div>

      <div class="resume-limit-banner" *ngIf="state.candidateResumes().length >= 3">
        <span><b>Resume limit reached (3/3).</b> Delete a resume before uploading a new one.</span>
      </div>

      <!-- Resume cards -->
      <div class="resume-grid" *ngIf="state.candidateResumes().length > 0">
        <div class="resume-card" *ngFor="let resume of state.candidateResumes()" [ngClass]="{ 'is-default': resume.defaultResume }">
          <span class="resume-default-tag" *ngIf="resume.defaultResume">
            <span class="badge published">Default</span>
          </span>
          <div class="resume-card-top">
            <div class="resume-icon">
              <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><path d="M14 2v6h6"/></svg>
            </div>
            <div>
              <div class="resume-card-name">{{ resume.displayName }}</div>
              <div class="resume-card-meta">{{ formatSize(resume.fileSize) }} · Uploaded {{ formatDate(resume.uploadedAt) }}</div>
            </div>
          </div>
          <div class="resume-card-actions">
            <button (click)="previewResume(resume)"><svg viewBox="0 0 24 24"><path d="M1 12s4-7 11-7 11 7 11 7-4 7-11 7-11-7-11-7z"/><circle cx="12" cy="12" r="3"/></svg>Preview</button>
            <button (click)="downloadResume(resume)"><svg viewBox="0 0 24 24"><path d="M12 3v12M7 10l5 5 5-5"/><path d="M5 21h14"/></svg>Download</button>
            <button (click)="openRenameModal(resume)"><svg viewBox="0 0 24 24"><path d="M12 20h9"/><path d="M16.5 3.5a2.1 2.1 0 013 3L7 19l-4 1 1-4z"/></svg>Rename</button>
            <button class="set-default-btn" *ngIf="!resume.defaultResume" (click)="state.setDefaultResume(resume.id)">
              <svg viewBox="0 0 24 24"><path d="M12 2l2.4 6.6L21 11l-6.6 2.4L12 20l-2.4-6.6L3 11l6.6-2.4z"/></svg>Set default
            </button>
            <button class="danger" (click)="openDeleteModal(resume)"><svg viewBox="0 0 24 24"><path d="M3 6h18M8 6V4h8v2M6 6l1 14h10l1-14"/></svg>Delete</button>
          </div>
        </div>
      </div>

      <!-- Empty State -->
      <div class="empty-state" *ngIf="state.candidateResumes().length === 0">
        <svg viewBox="0 0 24 24"><path d="M14 2H6a2 2 0 00-2 2v16a2 2 0 002 2h12a2 2 0 002-2V8z"/><path d="M14 2v6h6"/></svg>
        <p>No resumes uploaded yet</p>
        <button (click)="openUploadModal()">Upload your first resume</button>
      </div>

      <!-- Modals -->
      <!-- Upload Resume Modal Overlay -->
      <div class="modal-overlay" [ngClass]="{ 'show': showUpload() }">
        <div class="modal">
          <div class="modal-head">
            <h3>Upload resume</h3>
            <button class="modal-close" (click)="showUpload.set(false)">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <p style="font-size:13px; color:var(--ink-soft); margin-bottom:16px;">
              Select a PDF resume file to upload. Maximum file size: 5MB.
            </p>
            <div class="modal-dropzone" (click)="triggerFileInput()">
              <svg viewBox="0 0 24 24" style="width:36px; height:36px; stroke:var(--ink-faint); fill:none; stroke-width:1.5; margin-bottom:8px;">
                <path d="M21 15v4a2 2 0 01-2 2H5a2 2 0 01-2-2v-4M17 8l-5-5-5 5M12 3v12"/>
              </svg>
              <div style="font-size:13px; font-weight:500; color:var(--indigo); cursor:pointer;">Click to select file</div>
              <div style="font-size:11px; color:var(--ink-soft); margin-top:2px;">PDF format only</div>
            </div>
            <input type="file" id="resume-file-input" style="display:none;" accept=".pdf" (change)="onFileSelected($event)">
          </div>
        </div>
      </div>

      <!-- Rename Resume Modal Overlay -->
      <div class="modal-overlay" [ngClass]="{ 'show': activeRenameTarget() }">
        <div class="modal">
          <div class="modal-head">
            <h3>Rename resume</h3>
            <button class="modal-close" (click)="activeRenameTarget.set(null)">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <div class="field" style="margin-bottom:16px;">
              <label>Resume Name</label>
              <input type="text" [(ngModel)]="renameValue" placeholder="E.g. Backend Resume" style="width:100%;">
            </div>
            <div style="display:flex; justify-content:flex-end; gap:8px;">
              <button class="btn-ghost" (click)="activeRenameTarget.set(null)">Cancel</button>
              <button class="btn-primary-sm" (click)="submitRename()">Rename</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Delete Resume Modal Overlay -->
      <div class="modal-overlay" [ngClass]="{ 'show': activeDeleteTarget() }">
        <div class="modal">
          <div class="modal-head">
            <h3>Delete resume</h3>
            <button class="modal-close" (click)="activeDeleteTarget.set(null)">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body">
            <p style="font-size:13.5px; color:var(--ink-soft); margin-bottom:18px;">
              Are you sure you want to delete <b>{{ activeDeleteTarget()?.displayName }}</b>? This action cannot be undone.
            </p>
            <div style="display:flex; justify-content:flex-end; gap:8px;">
              <button class="btn-ghost" (click)="activeDeleteTarget.set(null)">Cancel</button>
              <button class="btn-primary-sm" style="background:var(--danger);" (click)="submitDelete()">Delete</button>
            </div>
          </div>
        </div>
      </div>

      <!-- Preview Modal Overlay -->
      <div class="modal-overlay" [ngClass]="{ 'show': activePreviewTarget() }">
        <div class="modal wide" style="max-width:800px; height:80vh; display:flex; flex-direction:column;">
          <div class="modal-head">
            <h3>Preview — {{ activePreviewTarget()?.displayName }}</h3>
            <button class="modal-close" (click)="activePreviewTarget.set(null)">
              <svg viewBox="0 0 24 24"><path d="M6 6l12 12M18 6L6 18"/></svg>
            </button>
          </div>
          <div class="modal-body" style="flex:1; overflow-y:auto; padding:20px; background:var(--line-soft);">
            <iframe *ngIf="previewUrl() as url" [src]="url" style="width:100%; height:100%; border:0; background:#fff;"></iframe>
            <div class="muted" style="text-align:center; padding:40px;" *ngIf="!previewUrl()">Loading preview…</div>
          </div>
        </div>
      </div>
    </section>
  `,
  styles: []
})
export class CandidateResumesComponent {
  showUpload = signal<boolean>(false);
  activeRenameTarget = signal<ResumeResponse | null>(null);
  activeDeleteTarget = signal<ResumeResponse | null>(null);
  activePreviewTarget = signal<ResumeResponse | null>(null);
  previewUrl = signal<SafeResourceUrl | null>(null);
  renameValue = '';

  formatSize = formatFileSize;
  formatDate = formatUploadedDate;

  constructor(public state: AppStateService, private sanitizer: DomSanitizer) {
    this.state.loadCandidateResumes();
  }

  openUploadModal() {
    this.showUpload.set(true);
  }

  triggerFileInput() {
    document.getElementById('resume-file-input')?.click();
  }

  onFileSelected(event: any) {
    const file: File | undefined = event.target.files[0];
    if (!file) return;
    if (file.type !== 'application/pdf') {
      this.state.showToast('Please select a valid PDF file');
      return;
    }
    if (file.size > 5 * 1024 * 1024) {
      this.state.showToast('File too large — maximum size is 5MB');
      return;
    }
    this.state.uploadResumeFile(file);
    this.showUpload.set(false);
  }

  previewResume(resume: ResumeResponse) {
    this.activePreviewTarget.set(resume);
    this.previewUrl.set(null);
    this.state.previewResume(resume.id).subscribe({
      next: (blob) => {
        const url = URL.createObjectURL(blob);
        this.previewUrl.set(this.sanitizer.bypassSecurityTrustResourceUrl(url));
      },
      error: () => {
        this.state.showToast('Could not load preview.');
        this.activePreviewTarget.set(null);
      },
    });
  }

  downloadResume(resume: ResumeResponse) {
    this.state.downloadResume(resume);
  }

  openRenameModal(resume: ResumeResponse) {
    this.activeRenameTarget.set(resume);
    this.renameValue = resume.displayName;
  }

  submitRename() {
    const target = this.activeRenameTarget();
    if (target && this.renameValue.trim()) {
      this.state.renameResume(target.id, this.renameValue.trim());
      this.activeRenameTarget.set(null);
    }
  }

  openDeleteModal(resume: ResumeResponse) {
    if (resume.defaultResume) {
      this.state.showToast('Cannot delete default resume. Set another default first.');
      return;
    }
    this.activeDeleteTarget.set(resume);
  }

  submitDelete() {
    const target = this.activeDeleteTarget();
    if (target) {
      this.state.deleteResume(target.id);
      this.activeDeleteTarget.set(null);
    }
  }
}
