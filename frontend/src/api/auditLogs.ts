import { apiClient } from "./client";
import type { AuditLogEntry } from "./types";

export async function listAuditLogs(): Promise<AuditLogEntry[]> {
  const { data } = await apiClient.get<AuditLogEntry[]>("/api/audit-logs");
  return data;
}
