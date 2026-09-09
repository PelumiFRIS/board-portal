import { apiClient } from "./client";
import type {
  ConversationSummary,
  CreateConversationPayload,
  MessageDto,
  SendMessagePayload,
  UnreadCountResponse,
} from "./types";

export async function listConversations(): Promise<ConversationSummary[]> {
  const { data } = await apiClient.get<ConversationSummary[]>("/api/conversations");
  return data;
}

export async function getUnreadCount(): Promise<UnreadCountResponse> {
  const { data } = await apiClient.get<UnreadCountResponse>("/api/conversations/unread-count");
  return data;
}

export async function createConversation(payload: CreateConversationPayload): Promise<ConversationSummary> {
  const { data } = await apiClient.post<ConversationSummary>("/api/conversations", payload);
  return data;
}

export async function listMessages(conversationId: string): Promise<MessageDto[]> {
  const { data } = await apiClient.get<MessageDto[]>(`/api/conversations/${conversationId}/messages`);
  return data;
}

export async function sendMessage(conversationId: string, payload: SendMessagePayload): Promise<MessageDto> {
  const { data } = await apiClient.post<MessageDto>(`/api/conversations/${conversationId}/messages`, payload);
  return data;
}

export async function toggleReaction(conversationId: string, messageId: string, emoji: string): Promise<MessageDto> {
  const { data } = await apiClient.post<MessageDto>(
    `/api/conversations/${conversationId}/messages/${messageId}/reactions`,
    { emoji },
  );
  return data;
}

export async function toggleImportant(conversationId: string, messageId: string): Promise<MessageDto> {
  const { data } = await apiClient.post<MessageDto>(
    `/api/conversations/${conversationId}/messages/${messageId}/important`,
  );
  return data;
}

export async function toggleMute(conversationId: string): Promise<ConversationSummary> {
  const { data } = await apiClient.post<ConversationSummary>(`/api/conversations/${conversationId}/mute`);
  return data;
}

export async function uploadMessageAttachment(
  conversationId: string,
  messageId: string,
  file: File,
): Promise<MessageDto> {
  const form = new FormData();
  form.append("file", file);
  const { data } = await apiClient.post<MessageDto>(
    `/api/conversations/${conversationId}/messages/${messageId}/attachment`,
    form,
    { headers: { "Content-Type": "multipart/form-data" } },
  );
  return data;
}

export async function downloadMessageAttachment(
  conversationId: string,
  messageId: string,
  fileName: string,
): Promise<void> {
  const response = await apiClient.get(`/api/conversations/${conversationId}/messages/${messageId}/attachment`, {
    responseType: "blob",
  });
  const url = URL.createObjectURL(response.data as Blob);
  const link = document.createElement("a");
  link.href = url;
  link.download = fileName;
  document.body.appendChild(link);
  link.click();
  link.remove();
  URL.revokeObjectURL(url);
}
