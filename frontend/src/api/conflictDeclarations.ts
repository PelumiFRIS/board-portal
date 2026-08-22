import { apiClient } from "./client";
import type { ConflictDeclarationSummary, CreateConflictDeclarationPayload } from "./types";

export async function listMyDeclarations(): Promise<ConflictDeclarationSummary[]> {
  const { data } = await apiClient.get<ConflictDeclarationSummary[]>("/api/conflict-declarations/me");
  return data;
}

export async function listAllDeclarations(): Promise<ConflictDeclarationSummary[]> {
  const { data } = await apiClient.get<ConflictDeclarationSummary[]>("/api/conflict-declarations");
  return data;
}

export async function createDeclaration(
  payload: CreateConflictDeclarationPayload,
): Promise<ConflictDeclarationSummary> {
  const { data } = await apiClient.post<ConflictDeclarationSummary>("/api/conflict-declarations", payload);
  return data;
}
