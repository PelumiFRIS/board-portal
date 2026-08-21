import type {
  ActionItemStatus,
  FilingStatus,
  MeetingStatus,
  ResolutionOutcome,
  ResolutionStatus,
  UserStatus,
} from "../api/types";

const CLASS_MAP: Record<string, string> = {
  ACTIVE: "badge-active",
  DISABLED: "badge-disabled",
  SCHEDULED: "badge-scheduled",
  COMPLETED: "badge-completed",
  CANCELLED: "badge-cancelled",
  DRAFT: "badge-disabled",
  OPEN: "badge-scheduled",
  CLOSED: "badge-completed",
  PASSED: "badge-active",
  FAILED: "badge-cancelled",
  DONE: "badge-active",
  PENDING: "badge-scheduled",
  SUBMITTED: "badge-active",
  OVERDUE: "badge-cancelled",
};

const LABEL_MAP: Record<string, string> = {
  OVERDUE: "Overdue",
};

export function StatusBadge({
  status,
}: {
  status: UserStatus | MeetingStatus | ResolutionStatus | ResolutionOutcome | ActionItemStatus | FilingStatus | "OVERDUE";
}) {
  return <span className={`badge ${CLASS_MAP[status] ?? "badge-disabled"}`}>{LABEL_MAP[status] ?? status}</span>;
}
