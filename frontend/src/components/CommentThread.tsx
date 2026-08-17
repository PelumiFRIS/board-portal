import { useEffect, useState, type FormEvent } from "react";
import { createComment, deleteComment, listComments } from "../api/comments";
import { extractErrorMessage } from "../api/client";
import type { Comment, CommentEntityType } from "../api/types";
import { useAuth } from "../context/AuthContext";
import { Avatar } from "./Avatar";

export function CommentThread({ entityType, entityId }: { entityType: CommentEntityType; entityId: string }) {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [comments, setComments] = useState<Comment[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [body, setBody] = useState("");
  const [posting, setPosting] = useState(false);
  const [deletingId, setDeletingId] = useState<string | null>(null);

  useEffect(() => {
    setLoading(true);
    listComments(entityType, entityId)
      .then(setComments)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [entityType, entityId]);

  async function handlePost(event: FormEvent) {
    event.preventDefault();
    setActionError(null);
    setPosting(true);
    try {
      const comment = await createComment({ entityType, entityId, body });
      setComments((prev) => [...prev, comment]);
      setBody("");
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setPosting(false);
    }
  }

  async function handleDelete(commentId: string) {
    setActionError(null);
    setDeletingId(commentId);
    try {
      await deleteComment(commentId);
      setComments((prev) => prev.filter((c) => c.id !== commentId));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setDeletingId(null);
    }
  }

  if (!user) return null;

  return (
    <div>
      {loading && <p>Loading discussion...</p>}
      {loadError && <p className="form-error">{loadError}</p>}
      {actionError && <p className="form-error">{actionError}</p>}

      {!loading && !loadError && comments.length === 0 && (
        <div className="empty-state">
          <p>No comments yet. Start the discussion below.</p>
        </div>
      )}

      {!loading &&
        !loadError &&
        comments.map((comment) => (
          <div key={comment.id} className="comment-item">
            <Avatar firstName={comment.authorName.split(" ")[0] ?? ""} lastName={comment.authorName.split(" ")[1] ?? ""} />
            <div className="comment-body">
              <div className="comment-meta">
                <strong>{comment.authorName}</strong>
                <span className="table-hint">{new Date(comment.createdAt).toLocaleString()}</span>
              </div>
              <p>{comment.body}</p>
              {(isAdmin || comment.authorId === user.id) && (
                <button
                  className="secondary small"
                  disabled={deletingId === comment.id}
                  onClick={() => handleDelete(comment.id)}
                >
                  {deletingId === comment.id ? "Deleting..." : "Delete"}
                </button>
              )}
            </div>
          </div>
        ))}

      <form className="add-user-form" onSubmit={handlePost}>
        <label>
          Add a comment
          <textarea
            className="minutes-textarea"
            rows={3}
            value={body}
            onChange={(e) => setBody(e.target.value)}
            required
          />
        </label>
        <button type="submit" disabled={posting}>
          {posting ? "Posting..." : "Post comment"}
        </button>
      </form>
    </div>
  );
}
