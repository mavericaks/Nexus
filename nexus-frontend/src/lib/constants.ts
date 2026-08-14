// ─── API Constants ──────────────────────────────────────────────────
export const API_BASE_URL = process.env.NEXT_PUBLIC_API_URL || 'https://nexus-tep5.onrender.com';

// ─── Status Configuration ───────────────────────────────────────────
export const STATUS_CONFIG: Record<string, { label: string; color: string; badgeClass: string }> = {
  NEW:            { label: 'New',            color: '#64748b', badgeClass: 'badge--status-new' },
  CLASSIFIED:     { label: 'Classified',     color: '#818cf8', badgeClass: 'badge--status-classified' },
  AI_DRAFTED:     { label: 'AI Drafted',     color: '#a78bfa', badgeClass: 'badge--status-ai-drafted' },
  AUTO_RESOLVED:  { label: 'Auto Resolved',  color: '#34d399', badgeClass: 'badge--status-auto-resolved' },
  ESCALATED:      { label: 'Escalated',      color: '#fb923c', badgeClass: 'badge--status-escalated' },
  IN_PROGRESS:    { label: 'In Progress',    color: '#60a5fa', badgeClass: 'badge--status-in-progress' },
  RESOLVED:       { label: 'Resolved',       color: '#4ade80', badgeClass: 'badge--status-resolved' },
  CLOSED:         { label: 'Closed',         color: '#475569', badgeClass: 'badge--status-closed' },
};

export const PRIORITY_CONFIG: Record<string, { label: string; color: string; badgeClass: string }> = {
  LOW:      { label: 'Low',      color: '#6ee7b7', badgeClass: 'badge--priority-low' },
  MEDIUM:   { label: 'Medium',   color: '#fbbf24', badgeClass: 'badge--priority-medium' },
  HIGH:     { label: 'High',     color: '#f97316', badgeClass: 'badge--priority-high' },
  CRITICAL: { label: 'Critical', color: '#ef4444', badgeClass: 'badge--priority-critical' },
};

export const CATEGORY_OPTIONS = [
  'BILLING', 'TECHNICAL', 'ACCOUNT', 'FEATURE_REQUEST', 'GENERAL'
] as const;

export const PRIORITY_OPTIONS = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'] as const;

export const STATUS_OPTIONS = [
  'NEW', 'CLASSIFIED', 'AI_DRAFTED', 'AUTO_RESOLVED',
  'ESCALATED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'
] as const;
