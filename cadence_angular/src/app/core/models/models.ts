export enum UserRole {
  team = 'team',
  candidate = 'candidate'
}

export interface UserModel {
  name: string;
  email: string;
  role: UserRole;
}

export interface Job {
  id: string;
  title: string;
  department: string;
  location: string;
  workType: string; // Hybrid, Remote, On-site
  empType: string;  // Full-time, Contract, Part-time
  status: string;   // published, draft, archived
  openings?: number;
  applicants?: number;
  posted?: string;
  // Only populated once the full job detail is fetched (job-service's list/search
  // endpoint returns summaries without these) -- optional rather than faked.
  experience?: string;
  salary?: string;
  description?: string;
  skills?: string[];
}

export interface Candidate {
  id: string;
  name: string;
  email: string;
  phone: string;
  currentCompany: string;
  matchScore: number;
  stage: string;       // AI Shortlisted, Interview Pending, Technical Round, HR Round, Offer Released, Consider
  sourcedFrom: string; // LinkedIn, Careers page, Referral, CSV import
  appliedDate: string;
  noticePeriod: string;
  expectedSalary: string;
  recruiterNote: string;
}

export interface Application {
  id: string;
  jobTitle: string;
  company: string;
  appliedDate: string;
  currentStage: string;
  status: string; // active, offer, rejected
  lastUpdate?: string;
}

export interface Interview {
  id: string;
  candidateName: string;
  role: string;
  type: string; // AI Interview, Technical round, HR round
  date: string;
  time: string;
  duration: string;
  panel: string[];
}

export interface Activity {
  id: string;
  text: string;
  timeAgo: string;
  dotColor: string; // teal, indigo, gold
}

export interface ChatMessage {
  id: string;
  sender: 'user' | 'ai';
  text: string;
  aiResults?: { name: string; score: string; skills: string }[];
}
