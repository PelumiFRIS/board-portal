import axios from "axios";

export const TOKEN_STORAGE_KEY = "board-portal.accessToken";
export const SESSION_EXPIRED_KEY = "board-portal.sessionExpired";

export const apiClient = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL ?? "http://localhost:8080",
});

apiClient.interceptors.request.use((config) => {
  const token = localStorage.getItem(TOKEN_STORAGE_KEY);
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  return config;
});

apiClient.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      const hadToken = Boolean(localStorage.getItem(TOKEN_STORAGE_KEY));
      localStorage.removeItem(TOKEN_STORAGE_KEY);
      if (hadToken && window.location.pathname !== "/login") {
        sessionStorage.setItem(SESSION_EXPIRED_KEY, "1");
        window.location.href = "/login";
      }
    }
    return Promise.reject(error);
  },
);

export function extractErrorMessage(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const body = error.response?.data as { message?: string; fieldErrors?: string[] } | undefined;
    if (body?.fieldErrors?.length) {
      return body.fieldErrors.join(", ");
    }
    if (body?.message) {
      return body.message;
    }
  }
  return "Something went wrong. Please try again.";
}
