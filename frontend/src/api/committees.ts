import { apiClient } from "./client";
import type { CommitteeSummary, CreateCommitteePayload, UpdateCommitteePayload } from "./types";

export async function listCommittees(): Promise<CommitteeSummary[]> {
  const { data } = await apiClient.get<CommitteeSummary[]>("/api/committees");
  return data;
}

export async function createCommittee(payload: CreateCommitteePayload): Promise<CommitteeSummary> {
  const { data } = await apiClient.post<CommitteeSummary>("/api/committees", payload);
  return data;
}

export async function updateCommittee(id: string, payload: UpdateCommitteePayload): Promise<CommitteeSummary> {
  const { data } = await apiClient.patch<CommitteeSummary>(`/api/committees/${id}`, payload);
  return data;
}

export async function deleteCommittee(id: string): Promise<void> {
  await apiClient.delete(`/api/committees/${id}`);
}

export async function addCommitteeMember(committeeId: string, userId: string): Promise<CommitteeSummary> {
  const { data } = await apiClient.post<CommitteeSummary>(`/api/committees/${committeeId}/members`, { userId });
  return data;
}

export async function removeCommitteeMember(committeeId: string, userId: string): Promise<CommitteeSummary> {
  const { data } = await apiClient.delete<CommitteeSummary>(`/api/committees/${committeeId}/members/${userId}`);
  return data;
}

export async function setCommitteeChair(committeeId: string, userId: string): Promise<CommitteeSummary> {
  const { data } = await apiClient.patch<CommitteeSummary>(`/api/committees/${committeeId}/members/${userId}/chair`);
  return data;
}
