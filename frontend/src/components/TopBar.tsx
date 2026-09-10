import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { listActionItems } from "../api/actionItems";
import { getUnreadCount, listConversations } from "../api/messaging";
import type { ActionItemSummary, ConversationSummary } from "../api/types";
import { ROLE_LABELS } from "../constants/roles";
import { useAuth } from "../context/AuthContext";
import { Avatar } from "./Avatar";
import { NAV_ITEMS } from "./Sidebar";

const UNREAD_POLL_MS = 25000;

interface NotificationEntry {
  key: string;
  category: "MESSAGE" | "ACTION ITEM";
  title: string;
  description: string;
  timestamp: string;
  to: string;
}

function conversationDisplayName(conversation: ConversationSummary, selfId: string): string {
  if (conversation.title) return conversation.title;
  const others = conversation.participants.filter((p) => p.userId !== selfId);
  if (others.length === 0) return "You";
  return others.map((p) => `${p.firstName} ${p.lastName}`).join(", ");
}

function formatRelativeTime(iso: string): string {
  const diffMs = Date.now() - new Date(iso).getTime();
  const minutes = Math.round(diffMs / 60000);
  if (minutes < 1) return "Just now";
  if (minutes < 60) return `${minutes}m ago`;
  const hours = Math.round(minutes / 60);
  if (hours < 24) return `${hours}h ago`;
  const days = Math.round(hours / 24);
  if (days < 7) return `${days}d ago`;
  return new Date(iso).toLocaleDateString(undefined, { month: "short", day: "numeric" });
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

function BellIcon() {
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

function ChevronIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path d="M19.5 8.25l-7.5 7.5-7.5-7.5" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round" />
    </svg>
  );
}

function LogoutIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M8.25 9V5.25A2.25 2.25 0 0110.5 3h6a2.25 2.25 0 012.25 2.25v13.5A2.25 2.25 0 0116.5 21h-6a2.25 2.25 0 01-2.25-2.25V15M12 9l-3 3m0 0l3 3m-3-3h12.75"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ProfileIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M17.982 18.725A7.488 7.488 0 0012 15.75a7.488 7.488 0 00-5.982 2.975m11.964 0a9 9 0 10-11.964 0m11.964 0A8.966 8.966 0 0112 21a8.966 8.966 0 01-5.982-2.275M15 9.75a3 3 0 11-6 0 3 3 0 016 0z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function TopBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);
  const [query, setQuery] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const [notifOpen, setNotifOpen] = useState(false);
  const [notifLoading, setNotifLoading] = useState(false);
  const [conversations, setConversations] = useState<ConversationSummary[]>([]);
  const [overdueItems, setOverdueItems] = useState<ActionItemSummary[]>([]);
  const menuRef = useRef<HTMLDivElement>(null);
  const notifRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    if (!user) return;
    function refresh() {
      getUnreadCount()
        .then((res) => setUnreadCount(res.count))
        .catch(() => {});
    }
    refresh();
    const interval = setInterval(refresh, UNREAD_POLL_MS);
    return () => clearInterval(interval);
  }, [user]);

  useEffect(() => {
    if (!notifOpen) return;
    setNotifLoading(true);
    Promise.all([listConversations(), listActionItems()])
      .then(([convos, items]) => {
        setConversations(convos);
        setOverdueItems(items);
      })
      .catch(() => {})
      .finally(() => setNotifLoading(false));
  }, [notifOpen]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
      if (notifRef.current && !notifRef.current.contains(event.target as Node)) {
        setNotifOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const isAdmin = user?.role === "ADMIN";
  const canManage = isAdmin || user?.role === "COMPANY_SECRETARY";
  const matches = useMemo(() => {
    if (!query.trim()) return [];
    const q = query.trim().toLowerCase();
    return NAV_ITEMS.filter((item) => {
      if (item.adminOnly && !isAdmin) return false;
      if (item.managementOnly && !canManage) return false;
      return item.label.toLowerCase().includes(q);
    }).slice(0, 6);
  }, [query, isAdmin, canManage]);

  const notifications = useMemo<NotificationEntry[]>(() => {
    if (!user) return [];
    const messageEntries: NotificationEntry[] = conversations
      .filter((c) => c.unreadCount > 0 && !c.muted)
      .map((c) => ({
        key: `conv-${c.id}`,
        category: "MESSAGE",
        title: conversationDisplayName(c, user.id),
        description: c.lastMessagePreview ?? "New message",
        timestamp: c.lastMessageAt ?? new Date(0).toISOString(),
        to: "/messages",
      }));

    const now = Date.now();
    const actionEntries: NotificationEntry[] = overdueItems
      .filter((item) => item.assigneeId === user.id && item.status === "OPEN" && item.dueDate && new Date(item.dueDate).getTime() < now)
      .map((item) => ({
        key: `action-${item.id}`,
        category: "ACTION ITEM",
        title: item.title,
        description: `Overdue since ${new Date(item.dueDate as string).toLocaleDateString(undefined, { month: "short", day: "numeric" })}`,
        timestamp: item.dueDate as string,
        to: "/matters-arising",
      }));

    return [...messageEntries, ...actionEntries].sort(
      (a, b) => new Date(b.timestamp).getTime() - new Date(a.timestamp).getTime(),
    );
  }, [conversations, overdueItems, user]);

  function handleSearchKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.key === "Enter" && matches.length > 0) {
      navigate(matches[0].to);
      setQuery("");
    } else if (event.key === "Escape") {
      setQuery("");
    }
  }

  if (!user) return null;

  return (
    <div className="topbar">
      <div className="topbar-search">
        <SearchIcon />
        <input
          value={query}
          onChange={(e) => setQuery(e.target.value)}
          onKeyDown={handleSearchKeyDown}
          placeholder="Search pages..."
          aria-label="Search pages"
        />
        {matches.length > 0 && (
          <div className="topbar-search-results">
            {matches.map((item) => (
              <button
                key={item.to}
                type="button"
                onClick={() => {
                  navigate(item.to);
                  setQuery("");
                }}
              >
                {item.label}
              </button>
            ))}
          </div>
        )}
      </div>

      <div className="topbar-actions">
        <div className="topbar-notif" ref={notifRef}>
          <button
            type="button"
            className="topbar-bell"
            aria-label={unreadCount > 0 ? `${unreadCount} unread messages` : "Notifications"}
            onClick={() => setNotifOpen((prev) => !prev)}
          >
            <BellIcon />
            {unreadCount > 0 && <span className="topbar-bell-dot">{unreadCount > 9 ? "9+" : unreadCount}</span>}
          </button>
          {notifOpen && (
            <div className="topbar-notif-panel">
              <div className="topbar-notif-header">
                <strong>Notifications</strong>
                <span className="topbar-notif-count">
                  {notifLoading ? "Loading..." : notifications.length === 0 ? "All caught up" : `${notifications.length} update${notifications.length === 1 ? "" : "s"}`}
                </span>
              </div>
              <div className="topbar-notif-list">
                {!notifLoading && notifications.length === 0 && (
                  <div className="topbar-notif-empty">No new messages or overdue action items.</div>
                )}
                {notifications.map((item) => (
                  <button
                    type="button"
                    key={item.key}
                    className="topbar-notif-item"
                    onClick={() => {
                      navigate(item.to);
                      setNotifOpen(false);
                    }}
                  >
                    <span className={`topbar-notif-tag topbar-notif-tag-${item.category === "MESSAGE" ? "message" : "action"}`}>
                      {item.category}
                    </span>
                    <span className="topbar-notif-item-body">
                      <span className="topbar-notif-item-title">{item.title}</span>
                      <span className="topbar-notif-item-desc">{item.description}</span>
                    </span>
                    <span className="topbar-notif-item-time">{formatRelativeTime(item.timestamp)}</span>
                  </button>
                ))}
              </div>
              <div className="topbar-notif-footer">
                <Link to="/messages" onClick={() => setNotifOpen(false)}>
                  All messages
                </Link>
                <Link to="/matters-arising" onClick={() => setNotifOpen(false)}>
                  All action items
                </Link>
              </div>
            </div>
          )}
        </div>

        <div className="topbar-user" ref={menuRef}>
          <button type="button" className="topbar-user-trigger" onClick={() => setMenuOpen((prev) => !prev)}>
            <Avatar userId={user.id} photoUpdatedAt={user.photoUpdatedAt} firstName={user.firstName} lastName={user.lastName} />
            <span className="topbar-user-text">
              <span className="topbar-user-name">
                {user.firstName} {user.lastName}
              </span>
              <span className="topbar-user-role">{ROLE_LABELS[user.role]}</span>
            </span>
            <ChevronIcon />
          </button>
          {menuOpen && (
            <div className="topbar-user-menu">
              <div className="topbar-user-menu-header">
                <strong>
                  {user.firstName} {user.lastName}
                </strong>
                <span>{user.email}</span>
              </div>
              <Link to="/profile" onClick={() => setMenuOpen(false)}>
                <ProfileIcon />
                My profile
              </Link>
              <div className="topbar-user-menu-divider" />
              <button type="button" className="topbar-user-menu-danger" onClick={logout}>
                <LogoutIcon />
                Sign out
              </button>
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
