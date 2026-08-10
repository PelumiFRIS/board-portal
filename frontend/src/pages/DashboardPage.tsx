import { useEffect, useState, type FormEvent } from "react";
import { createUser, listOrganizationUsers, updateUserStatus } from "../api/auth";
import { extractErrorMessage } from "../api/client";
import type { Role, UserSummary } from "../api/types";
import { useAuth } from "../context/AuthContext";

const ROLE_OPTIONS: Role[] = ["BOARD_MEMBER", "EXECUTIVE", "ADMIN"];

export function DashboardPage() {
  const { user, logout } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [users, setUsers] = useState<UserSummary[]>([]);
  const [usersError, setUsersError] = useState<string | null>(null);
  const [loadingUsers, setLoadingUsers] = useState(isAdmin);

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
    <div className="dashboard">
      <header className="dashboard-header">
        <div>
          <h1>{user.organizationName}</h1>
          <p>
            Welcome, {user.firstName} {user.lastName} &middot; {user.role}
          </p>
        </div>
        <button className="secondary" onClick={logout}>
          Sign out
        </button>
      </header>

      {isAdmin && (
        <section className="dashboard-section">
          <h2>Board & team members</h2>
          {loadingUsers && <p>Loading users...</p>}
          {usersError && <p className="form-error">{usersError}</p>}
          {statusError && <p className="form-error">{statusError}</p>}
          {!loadingUsers && !usersError && (
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
                      {u.firstName} {u.lastName}
                    </td>
                    <td>{u.email}</td>
                    <td>{u.role}</td>
                    <td>{u.status}</td>
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
    </div>
  );
}
