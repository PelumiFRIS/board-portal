import { apiClient } from "./client";
import type { Comment, CommentEntityType, CreateCommentPayload } from "./types";

export async function listComments(entityType: CommentEntityType, entityId: string): Promise<Comment[]> {
  const { data } = await apiClient.get<Comment[]>("/api/comments", { params: { entityType, entityId } });
  return data;
}

export async function createComment(payload: CreateCommentPayload): Promise<Comment> {
  const { data } = await apiClient.post<Comment>("/api/comments", payload);
  return data;
}

export async function deleteComment(id: string): Promise<void> {
  await apiClient.delete(`/api/comments/${id}`);
}
