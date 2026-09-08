import { useEffect, useMemo, useRef, useState, type KeyboardEvent } from "react";
import { useNavigate } from "react-router-dom";
import { getUnreadCount } from "../api/messaging";
import { useAuth } from "../context/AuthContext";
import { Avatar } from "./Avatar";
import { NAV_ITEMS } from "./Sidebar";

const UNREAD_POLL_MS = 25000;

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

export function TopBar() {
  const { user, logout } = useAuth();
  const navigate = useNavigate();
  const [unreadCount, setUnreadCount] = useState(0);
  const [query, setQuery] = useState("");
  const [menuOpen, setMenuOpen] = useState(false);
  const menuRef = useRef<HTMLDivElement>(null);

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
    function handleClickOutside(event: MouseEvent) {
      if (menuRef.current && !menuRef.current.contains(event.target as Node)) {
        setMenuOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  const isAdmin = user?.role === "ADMIN";
  const canManage = isAdmin || user?.role === "EXECUTIVE";
  const matches = useMemo(() => {
    if (!query.trim()) return [];
    const q = query.trim().toLowerCase();
    return NAV_ITEMS.filter((item) => {
      if (item.adminOnly && !isAdmin) return false;
      if (item.managementOnly && !canManage) return false;
      return item.label.toLowerCase().includes(q);
    }).slice(0, 6);
  }, [query, isAdmin, canManage]);

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
        <button
          type="button"
          className="topbar-bell"
          aria-label={unreadCount > 0 ? `${unreadCount} unread messages` : "Notifications"}
          onClick={() => navigate("/messages")}
        >
          <BellIcon />
          {unreadCount > 0 && <span className="topbar-bell-dot">{unreadCount > 9 ? "9+" : unreadCount}</span>}
        </button>

        <div className="topbar-user" ref={menuRef}>
          <button type="button" className="topbar-user-trigger" onClick={() => setMenuOpen((prev) => !prev)}>
            <Avatar userId={user.id} photoUpdatedAt={user.photoUpdatedAt} firstName={user.firstName} lastName={user.lastName} />
            <span className="topbar-user-text">
              <span className="topbar-user-name">
                {user.firstName} {user.lastName}
              </span>
              <span className="topbar-user-role">{user.role.replace("_", " ")}</span>
            </span>
            <ChevronIcon />
          </button>
          {menuOpen && (
            <div className="topbar-user-menu">
              <button type="button" onClick={logout}>
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
