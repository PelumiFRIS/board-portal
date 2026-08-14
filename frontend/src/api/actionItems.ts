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
