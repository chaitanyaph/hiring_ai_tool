// Mirrors resume-service's DTOs (dto/request, dto/response).

export enum ResumeStatus {
  ACTIVE = 'ACTIVE',
  DELETED = 'DELETED',
  ARCHIVED = 'ARCHIVED',
}

export interface RenameResumeRequest {
  displayName: string;
}

export interface ResumeResponse {
  id: string;
  candidateId: string;
  displayName: string;
  originalFileName: string;
  fileExtension: string;
  mimeType: string;
  fileSize: number;
  defaultResume: boolean;
  status: ResumeStatus;
  uploadedAt: string;
  createdAt: string;
  updatedAt: string;
  version: number;
}
