import { apiClient } from "./client";
import type { AuditLogEntry } from "./types";

export async function listAuditLogs(): Promise<AuditLogEntry[]> {
  const { data } = await apiClient.get<AuditLogEntry[]>("/api/audit-logs");
  return data;
}

export async function downloadAuditLogCsv(): Promise<void> {
  const response = await apiClient.get("/api/audit-logs/export", { responseType: "blob" });
  const url = URL.createObjectURL(response.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `audit-trail-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
