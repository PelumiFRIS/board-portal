import { apiClient } from "./client";
import type {
  AgendaItem,
  CreateAgendaItemPayload,
  CreateMeetingPayload,
  MeetingDetail,
  MeetingSummary,
  UpdateAgendaItemPayload,
  UpdateMeetingPayload,
} from "./types";

export async function listMeetings(): Promise<MeetingSummary[]> {
  const { data } = await apiClient.get<MeetingSummary[]>("/api/meetings");
  return data;
}

export async function getMeeting(id: string): Promise<MeetingDetail> {
  const { data } = await apiClient.get<MeetingDetail>(`/api/meetings/${id}`);
  return data;
}

export async function createMeeting(payload: CreateMeetingPayload): Promise<MeetingSummary> {
  const { data } = await apiClient.post<MeetingSummary>("/api/meetings", payload);
  return data;
}

export async function updateMeeting(id: string, payload: UpdateMeetingPayload): Promise<MeetingDetail> {
  const { data } = await apiClient.patch<MeetingDetail>(`/api/meetings/${id}`, payload);
  return data;
}

export async function addAgendaItem(meetingId: string, payload: CreateAgendaItemPayload): Promise<AgendaItem> {
  const { data } = await apiClient.post<AgendaItem>(`/api/meetings/${meetingId}/agenda-items`, payload);
  return data;
}

export async function updateAgendaItem(
  meetingId: string,
  itemId: string,
  payload: UpdateAgendaItemPayload,
): Promise<AgendaItem> {
  const { data } = await apiClient.patch<AgendaItem>(`/api/meetings/${meetingId}/agenda-items/${itemId}`, payload);
  return data;
}

export async function deleteAgendaItem(meetingId: string, itemId: string): Promise<void> {
  await apiClient.delete(`/api/meetings/${meetingId}/agenda-items/${itemId}`);
}

export async function downloadMeetingIcs(id: string, title: string): Promise<void> {
  const response = await apiClient.get(`/api/meetings/${id}/ics`, { responseType: "blob" });
  const url = URL.createObjectURL(response.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = `${title.replace(/[^a-z0-9]+/gi, "-").toLowerCase()}.ics`;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export interface CalendarTokenResponse {
  token: string;
}

export async function getCalendarToken(): Promise<CalendarTokenResponse> {
  const { data } = await apiClient.get<CalendarTokenResponse>("/api/users/me/calendar-token");
  return data;
}

export async function regenerateCalendarToken(): Promise<CalendarTokenResponse> {
  const { data } = await apiClient.post<CalendarTokenResponse>("/api/users/me/calendar-token/regenerate");
  return data;
}

export function buildCalendarFeedUrl(token: string): string {
  const base = apiClient.defaults.baseURL ?? "";
  return `${base}/api/calendar/feed/${token}`;
}
