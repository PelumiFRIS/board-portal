import { apiClient } from "./client";
import type { AuthResponse, CreateUserPayload, LoginPayload, SignupPayload, UserStatus, UserSummary } from "./types";

export async function signup(payload: SignupPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/api/organizations/signup", payload);
  return data;
}

export async function login(payload: LoginPayload): Promise<AuthResponse> {
  const { data } = await apiClient.post<AuthResponse>("/api/auth/login", payload);
  return data;
}

export async function fetchCurrentUser(): Promise<UserSummary> {
  const { data } = await apiClient.get<UserSummary>("/api/users/me");
  return data;
}

export async function listOrganizationUsers(): Promise<UserSummary[]> {
  const { data } = await apiClient.get<UserSummary[]>("/api/users");
  return data;
}

export async function createUser(payload: CreateUserPayload): Promise<UserSummary> {
  const { data } = await apiClient.post<UserSummary>("/api/users", payload);
  return data;
}

export async function updateUserStatus(userId: string, status: UserStatus): Promise<UserSummary> {
  const { data } = await apiClient.patch<UserSummary>(`/api/users/${userId}`, { status });
  return data;
}
