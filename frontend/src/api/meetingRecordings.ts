import { apiClient } from "./client";
import type { MeetingRecordingSummary } from "./types";

export async function listMeetingRecordings(meetingId: string): Promise<MeetingRecordingSummary[]> {
  const { data } = await apiClient.get<MeetingRecordingSummary[]>(`/api/meetings/${meetingId}/recordings`);
  return data;
}

export async function uploadMeetingRecording(
  meetingId: string,
  file: Blob,
  fileName: string,
): Promise<MeetingRecordingSummary> {
  const form = new FormData();
  form.append("file", file, fileName);
  const { data } = await apiClient.post<MeetingRecordingSummary>(
    `/api/meetings/${meetingId}/recordings`,
    form,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data;
}

export async function fetchMeetingRecordingBlob(meetingId: string, recordingId: string): Promise<Blob> {
  const { data } = await apiClient.get(`/api/meetings/${meetingId}/recordings/${recordingId}/content`, {
    responseType: "blob",
  });
  return data as Blob;
}

export async function downloadMeetingRecording(meetingId: string, recordingId: string, fileName: string) {
  const blob = await fetchMeetingRecordingBlob(meetingId, recordingId);
  const url = URL.createObjectURL(blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}

export async function deleteMeetingRecording(meetingId: string, recordingId: string): Promise<void> {
  await apiClient.delete(`/api/meetings/${meetingId}/recordings/${recordingId}`);
}
