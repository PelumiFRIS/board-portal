import { apiClient } from "./client";
import type { CreateMeetingTypePayload, MeetingTypeSummary } from "./types";

export async function listMeetingTypes(): Promise<MeetingTypeSummary[]> {
  const { data } = await apiClient.get<MeetingTypeSummary[]>("/api/meeting-types");
  return data;
}

export async function createMeetingType(payload: CreateMeetingTypePayload): Promise<MeetingTypeSummary> {
  const { data } = await apiClient.post<MeetingTypeSummary>("/api/meeting-types", payload);
  return data;
}

export async function deleteMeetingType(id: string): Promise<void> {
  await apiClient.delete(`/api/meeting-types/${id}`);
}
