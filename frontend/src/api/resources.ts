import { apiClient } from "./client";
import type { CreateResourcePayload, ResourceSummary, UpdateResourcePayload } from "./types";

export async function listResources(): Promise<ResourceSummary[]> {
  const { data } = await apiClient.get<ResourceSummary[]>("/api/resources");
  return data;
}

export async function createResource(payload: CreateResourcePayload): Promise<ResourceSummary> {
  const { data } = await apiClient.post<ResourceSummary>("/api/resources", payload);
  return data;
}

export async function updateResource(id: string, payload: UpdateResourcePayload): Promise<ResourceSummary> {
  const { data } = await apiClient.patch<ResourceSummary>(`/api/resources/${id}`, payload);
  return data;
}

export async function deleteResource(id: string): Promise<void> {
  await apiClient.delete(`/api/resources/${id}`);
}
