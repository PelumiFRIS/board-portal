import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { createUser, listOrganizationUsers, updateUserStatus } from "../api/auth";
import { listActionItems, updateActionItemStatus } from "../api/actionItems";
import { extractErrorMessage } from "../api/client";
import { getDashboardStats } from "../api/dashboard";
import { listDocuments } from "../api/documents";
import { listMeetings } from "../api/meetings";
import { castVote, listResolutions } from "../api/resolutions";
import type {
  ActionItemSummary,
  DashboardStats as DashboardStatsType,
  DocumentSummary,
  MeetingSummary,
  ResolutionSummary,
  Role,
  UserSummary,
  VoteChoice,
} from "../api/types";
import { Avatar } from "../components/Avatar";
import { DashboardStats } from "../components/DashboardStats";
import { Sidebar } from "../components/Sidebar";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";

const ROLE_OPTIONS: Role[] = ["BOARD_MEMBER", "EXECUTIVE", "ADMIN"];

function AllCaughtUpIllustration() {
  return (
    <svg width="176" height="150" viewBox="0 0 176 150" fill="none" aria-hidden="true">
      <ellipse cx="88" cy="129" rx="70" ry="10" fill="var(--primary-tint)" />
      <rect x="30" y="14" width="92" height="112" rx="10" fill="var(--surface)" stroke="var(--border)" strokeWidth="1.5" />
      <rect x="30" y="14" width="92" height="30" rx="10" fill="var(--primary-tint)" />
      <path d="M30 34h92" stroke="var(--border)" strokeWidth="1.5" />
      <rect x="44" y="24" width="40" height="6" rx="3" fill="var(--primary)" opacity="0.55" />
      <rect x="44" y="60" width="64" height="5" rx="2.5" fill="var(--border)" />
      <rect x="44" y="74" width="52" height="5" rx="2.5" fill="var(--border)" />
      <rect x="44" y="88" width="58" height="5" rx="2.5" fill="var(--border)" />
      <rect x="44" y="102" width="34" height="5" rx="2.5" fill="var(--border)" />
      <circle cx="132" cy="104" r="26" fill="var(--accent)" />
      <path
        d="M120.5 104.5l7 7 14.5-15"
        stroke="#12406e"
        strokeWidth="4.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
      <circle cx="24" cy="46" r="4" fill="var(--accent)" opacity="0.6" />
      <circle cx="146" cy="40" r="3" fill="var(--primary)" opacity="0.3" />
      <circle cx="18" cy="96" r="3" fill="var(--primary)" opacity="0.3" />
    </svg>
  );
}

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function MemberDashboard() {
  const { user } = useAuth();
  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [resolutions, setResolutions] = useState<ResolutionSummary[]>([]);
  const [actionItems, setActionItems] = useState<ActionItemSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [votingId, setVotingId] = useState<string | null>(null);
  const [voteError, setVoteError] = useState<string | null>(null);
  const [busyActionItemId, setBusyActionItemId] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([listMeetings(), listDocuments(), listResolutions(), listActionItems()])
      .then(([m, d, r, a]) => {
        setMeetings(m);
        setDocuments(d);
        setResolutions(r);
        setActionItems(a);
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function handleVote(resolutionId: string, choice: VoteChoice) {
    setVoteError(null);
    setVotingId(resolutionId);
    try {
      const updated = await castVote(resolutionId, choice);
      setResolutions((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
    } catch (err) {
      setVoteError(extractErrorMessage(err));
    } finally {
      setVotingId(null);
    }
  }

  async function handleMarkDone(itemId: string) {
    setVoteError(null);
    setBusyActionItemId(itemId);
    try {
      const updated = await updateActionItemStatus(itemId, "DONE");
      setActionItems((prev) => prev.map((i) => (i.id === updated.id ? updated : i)));
    } catch (err) {
      setVoteError(extractErrorMessage(err));
    } finally {
      setBusyActionItemId(null);
    }
  }

  if (loading) return <p>Loading your dashboard...</p>;
  if (loadError) return <p className="form-error">{loadError}</p>;
  if (!user) return null;

  const meetingTitleById = new Map(meetings.map((m) => [m.id, m.title]));
  const needsVote = resolutions.filter((r) => r.status === "OPEN" && r.myVote === null);
  const myActionItems = actionItems
    .filter((i) => i.assigneeId === user.id && i.status === "OPEN")
    .sort((a, b) => {
      if (!a.dueDate) return 1;
      if (!b.dueDate) return -1;
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    });
  const upcomingMeetings = meetings
    .filter((m) => m.status === "SCHEDULED")
    .sort((a, b) => new Date(a.scheduledStart).getTime() - new Date(b.scheduledStart).getTime())
    .slice(0, 3);
  const recentDocuments = documents.slice(0, 5);

  const nothingToShow =
    needsVote.length === 0 && myActionItems.length === 0 && upcomingMeetings.length === 0 && recentDocuments.length === 0;

  if (nothingToShow) {
    return (
      <section className="dashboard-section">
        <div className="empty-hero">
          <AllCaughtUpIllustration />
          <h2>You&apos;re all caught up</h2>
          <p>No action items, meetings, resolutions, or documents need your attention right now.</p>
          <div className="field-row">
            <Link to="/meetings">
              <button className="secondary small">View meetings</button>
            </Link>
            <Link to="/documents">
              <button className="secondary small">View documents</button>
            </Link>
          </div>
        </div>
      </section>
    );
  }

  return (
    <>
      {myActionItems.length > 0 && (
        <section className="dashboard-section">
          <h2>My action items</h2>
          {voteError && <p className="form-error">{voteError}</p>}
          {myActionItems.map((item) => (
            <div key={item.id} className="resolution-card">
              <div className="resolution-card-header">
                <strong>{item.title}</strong>
                <Link to={`/meetings/${item.meetingId}`} className="table-hint">
                  {meetingTitleById.get(item.meetingId) ?? "View meeting"} &rarr;
                </Link>
              </div>
              {item.description && <p>{item.description}</p>}
              <p className="table-hint">{item.dueDate ? `Due ${new Date(item.dueDate).toLocaleDateString()}` : "No due date"}</p>
              <button className="small" disabled={busyActionItemId === item.id} onClick={() => handleMarkDone(item.id)}>
                {busyActionItemId === item.id ? "Saving..." : "Mark done"}
              </button>
            </div>
          ))}
        </section>
      )}

      {needsVote.length > 0 && (
        <section className="dashboard-section">
          <h2>Needs your vote</h2>
          {voteError && <p className="form-error">{voteError}</p>}
          {needsVote.map((r) => (
            <div key={r.id} className="resolution-card">
              <div className="resolution-card-header">
                <strong>{r.title}</strong>
                <Link to={`/meetings/${r.meetingId}`} className="table-hint">
                  {meetingTitleById.get(r.meetingId) ?? "View meeting"} &rarr;
                </Link>
              </div>
              {r.description && <p>{r.description}</p>}
              <div className="field-row">
                <button className="small" disabled={votingId === r.id} onClick={() => handleVote(r.id, "FOR")}>
                  Vote for
                </button>
                <button
                  className="secondary small"
                  disabled={votingId === r.id}
                  onClick={() => handleVote(r.id, "AGAINST")}
                >
                  Vote against
                </button>
                <button
                  className="secondary small"
                  disabled={votingId === r.id}
                  onClick={() => handleVote(r.id, "ABSTAIN")}
                >
                  Abstain
                </button>
              </div>
            </div>
          ))}
        </section>
      )}

      <section className="dashboard-section">
        <h2>Upcoming meetings</h2>
        {upcomingMeetings.length === 0 && <p className="table-hint">Nothing scheduled.</p>}
        {upcomingMeetings.map((m) => (
          <div key={m.id} className="agenda-item-row">
            <div className="agenda-item-body">
              <Link to={`/meetings/${m.id}`}>
                <strong>{m.title}</strong>
              </Link>
              <p>
                {new Date(m.scheduledStart).toLocaleString()}
                {m.location ? ` · ${m.location}` : ""}
              </p>
            </div>
            <StatusBadge status={m.status} />
          </div>
        ))}
        <p>
          <Link to="/meetings">See all meetings &rarr;</Link>
        </p>
      </section>

      <section className="dashboard-section">
        <h2>Recent documents</h2>
        {recentDocuments.length === 0 && <p className="table-hint">No documents yet.</p>}
        {recentDocuments.map((doc) => (
          <div key={doc.id} className="document-row">
            <div>
              <strong>{doc.title}</strong>{" "}
              <span className="badge badge-category">{doc.category.replace("_", " ")}</span>
            </div>
            <span className="table-hint">{formatFileSize(doc.fileSize)}</span>
          </div>
        ))}
        <p>
          <Link to="/documents">See all documents &rarr;</Link>
        </p>
      </section>
    </>
  );
}

export function DashboardPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [users, setUsers] = useState<UserSummary[]>([]);
  const [usersError, setUsersError] = useState<string | null>(null);
  const [loadingUsers, setLoadingUsers] = useState(isAdmin);

  const [stats, setStats] = useState<DashboardStatsType | null>(null);
  const [statsError, setStatsError] = useState<string | null>(null);

  const [firstName, setFirstName] = useState("");
  const [lastName, setLastName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [role, setRole] = useState<Role>("BOARD_MEMBER");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [statusUpdatingId, setStatusUpdatingId] = useState<string | null>(null);
  const [statusError, setStatusError] = useState<string | null>(null);

  useEffect(() => {
    if (!isAdmin) return;
    listOrganizationUsers()
      .then(setUsers)
      .catch((err) => setUsersError(extractErrorMessage(err)))
      .finally(() => setLoadingUsers(false));
  }, [isAdmin]);

  useEffect(() => {
    getDashboardStats()
      .then(setStats)
      .catch((err) => setStatsError(extractErrorMessage(err)));
  }, []);

  async function handleAddUser(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      const created = await createUser({ firstName, lastName, email, password, role });
      setUsers((prev) => [...prev, created]);
      setFirstName("");
      setLastName("");
      setEmail("");
      setPassword("");
      setRole("BOARD_MEMBER");
    } catch (err) {
      setFormError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleToggleStatus(target: UserSummary) {
    const nextStatus = target.status === "ACTIVE" ? "DISABLED" : "ACTIVE";
    setStatusError(null);
    setStatusUpdatingId(target.id);
    try {
      const updated = await updateUserStatus(target.id, nextStatus);
      setUsers((prev) => prev.map((u) => (u.id === updated.id ? updated : u)));
    } catch (err) {
      setStatusError(extractErrorMessage(err));
    } finally {
      setStatusUpdatingId(null);
    }
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Welcome, {user.firstName}</h1>
          <p>{user.organizationName} &middot; {user.role}</p>
        </div>

        {statsError && <p className="form-error">{statsError}</p>}
        {stats && <DashboardStats stats={stats} />}

        {!isAdmin && <MemberDashboard />}

        {isAdmin && (
          <section className="dashboard-section">
            <h2>Board & team members</h2>
            {loadingUsers && <p>Loading users...</p>}
            {usersError && <p className="form-error">{usersError}</p>}
            {statusError && <p className="form-error">{statusError}</p>}
            {!loadingUsers && !usersError && (
              <div className="table-scroll">
              <table className="user-table">
                <thead>
                  <tr>
                    <th>Name</th>
                    <th>Email</th>
                    <th>Role</th>
                    <th>Status</th>
                    <th></th>
                  </tr>
                </thead>
                <tbody>
                  {users.map((u) => (
                    <tr key={u.id}>
                      <td>
                        <div className="name-cell">
                          <Avatar userId={u.id} photoUpdatedAt={u.photoUpdatedAt} firstName={u.firstName} lastName={u.lastName} />
                          {u.firstName} {u.lastName}
                        </div>
                      </td>
                      <td>{u.email}</td>
                      <td>{u.role}</td>
                      <td>
                        <StatusBadge status={u.status} />
                      </td>
                      <td>
                        {u.id === user.id ? (
                          <span className="table-hint">You</span>
                        ) : (
                          <button
                            className="secondary small"
                            onClick={() => handleToggleStatus(u)}
                            disabled={statusUpdatingId === u.id}
                          >
                            {statusUpdatingId === u.id
                              ? "Saving..."
                              : u.status === "ACTIVE"
                                ? "Deactivate"
                                : "Reactivate"}
                          </button>
                        )}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
              </div>
            )}

            <h3>Add a member</h3>
            <form className="add-user-form" onSubmit={handleAddUser}>
              <div className="field-row">
                <label>
                  First name
                  <input value={firstName} onChange={(e) => setFirstName(e.target.value)} required />
                </label>
                <label>
                  Last name
                  <input value={lastName} onChange={(e) => setLastName(e.target.value)} required />
                </label>
              </div>
              <div className="field-row">
                <label>
                  Email
                  <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
                </label>
                <label>
                  Temporary password
                  <input
                    type="password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    minLength={8}
                    required
                  />
                </label>
              </div>
              <label>
                Role
                <select value={role} onChange={(e) => setRole(e.target.value as Role)}>
                  {ROLE_OPTIONS.map((option) => (
                    <option key={option} value={option}>
                      {option}
                    </option>
                  ))}
                </select>
              </label>
              {formError && <p className="form-error">{formError}</p>}
              <button type="submit" disabled={submitting}>
                {submitting ? "Adding..." : "Add member"}
              </button>
            </form>
          </section>
        )}
      </main>
    </div>
  );
}
