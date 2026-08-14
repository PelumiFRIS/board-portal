import { useEffect, useState, type FormEvent } from "react";
import { listDirectory, updateUserProfile } from "../api/auth";
import { extractErrorMessage } from "../api/client";
import type { UserSummary } from "../api/types";
import { Avatar } from "../components/Avatar";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

export function DirectoryPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [members, setMembers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editPhone, setEditPhone] = useState("");
  const [editBio, setEditBio] = useState("");
  const [editCommittees, setEditCommittees] = useState("");
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    listDirectory()
      .then(setMembers)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  function startEditing(member: UserSummary) {
    setActionError(null);
    setEditingId(member.id);
    setEditTitle(member.title ?? "");
    setEditPhone(member.phone ?? "");
    setEditBio(member.bio ?? "");
    setEditCommittees(member.committees ?? "");
  }

  async function handleSave(event: FormEvent, memberId: string) {
    event.preventDefault();
    setActionError(null);
    setSaving(true);
    try {
      const updated = await updateUserProfile(memberId, {
        title: editTitle,
        phone: editPhone,
        bio: editBio,
        committees: editCommittees,
      });
      setMembers((prev) => prev.map((m) => (m.id === updated.id ? updated : m)));
      setEditingId(null);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Directory</h1>
          <p>Board and team members at {user.organizationName}</p>
        </div>

        {loading && <p>Loading directory...</p>}
        {loadError && <p className="form-error">{loadError}</p>}
        {actionError && <p className="form-error">{actionError}</p>}

        {!loading && !loadError && (
          <div className="member-grid">
            {members.map((member) =>
              editingId === member.id ? (
                <form key={member.id} className="member-card add-user-form" onSubmit={(e) => handleSave(e, member.id)}>
                  <div className="name-cell">
                    <Avatar firstName={member.firstName} lastName={member.lastName} />
                    <strong>
                      {member.firstName} {member.lastName}
                    </strong>
                  </div>
                  <label>
                    Title
                    <input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} placeholder="e.g. Chairperson" />
                  </label>
                  <label>
                    Phone
                    <input value={editPhone} onChange={(e) => setEditPhone(e.target.value)} />
                  </label>
                  <label>
                    Bio
                    <input value={editBio} onChange={(e) => setEditBio(e.target.value)} />
                  </label>
                  <label>
                    Committees
                    <input
                      value={editCommittees}
                      onChange={(e) => setEditCommittees(e.target.value)}
                      placeholder="e.g. Audit Committee, Risk Committee"
                    />
                  </label>
                  <div className="field-row">
                    <button type="submit" disabled={saving}>
                      {saving ? "Saving..." : "Save"}
                    </button>
                    <button type="button" className="secondary" onClick={() => setEditingId(null)}>
                      Cancel
                    </button>
                  </div>
                </form>
              ) : (
                <div key={member.id} className="member-card">
                  <div className="name-cell">
                    <Avatar firstName={member.firstName} lastName={member.lastName} />
                    <div>
                      <div>
                        <strong>
                          {member.firstName} {member.lastName}
                        </strong>
                      </div>
                      <div className="table-hint">{member.title || member.role}</div>
                    </div>
                  </div>
                  <p className="table-hint">{member.email}</p>
                  {member.phone && <p className="table-hint">{member.phone}</p>}
                  {member.committees && <p className="table-hint">{member.committees}</p>}
                  {member.bio && <p>{member.bio}</p>}
                  {isAdmin && (
                    <button className="secondary small" onClick={() => startEditing(member)}>
                      Edit
                    </button>
                  )}
                </div>
              ),
            )}
          </div>
        )}
      </main>
    </div>
  );
}
