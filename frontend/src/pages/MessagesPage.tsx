import { useEffect, useMemo, useState, type FormEvent } from "react";
import { listDirectory } from "../api/auth";
import { extractErrorMessage } from "../api/client";
import { createConversation, listConversations, listMessages, sendMessage } from "../api/messaging";
import type { ConversationSummary, MessageDto, UserSummary } from "../api/types";
import { Avatar } from "../components/Avatar";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

const LIST_POLL_MS = 20000;
const THREAD_POLL_MS = 8000;

function conversationName(conversation: ConversationSummary, selfId: string): string {
  if (conversation.title) return conversation.title;
  const others = conversation.participants.filter((p) => p.userId !== selfId);
  if (others.length === 0) return "You";
  return others.map((p) => `${p.firstName} ${p.lastName}`).join(", ");
}

export function MessagesPage() {
  const { user } = useAuth();

  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [listLoading, setListLoading] = useState(true);
  const [listError, setListError] = useState<string | null>(null);

  const [selectedId, setSelectedId] = useState<string | null>(null);
  const [messages, setMessages] = useState<MessageDto[]>([]);
  const [threadLoading, setThreadLoading] = useState(false);
  const [threadError, setThreadError] = useState<string | null>(null);
  const [composeBody, setComposeBody] = useState("");
  const [sending, setSending] = useState(false);

  const [showNewMessage, setShowNewMessage] = useState(false);
  const [directory, setDirectory] = useState<UserSummary[]>([]);
  const [selectedRecipientIds, setSelectedRecipientIds] = useState<Set<string>>(new Set());
  const [newGroupTitle, setNewGroupTitle] = useState("");
  const [newInitialMessage, setNewInitialMessage] = useState("");
  const [creating, setCreating] = useState(false);
  const [createError, setCreateError] = useState<string | null>(null);

  useEffect(() => {
    function refreshList() {
      listConversations()
        .then(setConversations)
        .catch((err) => setListError(extractErrorMessage(err)))
        .finally(() => setListLoading(false));
    }
    refreshList();
    const interval = setInterval(refreshList, LIST_POLL_MS);
    return () => clearInterval(interval);
  }, []);

  useEffect(() => {
    if (!selectedId) return;
    function refreshThread() {
      listMessages(selectedId!)
        .then((msgs) => {
          setMessages(msgs);
          setConversations((prev) => prev.map((c) => (c.id === selectedId ? { ...c, unreadCount: 0 } : c)));
        })
        .catch((err) => setThreadError(extractErrorMessage(err)))
        .finally(() => setThreadLoading(false));
    }
    setThreadLoading(true);
    setThreadError(null);
    refreshThread();
    const interval = setInterval(refreshThread, THREAD_POLL_MS);
    return () => clearInterval(interval);
  }, [selectedId]);

  useEffect(() => {
    if (!showNewMessage || directory.length > 0) return;
    listDirectory()
      .then(setDirectory)
      .catch((err) => setCreateError(extractErrorMessage(err)));
  }, [showNewMessage, directory.length]);

  const selectedConversation = useMemo(
    () => conversations.find((c) => c.id === selectedId) ?? null,
    [conversations, selectedId],
  );

  async function handleSend(event: FormEvent) {
    event.preventDefault();
    if (!selectedId || !composeBody.trim()) return;
    setSending(true);
    setThreadError(null);
    try {
      const message = await sendMessage(selectedId, { body: composeBody });
      setMessages((prev) => [...prev, message]);
      setComposeBody("");
    } catch (err) {
      setThreadError(extractErrorMessage(err));
    } finally {
      setSending(false);
    }
  }

  function toggleRecipient(userId: string) {
    setSelectedRecipientIds((prev) => {
      const next = new Set(prev);
      if (next.has(userId)) next.delete(userId);
      else next.add(userId);
      return next;
    });
  }

  async function handleCreateConversation(event: FormEvent) {
    event.preventDefault();
    if (selectedRecipientIds.size === 0 || !newInitialMessage.trim()) return;
    setCreating(true);
    setCreateError(null);
    try {
      const conversation = await createConversation({
        participantIds: [...selectedRecipientIds],
        initialMessage: newInitialMessage,
        title: selectedRecipientIds.size > 1 ? newGroupTitle || undefined : undefined,
      });
      setConversations((prev) => {
        const withoutExisting = prev.filter((c) => c.id !== conversation.id);
        return [conversation, ...withoutExisting];
      });
      setSelectedId(conversation.id);
      setShowNewMessage(false);
      setSelectedRecipientIds(new Set());
      setNewGroupTitle("");
      setNewInitialMessage("");
    } catch (err) {
      setCreateError(extractErrorMessage(err));
    } finally {
      setCreating(false);
    }
  }

  if (!user) return null;

  const sortedConversations = [...conversations].sort((a, b) => {
    const aTime = a.lastMessageAt ? new Date(a.lastMessageAt).getTime() : 0;
    const bTime = b.lastMessageAt ? new Date(b.lastMessageAt).getTime() : 0;
    return bTime - aTime;
  });

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header page-header-with-actions">
          <div>
            <h1>Messages</h1>
            <p>Direct and group conversations with board members</p>
          </div>
          <button
            onClick={() => {
              setShowNewMessage((prev) => !prev);
              setCreateError(null);
            }}
          >
            {showNewMessage ? "Cancel" : "New message"}
          </button>
        </div>

        {showNewMessage && (
          <section className="dashboard-section">
            <h2>New message</h2>
            {createError && <p className="form-error">{createError}</p>}
            <form className="add-user-form" onSubmit={handleCreateConversation}>
              <label>Recipients</label>
              <div className="recipient-picker">
                {directory
                  .filter((member) => member.id !== user.id)
                  .map((member) => (
                    <label key={member.id} className="recipient-picker-item">
                      <input
                        type="checkbox"
                        checked={selectedRecipientIds.has(member.id)}
                        onChange={() => toggleRecipient(member.id)}
                      />
                      <Avatar
                        userId={member.id}
                        photoUpdatedAt={member.photoUpdatedAt}
                        firstName={member.firstName}
                        lastName={member.lastName}
                      />
                      <span>
                        {member.firstName} {member.lastName}
                        <span className="table-hint"> {member.email}</span>
                      </span>
                    </label>
                  ))}
                {directory.length === 0 && <p className="table-hint">Loading directory...</p>}
              </div>
              {selectedRecipientIds.size > 1 && (
                <label>
                  Group name (optional)
                  <input value={newGroupTitle} onChange={(e) => setNewGroupTitle(e.target.value)} />
                </label>
              )}
              <label>
                Message
                <textarea
                  className="minutes-textarea"
                  rows={3}
                  value={newInitialMessage}
                  onChange={(e) => setNewInitialMessage(e.target.value)}
                  required
                />
              </label>
              <button type="submit" disabled={creating || selectedRecipientIds.size === 0}>
                {creating ? "Sending..." : "Start conversation"}
              </button>
            </form>
          </section>
        )}

        <div className="messages-layout">
          <div className="messages-list-pane">
            {listLoading && <p>Loading conversations...</p>}
            {listError && <p className="form-error">{listError}</p>}
            {!listLoading && !listError && sortedConversations.length === 0 && (
              <div className="empty-state">
                <p>No conversations yet. Start one above.</p>
              </div>
            )}
            {sortedConversations.map((conversation) => (
              <button
                key={conversation.id}
                className={`messages-list-item${conversation.id === selectedId ? " active" : ""}`}
                onClick={() => setSelectedId(conversation.id)}
              >
                <div className="messages-list-item-top">
                  <strong>{conversationName(conversation, user.id)}</strong>
                  {conversation.unreadCount > 0 && (
                    <span className="badge badge-cancelled">{conversation.unreadCount}</span>
                  )}
                </div>
                {conversation.lastMessagePreview && (
                  <p className="table-hint messages-preview">{conversation.lastMessagePreview}</p>
                )}
              </button>
            ))}
          </div>

          <div className="messages-thread-pane">
            {!selectedConversation && (
              <div className="empty-state">
                <p>Select a conversation to view messages.</p>
              </div>
            )}
            {selectedConversation && (
              <>
                <div className="messages-thread-header">
                  <strong>{conversationName(selectedConversation, user.id)}</strong>
                  <p className="table-hint">
                    {selectedConversation.participants
                      .filter((p) => p.userId !== user.id)
                      .map((p) => p.email)
                      .join(", ")}
                  </p>
                </div>

                {threadLoading && <p>Loading messages...</p>}
                {threadError && <p className="form-error">{threadError}</p>}

                <div className="messages-thread-body">
                  {messages.map((message) => (
                    <div
                      key={message.id}
                      className={`message-bubble${message.senderId === user.id ? " message-bubble-self" : ""}`}
                    >
                      {selectedConversation.isGroup && message.senderId !== user.id && (
                        <div className="message-bubble-sender">{message.senderName}</div>
                      )}
                      <p>{message.body}</p>
                      <span className="message-bubble-time">{new Date(message.createdAt).toLocaleString()}</span>
                    </div>
                  ))}
                </div>

                <form className="messages-compose-form" onSubmit={handleSend}>
                  <textarea
                    rows={2}
                    value={composeBody}
                    onChange={(e) => setComposeBody(e.target.value)}
                    placeholder="Write a message..."
                    required
                  />
                  <button type="submit" disabled={sending || !composeBody.trim()}>
                    {sending ? "Sending..." : "Send"}
                  </button>
                </form>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
