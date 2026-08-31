import { useEffect, useState, type FormEvent } from "react";
import { listDirectory } from "../api/auth";
import {
  addCommitteeMember,
  createCommittee,
  deleteCommittee,
  listCommittees,
  removeCommitteeMember,
  setCommitteeChair,
  updateCommittee,
} from "../api/committees";
import { extractErrorMessage } from "../api/client";
import type { CommitteeSummary, UserSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

function EmptyCommitteesIcon() {
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

export function CommitteesPage() {
  const { user } = useAuth();
  const canManage = user?.role === "ADMIN" || user?.role === "EXECUTIVE";

  const [committees, setCommittees] = useState<CommitteeSummary[]>([]);
  const [orgUsers, setOrgUsers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [newName, setNewName] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [newParentId, setNewParentId] = useState("");
  const [creating, setCreating] = useState(false);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editName, setEditName] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [savingEdit, setSavingEdit] = useState(false);

  const [addSelections, setAddSelections] = useState<Record<string, string>>({});
  const [busyCommitteeId, setBusyCommitteeId] = useState<string | null>(null);

  useEffect(() => {
    const requests: [Promise<CommitteeSummary[]>, Promise<UserSummary[]>] = [
      listCommittees(),
      canManage ? listDirectory() : Promise.resolve([]),
    ];
    Promise.all(requests)
      .then(([committeeList, users]) => {
        setCommittees(committeeList);
        setOrgUsers(users);
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [canManage]);

  function replaceCommitteeInTree(list: CommitteeSummary[], updated: CommitteeSummary): CommitteeSummary[] {
    if (updated.parentCommitteeId === null) {
      return list.map((c) => (c.id === updated.id ? updated : c));
    }
    return list.map((c) =>
      c.id === updated.parentCommitteeId
        ? { ...c, subCommittees: c.subCommittees.map((sc) => (sc.id === updated.id ? updated : sc)) }
        : c,
    );
  }

  function removeCommitteeFromTree(list: CommitteeSummary[], id: string): CommitteeSummary[] {
    return list
      .filter((c) => c.id !== id)
      .map((c) => ({ ...c, subCommittees: c.subCommittees.filter((sc) => sc.id !== id) }));
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setActionError(null);
    setCreating(true);
    try {
      const created = await createCommittee({
        name: newName,
        description: newDescription || undefined,
        parentCommitteeId: newParentId || undefined,
      });
      setCommittees((prev) => {
        if (created.parentCommitteeId === null) {
          return [...prev, created].sort((a, b) => a.name.localeCompare(b.name));
        }
        return prev.map((c) =>
          c.id === created.parentCommitteeId
            ? { ...c, subCommittees: [...c.subCommittees, created].sort((a, b) => a.name.localeCompare(b.name)) }
            : c,
        );
      });
      setNewName("");
      setNewDescription("");
      setNewParentId("");
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setCreating(false);
    }
  }

  function startEditing(committee: CommitteeSummary) {
    setActionError(null);
    setEditingId(committee.id);
    setEditName(committee.name);
    setEditDescription(committee.description ?? "");
  }

  async function handleSaveEdit(event: FormEvent, committeeId: string) {
    event.preventDefault();
    setActionError(null);
    setSavingEdit(true);
    try {
      const updated = await updateCommittee(committeeId, { name: editName, description: editDescription });
      setCommittees((prev) => replaceCommitteeInTree(prev, updated));
      setEditingId(null);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSavingEdit(false);
    }
  }

  async function handleDelete(committeeId: string) {
    setActionError(null);
    setBusyCommitteeId(committeeId);
    try {
      await deleteCommittee(committeeId);
      setCommittees((prev) => removeCommitteeFromTree(prev, committeeId));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyCommitteeId(null);
    }
  }

  async function handleAddMember(committeeId: string) {
    const userId = addSelections[committeeId];
    if (!userId) return;
    setActionError(null);
    setBusyCommitteeId(committeeId);
    try {
      const updated = await addCommitteeMember(committeeId, userId);
      setCommittees((prev) => replaceCommitteeInTree(prev, updated));
      setAddSelections((prev) => ({ ...prev, [committeeId]: "" }));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyCommitteeId(null);
    }
  }

  async function handleRemoveMember(committeeId: string, userId: string) {
    setActionError(null);
    setBusyCommitteeId(committeeId);
    try {
      const updated = await removeCommitteeMember(committeeId, userId);
      setCommittees((prev) => replaceCommitteeInTree(prev, updated));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyCommitteeId(null);
    }
  }

  async function handleSetChair(committeeId: string, userId: string) {
    setActionError(null);
    setBusyCommitteeId(committeeId);
    try {
      const updated = await setCommitteeChair(committeeId, userId);
      setCommittees((prev) => replaceCommitteeInTree(prev, updated));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyCommitteeId(null);
    }
  }

  function renderCommitteeCard(committee: CommitteeSummary, indent: boolean) {
    const availableUsers = orgUsers.filter((u) => !committee.members.some((m) => m.userId === u.id));
    const busy = busyCommitteeId === committee.id;
    return (
      <div
        key={committee.id}
        className="resolution-card"
        style={indent ? { marginLeft: 32, borderLeft: "3px solid var(--border-color, #ddd)" } : undefined}
      >
        {editingId === committee.id ? (
          <form className="add-user-form" onSubmit={(e) => handleSaveEdit(e, committee.id)}>
            <label>
              Name
              <input value={editName} onChange={(e) => setEditName(e.target.value)} required />
            </label>
            <label>
              Description
              <input value={editDescription} onChange={(e) => setEditDescription(e.target.value)} />
            </label>
            <div className="field-row">
              <button type="submit" disabled={savingEdit}>
                {savingEdit ? "Saving..." : "Save"}
              </button>
              <button type="button" className="secondary" onClick={() => setEditingId(null)}>
                Cancel
              </button>
            </div>
          </form>
        ) : (
          <>
            <div className="resolution-card-header">
              <div>
                <strong>{committee.name}</strong>
                {indent && <span className="badge badge-category"> Sub-committee</span>}
                {committee.description && <p className="table-hint">{committee.description}</p>}
              </div>
              {canManage && (
                <div className="field-row">
                  <button className="secondary small" onClick={() => startEditing(committee)}>
                    Edit
                  </button>
                  <button className="secondary small" disabled={busy} onClick={() => handleDelete(committee.id)}>
                    Delete
                  </button>
                </div>
              )}
            </div>

            {committee.members.length === 0 ? (
              <p className="table-hint">No members yet.</p>
            ) : (
              <ul className="vote-record-list">
                {committee.members.map((member) => (
                  <li key={member.userId}>
                    {member.firstName} {member.lastName}
                    {member.isChair && <span className="badge badge-category"> Chair</span>}
                    {canManage && (
                      <span className="field-row" style={{ marginLeft: 12 }}>
                        {!member.isChair && (
                          <button
                            type="button"
                            className="secondary small"
                            disabled={busy}
                            onClick={() => handleSetChair(committee.id, member.userId)}
                          >
                            Make chair
                          </button>
                        )}
                        <button
                          type="button"
                          className="secondary small"
                          disabled={busy}
                          onClick={() => handleRemoveMember(committee.id, member.userId)}
                        >
                          Remove
                        </button>
                      </span>
                    )}
                  </li>
                ))}
              </ul>
            )}

            {canManage && availableUsers.length > 0 && (
              <div className="field-row">
                <select
                  value={addSelections[committee.id] ?? ""}
                  onChange={(e) => setAddSelections((prev) => ({ ...prev, [committee.id]: e.target.value }))}
                >
                  <option value="">Add a member...</option>
                  {availableUsers.map((u) => (
                    <option key={u.id} value={u.id}>
                      {u.firstName} {u.lastName}
                    </option>
                  ))}
                </select>
                <button
                  type="button"
                  className="secondary small"
                  disabled={busy || !addSelections[committee.id]}
                  onClick={() => handleAddMember(committee.id)}
                >
                  Add
                </button>
              </div>
            )}
          </>
        )}
      </div>
    );
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Committees</h1>
          <p>Board committee structure at {user.organizationName}</p>
        </div>

        <section className="dashboard-section">
          {loading && <p>Loading committees...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {actionError && <p className="form-error">{actionError}</p>}

          {!loading && !loadError && committees.length === 0 && (
            <div className="empty-state">
              <EmptyCommitteesIcon />
              <p>No committees have been set up yet.</p>
            </div>
          )}

          {!loading &&
            !loadError &&
            committees.map((committee) => (
              <div key={committee.id}>
                {renderCommitteeCard(committee, false)}
                {committee.subCommittees.map((sub) => renderCommitteeCard(sub, true))}
              </div>
            ))}

          {canManage && (
            <>
              <h3>Create a committee</h3>
              <form className="add-user-form" onSubmit={handleCreate}>
                <label>
                  Name
                  <input value={newName} onChange={(e) => setNewName(e.target.value)} required />
                </label>
                <label>
                  Description
                  <input value={newDescription} onChange={(e) => setNewDescription(e.target.value)} />
                </label>
                <label>
                  Parent committee (optional)
                  <select value={newParentId} onChange={(e) => setNewParentId(e.target.value)}>
                    <option value="">None — top-level committee</option>
                    {committees.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.name}
                      </option>
                    ))}
                  </select>
                </label>
                <button type="submit" disabled={creating}>
                  {creating ? "Creating..." : "Create committee"}
                </button>
              </form>
            </>
          )}
        </section>
      </main>
    </div>
  );
}
