import { apiClient } from "./client";
import type { ActionItemStatus, ActionItemSummary, CreateActionItemPayload } from "./types";

export async function listActionItems(meetingId?: string): Promise<ActionItemSummary[]> {
  const { data } = await apiClient.get<ActionItemSummary[]>("/api/action-items", { params: { meetingId } });
  return data;
}

export async function createActionItem(payload: CreateActionItemPayload): Promise<ActionItemSummary> {
  const { data } = await apiClient.post<ActionItemSummary>("/api/action-items", payload);
  return data;
}

export async function updateActionItemStatus(id: string, status: ActionItemStatus): Promise<ActionItemSummary> {
  const { data } = await apiClient.patch<ActionItemSummary>(`/api/action-items/${id}/status`, { status });
  return data;
}

export async function deleteActionItem(id: string): Promise<void> {
  await apiClient.delete(`/api/action-items/${id}`);
}

export async function downloadActionItemsCsv(): Promise<void> {
  const response = await apiClient.get("/api/action-items/export", { responseType: "blob" });
  const url = URL.createObjectURL(response.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `action-items-${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
