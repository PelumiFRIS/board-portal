import type { MeetingStatus, ResolutionOutcome, ResolutionStatus, UserStatus } from "../api/types";

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
};

export function StatusBadge({ status }: { status: UserStatus | MeetingStatus | ResolutionStatus | ResolutionOutcome }) {
  return <span className={`badge ${CLASS_MAP[status] ?? "badge-disabled"}`}>{status}</span>;
}
