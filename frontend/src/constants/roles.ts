import type { Role } from "../api/types";

export const ROLE_OPTIONS: Role[] = ["BOARD_MEMBER", "COMPANY_SECRETARY", "ADMIN"];

export const ROLE_LABELS: Record<Role, string> = {
  BOARD_MEMBER: "Board Member",
  COMPANY_SECRETARY: "Company Secretary",
  ADMIN: "Admin",
};
