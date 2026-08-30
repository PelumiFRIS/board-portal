import { apiClient } from "./client";
import type { ApiKeySummary, CreateApiKeyPayload, CreateApiKeyResponse } from "./types";

export async function listApiKeys(): Promise<ApiKeySummary[]> {
  const { data } = await apiClient.get<ApiKeySummary[]>("/api/api-keys");
  return data;
}

export async function createApiKey(payload: CreateApiKeyPayload): Promise<CreateApiKeyResponse> {
  const { data } = await apiClient.post<CreateApiKeyResponse>("/api/api-keys", payload);
  return data;
}

export async function revokeApiKey(id: string): Promise<void> {
  await apiClient.delete(`/api/api-keys/${id}`);
}
