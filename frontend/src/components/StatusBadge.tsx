import type { MeetingStatus, UserStatus } from "../api/types";

const CLASS_MAP: Record<string, string> = {
  ACTIVE: "badge-active",
  DISABLED: "badge-disabled",
  SCHEDULED: "badge-scheduled",
  COMPLETED: "badge-completed",
  CANCELLED: "badge-cancelled",
};

export function StatusBadge({ status }: { status: UserStatus | MeetingStatus }) {
  return <span className={`badge ${CLASS_MAP[status] ?? "badge-disabled"}`}>{status}</span>;
}
