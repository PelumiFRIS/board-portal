import { apiClient } from "./client";
import type { OnboardClientPayload, OnboardClientResponse } from "./types";

export async function onboardClient(payload: OnboardClientPayload): Promise<OnboardClientResponse> {
  const { data } = await apiClient.post<OnboardClientResponse>("/api/organizations", payload);
  return data;
}
