import { useEffect, useMemo, useRef, useState, type FormEvent, type KeyboardEvent, type ReactNode } from "react";
import { listDirectory } from "../api/auth";
import { extractErrorMessage } from "../api/client";
import {
  createConversation,
  downloadMessageAttachment,
  listConversations,
  listMessages,
  sendMessage,
  toggleImportant,
  toggleMute,
  toggleReaction,
  uploadMessageAttachment,
} from "../api/messaging";
import type { ConversationSummary, MessageDto, ParticipantSummary, UserSummary } from "../api/types";
import { Avatar } from "../components/Avatar";
import { Sidebar } from "../components/Sidebar";
import { Skeleton } from "../components/Skeleton";
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

function QuoteIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M9.75 6.75h-3a2.25 2.25 0 00-2.25 2.25v2.25a2.25 2.25 0 002.25 2.25H8.25v1.5a2.25 2.25 0 01-2.25 2.25M18.75 6.75h-3a2.25 2.25 0 00-2.25 2.25v2.25a2.25 2.25 0 002.25 2.25h1.5v1.5a2.25 2.25 0 01-2.25 2.25"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function CodeIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M17.25 6.75L22.5 12l-5.25 5.25M6.75 6.75L1.5 12l5.25 5.25M14.25 4.5l-4.5 15"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function LinkIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M13.19 8.688a4.5 4.5 0 011.242 7.244l-4.5 4.5a4.5 4.5 0 01-6.364-6.364l1.757-1.757M10.81 15.312a4.5 4.5 0 01-1.242-7.244l4.5-4.5a4.5 4.5 0 016.364 6.364l-1.757 1.757"
        strokeWidth="1.5"
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

function PinIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M9.75 3.75l10.5 10.5-3 3-2.25-2.25-4.5 4.5H6l4.5-4.5-2.25-2.25 3-3zM4.5 19.5l4.243-4.243"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function BellOutlineIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75V9A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c1.733.64 3.56 1.085 5.455 1.31m5.714 0a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function SearchIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M21 21l-5.197-5.197m0 0A7.5 7.5 0 105.196 5.196a7.5 7.5 0 0010.607 10.607z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function CloseIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path d="M6 18L18 6M6 6l12 12" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function PaperclipIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M18.375 12.739l-7.693 7.693a4.5 4.5 0 01-6.364-6.364l10.94-10.94A3 3 0 1119.5 7.372L8.552 18.32a1.5 1.5 0 01-2.122-2.121l9.545-9.546"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function FileIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5A3.375 3.375 0 0010.125 2.25H8.25m5.231 0H8.25a2.25 2.25 0 00-2.25 2.25v15A2.25 2.25 0 008.25 21.75h9a2.25 2.25 0 002.25-2.25v-6.75M13.5 2.25v4.5a2.25 2.25 0 002.25 2.25h4.5"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function formatAttachmentSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function BellSlashIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M3 3l18 18M14.857 17.082a23.848 23.848 0 005.454-1.31A8.967 8.967 0 0118 9.75V9c0-.909-.19-1.774-.534-2.556M15.591 5.106A6 6 0 006 9v.75a8.967 8.967 0 01-2.312 6.022c.98.362 1.99.665 3.026.906m5.848 1.278a24.255 24.255 0 01-5.714 0m5.714 0a3 3 0 11-5.714 0"
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
const LINK_TOKEN = /^\[([^\]]+)\]\(([^)]+)\)$/;

function isSafeLinkUrl(url: string): boolean {
  return /^(https?:|mailto:)/i.test(url.trim());
}

function renderInline(text: string, keyPrefix: string): ReactNode[] {
  const parts: ReactNode[] = [];
  const regex = /(\*\*[^*]+\*\*|\*[^*]+\*|~~[^~]+~~|`[^`]+`|\[[^\]]+\]\([^)]+\))/g;
  let lastIndex = 0;
  let match: RegExpExecArray | null;
  let key = 0;
  while ((match = regex.exec(text)) !== null) {
    if (match.index > lastIndex) parts.push(text.slice(lastIndex, match.index));
    const token = match[0];
    const k = `${keyPrefix}-${key++}`;
    const linkMatch = LINK_TOKEN.exec(token);
    if (token.startsWith("**")) parts.push(<strong key={k}>{token.slice(2, -2)}</strong>);
    else if (token.startsWith("~~")) parts.push(<del key={k}>{token.slice(2, -2)}</del>);
    else if (token.startsWith("`")) parts.push(<code key={k}>{token.slice(1, -1)}</code>);
    else if (linkMatch && isSafeLinkUrl(linkMatch[2])) {
      parts.push(
        <a key={k} href={linkMatch[2]} target="_blank" rel="noopener noreferrer">
          {linkMatch[1]}
        </a>,
      );
    } else if (linkMatch) parts.push(linkMatch[1]);
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
    const quoteMatch = /^> (.*)$/.exec(lines[i]);
    if (quoteMatch) {
      const quotedLines: string[] = [];
      while (i < lines.length) {
        const m = /^> (.*)$/.exec(lines[i]);
        if (!m) break;
        quotedLines.push(m[1]);
        i++;
      }
      const k = `q${key++}`;
      blocks.push(
        <blockquote key={k}>
          {quotedLines.map((line, idx) => (
            <p key={idx}>{renderInline(line, `${k}-${idx}`)}</p>
          ))}
        </blockquote>,
      );
    } else if (bulletMatch) {
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

type FormatType = "bold" | "italic" | "strike" | "bullet" | "numbered" | "quote" | "code" | "link";

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
  const fileInputRef = useRef<HTMLInputElement>(null);
  const [pendingAttachment, setPendingAttachment] = useState<File | null>(null);
  const [openPickerMessageId, setOpenPickerMessageId] = useState<string | null>(null);
  const [showImportantOnly, setShowImportantOnly] = useState(false);
  const [searchOpen, setSearchOpen] = useState(false);
  const [searchQuery, setSearchQuery] = useState("");
  const [mutingConversation, setMutingConversation] = useState(false);

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
    setShowImportantOnly(false);
    setSearchOpen(false);
    setSearchQuery("");
    setPendingAttachment(null);
  }, [selectedId]);

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
    if (!selectedId || (!composeBody.trim() && !pendingAttachment)) return;
    setSending(true);
    setThreadError(null);
    try {
      const body = composeBody.trim() || pendingAttachment!.name;
      let message = await sendMessage(selectedId, { body });
      if (pendingAttachment) {
        message = await uploadMessageAttachment(selectedId, message.id, pendingAttachment);
      }
      setMessages((prev) => [...prev, message]);
      setComposeBody("");
      setPendingAttachment(null);
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

  async function handleToggleImportant(messageId: string) {
    if (!selectedId) return;
    try {
      const updated = await toggleImportant(selectedId, messageId);
      setMessages((prev) => prev.map((m) => (m.id === messageId ? { ...m, important: updated.important } : m)));
    } catch (err) {
      setThreadError(extractErrorMessage(err));
    }
  }

  async function handleToggleMute() {
    if (!selectedId || mutingConversation) return;
    setMutingConversation(true);
    try {
      const updated = await toggleMute(selectedId);
      setConversations((prev) => prev.map((c) => (c.id === selectedId ? { ...c, muted: updated.muted } : c)));
    } catch (err) {
      setThreadError(extractErrorMessage(err));
    } finally {
      setMutingConversation(false);
    }
  }

  async function handleDownloadAttachment(messageId: string, fileName: string) {
    if (!selectedId) return;
    try {
      await downloadMessageAttachment(selectedId, messageId, fileName);
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

    if (type === "bold" || type === "italic" || type === "strike" || type === "code") {
      const marker = type === "bold" ? "**" : type === "italic" ? "*" : type === "strike" ? "~~" : "`";
      const selected = value.slice(start, end);
      const placeholder =
        type === "bold" ? "bold text" : type === "italic" ? "italic text" : type === "strike" ? "struck text" : "code";
      const inner = selected || placeholder;
      const next = value.slice(0, start) + marker + inner + marker + value.slice(end);
      setComposeBody(next);
      const cursor = start + marker.length + inner.length + marker.length;
      requestAnimationFrame(() => {
        ta.focus();
        ta.setSelectionRange(selected ? cursor : start + marker.length, selected ? cursor : start + marker.length + inner.length);
      });
    } else if (type === "link") {
      const selected = value.slice(start, end);
      const linkText = selected || "link text";
      const url = "https://";
      const inserted = `[${linkText}](${url})`;
      const next = value.slice(0, start) + inserted + value.slice(end);
      setComposeBody(next);
      const urlStart = start + 1 + linkText.length + 2;
      requestAnimationFrame(() => {
        ta.focus();
        ta.setSelectionRange(urlStart, urlStart + url.length);
      });
    } else if (type === "quote") {
      const lineStart = value.lastIndexOf("\n", start - 1) + 1;
      let lineEnd = value.indexOf("\n", end);
      if (lineEnd === -1) lineEnd = value.length;
      const block = value.slice(lineStart, lineEnd);
      const prefixed = block
        .split("\n")
        .map((line) => `> ${line}`)
        .join("\n");
      const next = value.slice(0, lineStart) + prefixed + value.slice(lineEnd);
      setComposeBody(next);
      requestAnimationFrame(() => ta.focus());
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
      if ((composeBody.trim() || pendingAttachment) && !sending) {
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
  const groupConversations = sortedConversations.filter((c) => c.isGroup);
  const directConversations = sortedConversations.filter((c) => !c.isGroup);
  const trimmedSearch = searchQuery.trim().toLowerCase();
  const visibleMessages = messages
    .filter((m) => !showImportantOnly || m.important)
    .filter((m) => !trimmedSearch || m.body.toLowerCase().includes(trimmedSearch));

  const renderConversationItem = (conversation: ConversationSummary) => {
    const other = otherParticipant(conversation, user.id);
    return (
      <button
        key={conversation.id}
        className={`messages-list-item${conversation.id === selectedId ? " active" : ""}`}
        onClick={() => setSelectedId(conversation.id)}
      >
        <span className="messages-list-item-avatar">
          {conversation.isGroup || !other ? (
            <GroupAvatarIcon />
          ) : (
            <Avatar userId={other.userId} firstName={other.firstName} lastName={other.lastName} />
          )}
          {!conversation.isGroup && other && (
            <span className={`presence-dot${other.online ? " online" : ""}`} aria-hidden="true" />
          )}
        </span>
        <div className="messages-list-item-body">
          <div className="messages-list-item-top">
            <strong>{conversationName(conversation, user.id)}</strong>
            <span className="messages-list-item-time">{formatListTimestamp(conversation.lastMessageAt)}</span>
          </div>
          <div className="messages-list-item-bottom">
            <p className="messages-preview">{conversation.lastMessagePreview || "No messages yet"}</p>
            <span className="messages-list-item-flags">
              {conversation.muted && <BellSlashIcon />}
              {conversation.unreadCount > 0 && (
                <span className="messages-unread-dot">{conversation.unreadCount}</span>
              )}
            </span>
          </div>
        </div>
      </button>
    );
  };

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
            {listLoading &&
              [0, 1, 2, 3, 4].map((i) => (
                <div className="messages-list-item" key={i}>
                  <Skeleton width={40} height={40} radius={999} />
                  <div className="messages-list-item-body">
                    <Skeleton width="70%" height={13} />
                    <Skeleton width="90%" height={12} />
                  </div>
                </div>
              ))}
            {listError && <p className="form-error">{listError}</p>}
            {!listLoading && !listError && sortedConversations.length === 0 && (
              <div className="empty-state">
                <EmptyConversationsIcon />
                <p>No conversations yet. Start one above.</p>
              </div>
            )}
            {groupConversations.length > 0 && (
              <div className="messages-list-section">
                <span className="messages-list-section-label">Group Conversations</span>
                {groupConversations.map(renderConversationItem)}
              </div>
            )}
            {directConversations.length > 0 && (
              <div className="messages-list-section">
                <span className="messages-list-section-label">Direct Messages</span>
                {directConversations.map(renderConversationItem)}
              </div>
            )}
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
                  <div className="messages-thread-header-title">
                    <strong>{conversationName(selectedConversation, user.id)}</strong>
                    <p className="table-hint">
                      {selectedConversation.participants
                        .filter((p) => p.userId !== user.id)
                        .map((p) => p.email)
                        .join(", ")}
                    </p>
                  </div>
                  <div className="messages-thread-header-actions">
                    <button
                      type="button"
                      className={`messages-header-icon-btn${searchOpen ? " active" : ""}`}
                      aria-label="Search this conversation"
                      title="Search this conversation"
                      onClick={() => setSearchOpen((prev) => !prev)}
                    >
                      <SearchIcon />
                    </button>
                    {selectedConversation.isGroup && (
                      <span className="messages-header-badge">
                        Members &middot; {selectedConversation.participants.length}
                      </span>
                    )}
                    <button
                      type="button"
                      className={`messages-header-badge messages-important-toggle${showImportantOnly ? " active" : ""}`}
                      onClick={() => setShowImportantOnly((prev) => !prev)}
                    >
                      <PinIcon /> Important &middot; {messages.filter((m) => m.important).length}
                    </button>
                    <button
                      type="button"
                      className="messages-header-icon-btn"
                      aria-label={selectedConversation.muted ? "Unmute conversation" : "Mute conversation"}
                      title={selectedConversation.muted ? "Unmute conversation" : "Mute conversation"}
                      onClick={handleToggleMute}
                      disabled={mutingConversation}
                    >
                      {selectedConversation.muted ? <BellSlashIcon /> : <BellOutlineIcon />}
                    </button>
                  </div>
                </div>

                {searchOpen && (
                  <div className="messages-search-bar">
                    <SearchIcon />
                    <input
                      autoFocus
                      value={searchQuery}
                      onChange={(e) => setSearchQuery(e.target.value)}
                      placeholder="Search in this conversation..."
                    />
                    <button
                      type="button"
                      aria-label="Close search"
                      onClick={() => {
                        setSearchOpen(false);
                        setSearchQuery("");
                      }}
                    >
                      <CloseIcon />
                    </button>
                  </div>
                )}

                {threadLoading && (
                  <div className="messages-thread-body">
                    {[0, 1, 2].map((i) => (
                      <div className={`message-row${i === 1 ? " message-row-self" : ""}`} key={i}>
                        <Skeleton width={i === 1 ? 180 : 240} height={48} radius={14} />
                      </div>
                    ))}
                  </div>
                )}
                {threadError && <p className="form-error">{threadError}</p>}

                <div className="messages-thread-body">
                  {visibleMessages.length === 0 && (showImportantOnly || trimmedSearch) && (
                    <p className="messages-pane-status">
                      {trimmedSearch ? `No messages match "${searchQuery.trim()}".` : "No important messages yet."}
                    </p>
                  )}
                  {visibleMessages.map((message, index) => {
                    const isSelf = message.senderId === user.id;
                    const prev = visibleMessages[index - 1];
                    const showSender = selectedConversation.isGroup && !isSelf && prev?.senderId !== message.senderId;
                    const pickerOpen = openPickerMessageId === message.id;
                    return (
                      <div key={message.id} className={`message-row${isSelf ? " message-row-self" : ""}`}>
                        <div
                          className={`message-bubble${isSelf ? " message-bubble-self" : ""}${message.important ? " message-bubble-important" : ""}`}
                        >
                          {showSender && <div className="message-bubble-sender">{message.senderName}</div>}
                          <div className="message-bubble-body">{renderMessageBody(message.body)}</div>
                          {message.attachment && (
                            <button
                              type="button"
                              className="message-attachment-chip"
                              onClick={() => handleDownloadAttachment(message.id, message.attachment!.fileName)}
                            >
                              <FileIcon />
                              <span className="message-attachment-name">{message.attachment.fileName}</span>
                              <span className="message-attachment-size">{formatAttachmentSize(message.attachment.fileSize)}</span>
                            </button>
                          )}
                          <span className="message-bubble-time">{formatBubbleTimestamp(message.createdAt)}</span>
                          <button
                            type="button"
                            className={`message-important-trigger${message.important ? " active" : ""}`}
                            aria-label={message.important ? "Unmark as important" : "Mark as important"}
                            title={message.important ? "Unmark as important" : "Mark as important"}
                            onClick={() => handleToggleImportant(message.id)}
                          >
                            <PinIcon />
                          </button>
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
                    <span className="messages-toolbar-divider" />
                    <button type="button" className="fmt-btn" title="Quote" onClick={() => applyFormat("quote")}>
                      <QuoteIcon />
                    </button>
                    <button type="button" className="fmt-btn" title="Code" onClick={() => applyFormat("code")}>
                      <CodeIcon />
                    </button>
                    <button type="button" className="fmt-btn" title="Link" onClick={() => applyFormat("link")}>
                      <LinkIcon />
                    </button>
                    <span className="messages-toolbar-divider" />
                    <button type="button" className="fmt-btn" title="Attach a file" onClick={() => fileInputRef.current?.click()}>
                      <PaperclipIcon />
                    </button>
                    <input
                      ref={fileInputRef}
                      type="file"
                      className="messages-file-input"
                      onChange={(e) => {
                        setPendingAttachment(e.target.files?.[0] ?? null);
                        e.target.value = "";
                      }}
                    />
                  </div>
                  {pendingAttachment && (
                    <div className="messages-pending-attachment">
                      <FileIcon />
                      <span>{pendingAttachment.name}</span>
                      <span className="table-hint">{formatAttachmentSize(pendingAttachment.size)}</span>
                      <button type="button" aria-label="Remove attachment" onClick={() => setPendingAttachment(null)}>
                        <CloseIcon />
                      </button>
                    </div>
                  )}
                  <textarea
                    ref={composeRef}
                    rows={2}
                    value={composeBody}
                    onChange={(e) => setComposeBody(e.target.value)}
                    onKeyDown={handleComposeKeyDown}
                    placeholder="Write a message..."
                  />
                  <div className="messages-compose-footer">
                    <span className="messages-compose-hint">Enter to send &middot; Shift+Enter for a new line</span>
                    <button
                      type="submit"
                      className="messages-send-btn"
                      disabled={sending || (!composeBody.trim() && !pendingAttachment)}
                      aria-label="Send message"
                    >
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
