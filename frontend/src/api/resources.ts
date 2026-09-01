import { apiClient } from "./client";
import type { CreateResourcePayload, ResourceSummary, UpdateResourcePayload } from "./types";

export async function listResources(): Promise<ResourceSummary[]> {
  const { data } = await apiClient.get<ResourceSummary[]>("/api/resources");
  return data;
}

export async function createResource(payload: CreateResourcePayload): Promise<ResourceSummary> {
  const form = new FormData();
  form.append("category", payload.category);
  form.append("title", payload.title);
  form.append("body", payload.body);
  if (payload.file) form.append("file", payload.file);

  const { data } = await apiClient.post<ResourceSummary>("/api/resources", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

export async function updateResource(id: string, payload: UpdateResourcePayload): Promise<ResourceSummary> {
  const { data } = await apiClient.patch<ResourceSummary>(`/api/resources/${id}`, payload);
  return data;
}

export async function deleteResource(id: string): Promise<void> {
  await apiClient.delete(`/api/resources/${id}`);
}

export async function downloadResource(id: string, fileName: string): Promise<void> {
  const response = await apiClient.get(`/api/resources/${id}/content`, { responseType: "blob" });
  const url = URL.createObjectURL(response.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
