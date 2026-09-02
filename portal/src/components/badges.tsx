import type { CasePriority, CaseStatus } from '../types';

const STATUS_CLASS: Record<CaseStatus, string> = {
  DRAFT: 'badge-normal',
  OPEN: 'badge-open',
  IN_PROGRESS: 'badge-progress',
  ON_HOLD: 'badge-hold',
  COMPLETED: 'badge-progress',
  CLOSED: 'badge-closed',
  CANCELLED: 'badge-closed',
};

export function StatusBadge({ status }: { status: CaseStatus }) {
  return <span className={`badge ${STATUS_CLASS[status]}`}>{status.replace('_', ' ')}</span>;
}

export function PriorityBadge({ priority, emergency }: { priority: CasePriority; emergency?: boolean }) {
  if (priority === 'EMERGENCY' || emergency) return <span className="badge badge-emergency">● EMERGENCY</span>;
  if (priority === 'HIGH') return <span className="badge badge-high">HIGH</span>;
  return <span className="badge badge-normal">{priority}</span>;
}
