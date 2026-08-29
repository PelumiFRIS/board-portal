import { apiClient } from "./client";
import type { DashboardStats } from "./types";

export async function getDashboardStats(): Promise<DashboardStats> {
  const { data } = await apiClient.get<DashboardStats>("/api/dashboard/stats");
  return data;
}
