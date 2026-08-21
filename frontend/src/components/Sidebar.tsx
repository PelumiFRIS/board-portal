import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";
import { Avatar } from "./Avatar";

function DashboardIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M2.25 12l8.954-8.955c.44-.439 1.152-.439 1.591 0L21.75 12M4.5 9.75v10.125c0 .621.504 1.125 1.125 1.125H9.75v-4.875c0-.621.504-1.125 1.125-1.125h2.25c.621 0 1.125.504 1.125 1.125V21h4.125c.621 0 1.125-.504 1.125-1.125V9.75"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function MeetingsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M6.75 3v2.25M17.25 3v2.25M3 18.75V7.5a2.25 2.25 0 012.25-2.25h13.5A2.25 2.25 0 0121 7.5v11.25a2.25 2.25 0 01-2.25 2.25H5.25A2.25 2.25 0 013 18.75zm0-7.5h18"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function DocumentsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function AuditIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M12 8v4l2.5 2.5M21 12a9 9 0 11-9-9 9 9 0 019 9z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function DirectoryIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M15 19.128a9.38 9.38 0 002.625.372 9.337 9.337 0 004.121-.952 4.125 4.125 0 00-7.533-2.493M15 19.128v-.003c0-1.113-.285-2.16-.786-3.07M15 19.128v.106A12.318 12.318 0 018.624 21c-2.331 0-4.512-.645-6.374-1.766l-.001-.109a6.375 6.375 0 0111.964-3.07M12 6.375a3.375 3.375 0 11-6.75 0 3.375 3.375 0 016.75 0zm8.25 2.25a2.625 2.625 0 11-5.25 0 2.625 2.625 0 015.25 0z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function CommitteesIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

function ComplianceIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M9 12h3.75M9 15h3.75M9 18h3.75M17.25 4.5v15a.75.75 0 01-.75.75H6a.75.75 0 01-.75-.75v-15A.75.75 0 016 3.75h10.5a.75.75 0 01.75.75zM12.75 7.5a.75.75 0 01-.75.75h-2.25a.75.75 0 01-.75-.75V6.75a.75.75 0 01.75-.75H12a.75.75 0 01.75.75v.75z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

const NAV_ITEMS = [
  { to: "/dashboard", label: "Dashboard", icon: DashboardIcon, adminOnly: false },
  { to: "/meetings", label: "Meetings", icon: MeetingsIcon, adminOnly: false },
  { to: "/documents", label: "Documents", icon: DocumentsIcon, adminOnly: false },
  { to: "/directory", label: "Directory", icon: DirectoryIcon, adminOnly: false },
  { to: "/committees", label: "Committees", icon: CommitteesIcon, adminOnly: false },
  { to: "/compliance", label: "Compliance", icon: ComplianceIcon, adminOnly: false },
  { to: "/audit", label: "Audit Trail", icon: AuditIcon, adminOnly: true },
];

export function Sidebar() {
  const { user, logout } = useAuth();
  const location = useLocation();

  if (!user) return null;

  const visibleNavItems = NAV_ITEMS.filter((item) => !item.adminOnly || user.role === "ADMIN");

  return (
    <aside className="sidebar">
      <div className="sidebar-logo">
        <img src="/logo.png" alt="FirstRegistrars" />
      </div>
      <div className="sidebar-org">{user.organizationName}</div>
      <nav className="sidebar-nav">
        {visibleNavItems.map(({ to, label, icon: Icon }) => (
          <Link
            key={to}
            to={to}
            className={location.pathname === to || location.pathname.startsWith(`${to}/`) ? "active" : ""}
          >
            <Icon />
            {label}
          </Link>
        ))}
      </nav>
      <div className="sidebar-footer">
        <div className="sidebar-user">
          <Avatar userId={user.id} photoUpdatedAt={user.photoUpdatedAt} firstName={user.firstName} lastName={user.lastName} />
          <div>
            <div className="sidebar-user-name">
              {user.firstName} {user.lastName}
            </div>
            <div className="sidebar-user-role">{user.role}</div>
          </div>
        </div>
        <button onClick={logout}>Sign out</button>
      </div>
    </aside>
  );
}
