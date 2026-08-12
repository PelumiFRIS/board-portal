import { apiClient } from "./client";
import type { CreateResolutionPayload, ResolutionDetail, ResolutionSummary, VoteChoice } from "./types";

export async function listResolutions(meetingId?: string): Promise<ResolutionSummary[]> {
  const { data } = await apiClient.get<ResolutionSummary[]>("/api/resolutions", { params: { meetingId } });
  return data;
}

export async function getResolution(id: string): Promise<ResolutionDetail> {
  const { data } = await apiClient.get<ResolutionDetail>(`/api/resolutions/${id}`);
  return data;
}

export async function createResolution(payload: CreateResolutionPayload): Promise<ResolutionSummary> {
  const { data } = await apiClient.post<ResolutionSummary>("/api/resolutions", payload);
  return data;
}

export async function openResolution(id: string): Promise<ResolutionSummary> {
  const { data } = await apiClient.patch<ResolutionSummary>(`/api/resolutions/${id}/open`);
  return data;
}

export async function closeResolution(id: string): Promise<ResolutionSummary> {
  const { data } = await apiClient.patch<ResolutionSummary>(`/api/resolutions/${id}/close`);
  return data;
}

export async function castVote(id: string, choice: VoteChoice): Promise<ResolutionSummary> {
  const { data } = await apiClient.post<ResolutionSummary>(`/api/resolutions/${id}/votes`, { choice });
  return data;
}

export async function deleteResolution(id: string): Promise<void> {
  await apiClient.delete(`/api/resolutions/${id}`);
}
