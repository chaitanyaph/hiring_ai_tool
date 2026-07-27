import { Job } from '../models/models';
import { EmploymentType, JobStatus, JobSummaryResponse, WorkType } from '../models/job.model';

const EMPLOYMENT_TYPE_LABEL: Record<EmploymentType, string> = {
  [EmploymentType.FULL_TIME]: 'Full-time',
  [EmploymentType.PART_TIME]: 'Part-time',
  [EmploymentType.CONTRACT]: 'Contract',
  [EmploymentType.INTERNSHIP]: 'Internship',
};

const WORK_TYPE_LABEL: Record<WorkType, string> = {
  [WorkType.HYBRID]: 'Hybrid',
  [WorkType.REMOTE]: 'Remote',
  [WorkType.ON_SITE]: 'On-site',
};

/**
 * The existing jobs-list template's status badge/actions only branch on the
 * literal strings 'published' | 'draft' | else-treated-as-archived (it predates
 * job-service's fuller 6-value status enum). PAUSED/CLOSED/EXPIRED intentionally
 * pass through as their own lowercase value rather than being force-bucketed --
 * the template's ternary falls back to showing "Archived" as generic terminal-
 * state text for anything that isn't 'published'/'draft', which degrades
 * gracefully (no crash, no misleading "Published"/"Draft" label) without
 * requiring a template change.
 */
function statusToUiValue(status: JobStatus): string {
  return status.toLowerCase();
}

function relativeTime(isoDate: string | null): string {
  if (!isoDate) return '—';
  const diffMs = Date.now() - new Date(isoDate).getTime();
  const days = Math.floor(diffMs / (1000 * 60 * 60 * 24));
  if (days <= 0) return 'Just now';
  if (days === 1) return '1 day ago';
  return `${days} days ago`;
}

export function jobSummaryToJob(summary: JobSummaryResponse): Job {
  return {
    id: summary.id,
    title: summary.title,
    department: summary.departmentName ?? '—',
    location: summary.location ?? '—',
    workType: summary.workType ? WORK_TYPE_LABEL[summary.workType] : '—',
    empType: summary.employmentType ? EMPLOYMENT_TYPE_LABEL[summary.employmentType] : '—',
    status: statusToUiValue(summary.status),
    openings: summary.numberOfOpenings ?? 0,
    applicants: summary.applicantsCount,
    posted: summary.status === JobStatus.DRAFT ? '—' : relativeTime(summary.publishedAt ?? summary.createdAt),
  };
}
