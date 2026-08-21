import { apiClient } from "./client";
import type { ComplianceFilingSummary, CreateComplianceFilingPayload, UpdateComplianceFilingPayload } from "./types";

export async function listComplianceFilings(): Promise<ComplianceFilingSummary[]> {
  const { data } = await apiClient.get<ComplianceFilingSummary[]>("/api/compliance-filings");
  return data;
}

export async function createComplianceFiling(
  payload: CreateComplianceFilingPayload,
): Promise<ComplianceFilingSummary> {
  const { data } = await apiClient.post<ComplianceFilingSummary>("/api/compliance-filings", payload);
  return data;
}

export async function updateComplianceFiling(
  id: string,
  payload: UpdateComplianceFilingPayload,
): Promise<ComplianceFilingSummary> {
  const { data } = await apiClient.patch<ComplianceFilingSummary>(`/api/compliance-filings/${id}`, payload);
  return data;
}

export async function markFilingSubmitted(id: string): Promise<ComplianceFilingSummary> {
  const { data } = await apiClient.patch<ComplianceFilingSummary>(`/api/compliance-filings/${id}/submit`);
  return data;
}

export async function deleteComplianceFiling(id: string): Promise<void> {
  await apiClient.delete(`/api/compliance-filings/${id}`);
}
