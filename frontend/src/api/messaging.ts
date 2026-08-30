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
