import { useEffect, useMemo, useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode } from "react";
import { listDirectory } from "../api/auth";
import { extractErrorMessage } from "../api/client";
import { createConversation, listConversations, listMessages, sendMessage, toggleReaction } from "../api/messaging";
import type { ConversationSummary, MessageDto, ParticipantSummary, UserSummary } from "../api/types";
import { Avatar } from "../components/Avatar";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { useAuth } from "../context/AuthContext";

const LIST_POLL_MS = 20000;
const THREAD_POLL_MS = 8000;

function conversationName(conversation: ConversationSummary, selfId: string): string {
  if (conversation.title) return conversation.title;
  const others = conversation.participants.filter((p) => p.userId !== selfId);
  if (others.length === 0) return "You";
  return others.map((p) => `${p.firstName} ${p.lastName}`).join(", ");
}

function otherParticipant(conversation: ConversationSummary, selfId: string): ParticipantSummary | null {
  return conversation.participants.find((p) => p.userId !== selfId) ?? null;
}

function GroupAvatarIcon() {
  return (
    <span className="avatar avatar-group" aria-hidden="true">
      <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
        <path
          d="M17.25 21v-1.5a3.75 3.75 0 00-3.75-3.75h-3A3.75 3.75 0 006.75 19.5V21M12 12a3.75 3.75 0 100-7.5 3.75 3.75 0 000 7.5zm7.5 9v-1.125a3 3 0 00-2.25-2.906M15.75 6.19a3 3 0 010 5.622"
          strokeWidth="1.6"
          strokeLinecap="round"
          strokeLinejoin="round"
        />
      </svg>
    </span>
  );
}

function EmptyConversationsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M7.5 8.25h9m-9 3.75h5.25M21 12c0 4.556-4.03 8.25-9 8.25a9.76 9.76 0 01-2.555-.337A5.972 5.972 0 015.41 20.4a5.969 5.969 0 01-1.925-3.546 8.9 8.9 0 01-.235-1.634C3.25 10.694 7.05 3.75 12 3.75s9 4.362 9 8.25z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function SendIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path d="M4.5 19.5l15-7.5-15-7.5 3.75 7.5-3.75 7.5z" strokeWidth="1.6" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function BulletListIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M8.25 6.75h12M8.25 12h12M8.25 17.25h12M3.75 6.75h.008v.008H3.75V6.75zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zM3.75 12h.008v.008H3.75V12zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0zm-.375 5.25h.008v.008H3.75v-.008zm.375 0a.375.375 0 11-.75 0 .375.375 0 01.75 0z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function NumberedListIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M8.25 6.75h12M8.25 12h12M8.25 17.25h12M4.5 5.25v3M4.5 5.25L3.75 6M3.75 15.75h1.5l-1.5 1.5h1.5m-1.5-3.75a.75.75 0 01.75-.75h.75v1.5H4.5"
        strokeWidth="1.4"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ReactIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M15.182 15.182a4.5 4.5 0 01-6.364 0M21 12a9 9 0 11-18 0 9 9 0 0118 0zM9.75 9.75c0 .414-.168 0-.375 0S9 10.164 9 9.75 9.168 9 9.375 9s.375.336.375.75zm-.375 0h.008v.015h-.008V9.75zm5.625 0c0 .414-.168 0-.375 0s-.375.164-.375-.25.168-.75.375-.75.375.336.375.75zm-.375 0h.008v.015h-.008V9.75z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

const REACTION_EMOJI = ["👍", "❤️", "😂", "🎉", "✅", "👀"];

function formatSeenBy(seenBy: ParticipantSummary[]): string {
  const names = seenBy.map((p) => p.firstName);
  if (names.length <= 2) return `Seen by ${names.join(" and ")}`;
  return `Seen by ${names.slice(0, 2).join(", ")} and ${names.length - 2} more`;
}

/** Minimal, safe markdown-lite: **bold**, *italic*, ~~strike~~, "- " bullets, "1. " numbered lists. */
function renderInline(text: string, keyPrefix: string): ReactNode[] {
  const parts: ReactNode[] = [];
  const regex = /(\*\*[^*]+\*\*|\*[^*]+\*|~~[^~]+~~)/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let key = 0;
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) parts.push(text.slice(lastIndex, match.index));
    const token = match[0];
    const k = `${keyPrefix}-${key++}`;
    if (token.startsWith("**")) parts.push(<strong key={k}>{token.slice(2, -2)}</strong>);
    else if (token.startsWith("~~")) parts.push(<del key={k}>{token.slice(2, -2)}</del>);
    else parts.push(<em key={k}>{token.slice(1, -1)}</em>);
    lastIndex = match.index + token.length;
  }
  if (lastIndex < text.length) parts.push(text.slice(lastIndex));
  return parts;
}

function renderMessageBody(text: string): ReactNode {
  const lines = text.split("\n");
  const blocks: ReactNode[] = [];
  let i = 0;
  let key = 0;
  while (i < lines.length) {
    const bulletMatch = /^- (.*)$/.exec(lines[i]);
    const numberedMatch = /^\d+\. (.*)$/.exec(lines[i]);
    if (bulletMatch) {
      const items: string[] = [];
      while (i < lines.length) {
        const m = /^- (.*)$/.exec(lines[i]);
        if (!m) break;
        items.push(m[1]);
        i++;
      }
      const k = `b${key++}`;
      blocks.push(
        <ul key={k}>
          {items.map((item, idx) => (
            <li key={idx}>{renderInline(item, `${k}-${idx}`)}</li>
          ))}
        </ul>,
      );
    } else if (numberedMatch) {
      const items: string[] = [];
      while (i < lines.length) {
        const m = /^\d+\. (.*)$/.exec(lines[i]);
        if (!m) break;
        items.push(m[1]);
        i++;
      }
      const k = `n${key++}`;
      blocks.push(
        <ol key={k}>
          {items.map((item, idx) => (
            <li key={idx}>{renderInline(item, `${k}-${idx}`)}</li>
          ))}
        </ol>,
      );
    } else {
      const k = `p${key++}`;
      blocks.push(<p key={k}>{renderInline(lines[i], k)}</p>);
      i++;
    }
  }
  return blocks;
}

type FormatType = "bold" | "italic" | "strike" | "bullet" | "numbered";

const DAY_MS = 24 * 60 * 60 * 1000;

function formatListTimestamp(iso: string | null): string {
  if (!iso) return "";
  const date = new Date(iso);
  const now = new Date();
  const startOfToday = new Date(now.getFullYear(), now.getMonth(), now.getDate());
  const diffDays = Math.floor((startOfToday.getTime() - new Date(date.getFullYear(), date.getMonth(), date.getDate()).getTime()) / DAY_MS);
  if (diffDays <= 0) return date.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
  if (diffDays === 1) return "Yesterday";
  if (diffDays < 7) return date.toLocaleDateString([], { weekday: "short" });
  return date.toLocaleDateString([], { month: "short", day: "numeric" });
}

function formatBubbleTimestamp(iso: string): string {
  const date = new Date(iso);
  const now = new Date();
  const sameDay = date.toDateString() === now.toDateString();
  const time = date.toLocaleTimeString([], { hour: "numeric", minute: "2-digit" });
  if (sameDay) return time;
  const yesterday = new Date(now);
  yesterday.setDate(now.getDate() - 1);
  if (date.toDateString() === yesterday.toDateString()) return `Yesterday, ${time}`;
  return `${date.toLocaleDateString([], { month: "short", day: "numeric" })}, ${time}`;
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
  const composeRef = useRef<HTMLTextAreaElement>(null);
  const [openPickerMessageId, setOpenPickerMessageId] = useState<string | null>(null);

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

  useEffect(() => {
    if (!openPickerMessageId) return;
    function handleClickOutside(event: MouseEvent) {
      const target = event.target as HTMLElement;
      if (!target.closest(".message-react-picker") && !target.closest(".message-react-trigger")) {
        setOpenPickerMessageId(null);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [openPickerMessageId]);

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

  async function handleToggleReaction(messageId: string, emoji: string) {
    if (!selectedId) return;
    setOpenPickerMessageId(null);
    try {
      const updated = await toggleReaction(selectedId, messageId, emoji);
      setMessages((prev) => prev.map((m) => (m.id === messageId ? { ...m, reactions: updated.reactions } : m)));
    } catch (err) {
      setThreadError(extractErrorMessage(err));
    }
  }

  function applyFormat(type: FormatType) {
    const ta = composeRef.current;
    if (!ta) return;
    const start = ta.selectionStart;
    const end = ta.selectionEnd;
    const value = composeBody;

    if (type === "bold" || type === "italic" || type === "strike") {
      const marker = type === "bold" ? "**" : type === "italic" ? "*" : "~~";
      const selected = value.slice(start, end);
      const placeholder = type === "bold" ? "bold text" : type === "italic" ? "italic text" : "struck text";
      const inner = selected || placeholder;
      const next = value.slice(0, start) + marker + inner + marker + value.slice(end);
      setComposeBody(next);
      const cursor = start + marker.length + inner.length + marker.length;
      requestAnimationFrame(() => {
        ta.focus();
        ta.setSelectionRange(selected ? cursor : start + marker.length, selected ? cursor : start + marker.length + inner.length);
      });
    } else {
      const lineStart = value.lastIndexOf("\n", start - 1) + 1;
      let lineEnd = value.indexOf("\n", end);
      if (lineEnd === -1) lineEnd = value.length;
      const block = value.slice(lineStart, lineEnd);
      const lines = block.split("\n");
      const prefixed = lines.map((line, i) => (type === "bullet" ? `- ${line}` : `${i + 1}. ${line}`)).join("\n");
      const next = value.slice(0, lineStart) + prefixed + value.slice(lineEnd);
      setComposeBody(next);
      requestAnimationFrame(() => ta.focus());
    }
  }

  function handleComposeKeyDown(event: KeyboardEvent<HTMLTextAreaElement>) {
    if (event.key === "Enter" && !event.shiftKey) {
      event.preventDefault();
      if (composeBody.trim() && !sending) {
        handleSend(event as unknown as FormEvent);
      }
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
        <TopBar />
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
            {listLoading && <p className="messages-pane-status">Loading conversations...</p>}
            {listError && <p className="form-error">{listError}</p>}
            {!listLoading && !listError && sortedConversations.length === 0 && (
              <div className="empty-state">
                <EmptyConversationsIcon />
                <p>No conversations yet. Start one above.</p>
              </div>
            )}
            {sortedConversations.map((conversation) => {
              const other = otherParticipant(conversation, user.id);
              return (
                <button
                  key={conversation.id}
                  className={`messages-list-item${conversation.id === selectedId ? " active" : ""}`}
                  onClick={() => setSelectedId(conversation.id)}
                >
                  {conversation.isGroup || !other ? (
                    <GroupAvatarIcon />
                  ) : (
                    <Avatar userId={other.userId} firstName={other.firstName} lastName={other.lastName} />
                  )}
                  <div className="messages-list-item-body">
                    <div className="messages-list-item-top">
                      <strong>{conversationName(conversation, user.id)}</strong>
                      <span className="messages-list-item-time">{formatListTimestamp(conversation.lastMessageAt)}</span>
                    </div>
                    <div className="messages-list-item-bottom">
                      <p className="messages-preview">{conversation.lastMessagePreview || "No messages yet"}</p>
                      {conversation.unreadCount > 0 && (
                        <span className="messages-unread-dot">{conversation.unreadCount}</span>
                      )}
                    </div>
                  </div>
                </button>
              );
            })}
          </div>

          <div className="messages-thread-pane">
            {!selectedConversation && (
              <div className="empty-state">
                <EmptyConversationsIcon />
                <p>Select a conversation to view messages.</p>
              </div>
            )}
            {selectedConversation && (
              <>
                <div className="messages-thread-header">
                  {selectedConversation.isGroup ? (
                    <GroupAvatarIcon />
                  ) : (
                    (() => {
                      const other = otherParticipant(selectedConversation, user.id);
                      return other ? (
                        <Avatar userId={other.userId} firstName={other.firstName} lastName={other.lastName} />
                      ) : (
                        <GroupAvatarIcon />
                      );
                    })()
                  )}
                  <div>
                    <strong>{conversationName(selectedConversation, user.id)}</strong>
                    <p className="table-hint">
                      {selectedConversation.isGroup && (
                        <span className="messages-member-count">
                          {selectedConversation.participants.length} members &middot;{" "}
                        </span>
                      )}
                      {selectedConversation.participants
                        .filter((p) => p.userId !== user.id)
                        .map((p) => p.email)
                        .join(", ")}
                    </p>
                  </div>
                </div>

                {threadLoading && <p className="messages-pane-status">Loading messages...</p>}
                {threadError && <p className="form-error">{threadError}</p>}

                <div className="messages-thread-body">
                  {messages.map((message, index) => {
                    const isSelf = message.senderId === user.id;
                    const prev = messages[index - 1];
                    const showSender = selectedConversation.isGroup && !isSelf && prev?.senderId !== message.senderId;
                    const pickerOpen = openPickerMessageId === message.id;
                    return (
                      <div key={message.id} className={`message-row${isSelf ? " message-row-self" : ""}`}>
                        <div className={`message-bubble${isSelf ? " message-bubble-self" : ""}`}>
                          {showSender && <div className="message-bubble-sender">{message.senderName}</div>}
                          <div className="message-bubble-body">{renderMessageBody(message.body)}</div>
                          <span className="message-bubble-time">{formatBubbleTimestamp(message.createdAt)}</span>
                          <button
                            type="button"
                            className="message-react-trigger"
                            aria-label="Add reaction"
                            onClick={() => setOpenPickerMessageId(pickerOpen ? null : message.id)}
                          >
                            <ReactIcon />
                          </button>
                          {pickerOpen && (
                            <div className="message-react-picker">
                              {REACTION_EMOJI.map((emoji) => (
                                <button
                                  type="button"
                                  key={emoji}
                                  onClick={() => handleToggleReaction(message.id, emoji)}
                                >
                                  {emoji}
                                </button>
                              ))}
                            </div>
                          )}
                        </div>
                        {message.reactions.length > 0 && (
                          <div className="message-reactions">
                            {message.reactions.map((reaction) => (
                              <button
                                type="button"
                                key={reaction.emoji}
                                className={`message-reaction-pill${reaction.reactedByMe ? " active" : ""}`}
                                onClick={() => handleToggleReaction(message.id, reaction.emoji)}
                              >
                                <span>{reaction.emoji}</span> {reaction.count}
                              </button>
                            ))}
                          </div>
                        )}
                        {isSelf && message.seenBy.length > 0 && (
                          <div className="message-seen-by" title={message.seenBy.map((p) => `${p.firstName} ${p.lastName}`).join(", ")}>
                            {formatSeenBy(message.seenBy)}
                          </div>
                        )}
                      </div>
                    );
                  })}
                </div>

                <form className="messages-compose-card" onSubmit={handleSend}>
                  <div className="messages-compose-toolbar">
                    <button type="button" className="fmt-btn fmt-bold" title="Bold" onClick={() => applyFormat("bold")}>
                      B
                    </button>
                    <button type="button" className="fmt-btn fmt-italic" title="Italic" onClick={() => applyFormat("italic")}>
                      I
                    </button>
                    <button type="button" className="fmt-btn fmt-strike" title="Strikethrough" onClick={() => applyFormat("strike")}>
                      S
                    </button>
                    <span className="messages-toolbar-divider" />
                    <button type="button" className="fmt-btn" title="Bulleted list" onClick={() => applyFormat("bullet")}>
                      <BulletListIcon />
                    </button>
                    <button type="button" className="fmt-btn" title="Numbered list" onClick={() => applyFormat("numbered")}>
                      <NumberedListIcon />
                    </button>
                  </div>
                  <textarea
                    ref={composeRef}
                    rows={2}
                    value={composeBody}
                    onChange={(e) => setComposeBody(e.target.value)}
                    onKeyDown={handleComposeKeyDown}
                    placeholder="Write a message..."
                    required
                  />
                  <div className="messages-compose-footer">
                    <span className="messages-compose-hint">Enter to send &middot; Shift+Enter for a new line</span>
                    <button type="submit" className="messages-send-btn" disabled={sending || !composeBody.trim()} aria-label="Send message">
                      <SendIcon />
                    </button>
                  </div>
                </form>
              </>
            )}
          </div>
        </div>
      </main>
    </div>
  );
}
