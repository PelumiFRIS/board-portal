import { apiClient } from "./client";

export async function uploadUserPhoto(userId: string, file: File): Promise<void> {
  const form = new FormData();
  form.append("file", file);
  await apiClient.post(`/api/users/${userId}/photo`, form, {
    headers: { "Content-Type": "multipart/form-data" },
  });
}

export async function fetchUserPhotoBlob(userId: string): Promise<Blob> {
  const { data } = await apiClient.get(`/api/users/${userId}/photo`, { responseType: "blob" });
  return data as Blob;
}

export async function deleteUserPhoto(userId: string): Promise<void> {
  await apiClient.delete(`/api/users/${userId}/photo`);
}
