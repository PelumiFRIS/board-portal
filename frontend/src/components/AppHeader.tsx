import { Link, useLocation } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function AppHeader() {
  const { user, logout } = useAuth();
  const location = useLocation();

  if (!user) return null;

  return (
    <header className="dashboard-header">
      <div>
        <h1>{user.organizationName}</h1>
        <p>
          Welcome, {user.firstName} {user.lastName} &middot; {user.role}
        </p>
        <nav className="app-nav">
          <Link to="/dashboard" className={location.pathname === "/dashboard" ? "active" : ""}>
            Dashboard
          </Link>
          <Link to="/meetings" className={location.pathname.startsWith("/meetings") ? "active" : ""}>
            Meetings
          </Link>
        </nav>
      </div>
      <button className="secondary" onClick={logout}>
        Sign out
      </button>
    </header>
  );
}
