import { apiClient } from "./client";
import type { DocumentCategory, DocumentDetail, DocumentSummary } from "./types";

export interface ListDocumentsParams {
  meetingId?: string;
  category?: DocumentCategory;
  committeeId?: string;
}

export async function listDocuments(params: ListDocumentsParams = {}): Promise<DocumentSummary[]> {
  const { data } = await apiClient.get<DocumentSummary[]>("/api/documents", { params });
  return data;
}

export async function getDocument(id: string): Promise<DocumentDetail> {
  const { data } = await apiClient.get<DocumentDetail>(`/api/documents/${id}`);
  return data;
}

export async function updateDocumentRetention(id: string, retentionUntil: string | null): Promise<DocumentSummary> {
  const { data } = await apiClient.patch<DocumentSummary>(`/api/documents/${id}/retention`, { retentionUntil });
  return data;
}

export async function signDocument(id: string): Promise<DocumentSummary> {
  const { data } = await apiClient.post<DocumentSummary>(`/api/documents/${id}/sign`);
  return data;
}

export interface UploadDocumentPayload {
  file: File;
  title: string;
  description?: string;
  category: DocumentCategory;
  meetingId?: string;
  committeeId?: string;
}

export async function uploadDocument(payload: UploadDocumentPayload): Promise<DocumentSummary> {
  const form = new FormData();
  form.append("file", payload.file);
  form.append("title", payload.title);
  form.append("category", payload.category);
  if (payload.description) form.append("description", payload.description);
  if (payload.meetingId) form.append("meetingId", payload.meetingId);
  if (payload.committeeId) form.append("committeeId", payload.committeeId);

  const { data } = await apiClient.post<DocumentSummary>("/api/documents", form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
  return data;
}

export async function deleteDocument(id: string): Promise<void> {
  await apiClient.delete(`/api/documents/${id}`);
}

export async function downloadDocument(id: string, fileName: string): Promise<void> {
  const response = await apiClient.get(`/api/documents/${id}/content`, { responseType: "blob" });
  const url = URL.createObjectURL(response.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
