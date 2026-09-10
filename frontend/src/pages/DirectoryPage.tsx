import { useEffect, useState, type FormEvent } from "react";
import { listDirectory, updateUserProfile } from "../api/auth";
import { extractErrorMessage } from "../api/client";
import { onboardClient } from "../api/organizations";
import { deleteUserPhoto, uploadUserPhoto } from "../api/userPhotos";
import type { UserSummary } from "../api/types";
import { Avatar } from "../components/Avatar";
import { Sidebar } from "../components/Sidebar";
import { TopBar } from "../components/TopBar";
import { ROLE_LABELS } from "../constants/roles";
import { useAuth } from "../context/AuthContext";

export function DirectoryPage() {
  const { user, refreshUser } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [members, setMembers] = useState<UserSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editPhone, setEditPhone] = useState("");
  const [editBio, setEditBio] = useState("");
  const [photoFile, setPhotoFile] = useState<File | null>(null);
  const [saving, setSaving] = useState(false);
  const [removingPhotoId, setRemovingPhotoId] = useState<string | null>(null);

  const [clientOrgName, setClientOrgName] = useState("");
  const [clientFirstName, setClientFirstName] = useState("");
  const [clientLastName, setClientLastName] = useState("");
  const [clientEmail, setClientEmail] = useState("");
  const [onboarding, setOnboarding] = useState(false);
  const [onboardError, setOnboardError] = useState<string | null>(null);
  const [revealedOnboard, setRevealedOnboard] = useState<{ orgName: string; email: string; password: string } | null>(
    null,
  );
  const [onboardCopied, setOnboardCopied] = useState(false);

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
    setPhotoFile(null);
  }

  async function handleSave(event: FormEvent, memberId: string) {
    event.preventDefault();
    setActionError(null);
    setSaving(true);
    try {
      if (photoFile) {
        await uploadUserPhoto(memberId, photoFile);
      }
      const updated = await updateUserProfile(memberId, {
        title: editTitle,
        phone: editPhone,
        bio: editBio,
      });
      setMembers((prev) => prev.map((m) => (m.id === updated.id ? updated : m)));
      setEditingId(null);
      setPhotoFile(null);
      if (memberId === user?.id) await refreshUser();
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSaving(false);
    }
  }

  async function handleRemovePhoto(memberId: string) {
    setActionError(null);
    setRemovingPhotoId(memberId);
    try {
      await deleteUserPhoto(memberId);
      setMembers((prev) => prev.map((m) => (m.id === memberId ? { ...m, photoUpdatedAt: null } : m)));
      if (memberId === user?.id) await refreshUser();
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setRemovingPhotoId(null);
    }
  }

  async function handleOnboardClient(event: FormEvent) {
    event.preventDefault();
    setOnboardError(null);
    setOnboarding(true);
    try {
      const result = await onboardClient({
        organizationName: clientOrgName,
        adminFirstName: clientFirstName,
        adminLastName: clientLastName,
        adminEmail: clientEmail,
      });
      setRevealedOnboard({
        orgName: clientOrgName,
        email: result.admin.email,
        password: result.temporaryPassword,
      });
      setOnboardCopied(false);
      setClientOrgName("");
      setClientFirstName("");
      setClientLastName("");
      setClientEmail("");
    } catch (err) {
      setOnboardError(extractErrorMessage(err));
    } finally {
      setOnboarding(false);
    }
  }

  async function handleCopyOnboard() {
    if (!revealedOnboard) return;
    await navigator.clipboard.writeText(revealedOnboard.password);
    setOnboardCopied(true);
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <TopBar />
        <div className="page-header">
          <h1>Directory</h1>
          <p>Board and team members at {user.organizationName}</p>
        </div>

        {loading && <p>Loading directory...</p>}
        {loadError && <p className="form-error">{loadError}</p>}
        {actionError && <p className="form-error">{actionError}</p>}

        {!loading && !loadError && (
          <div className="member-grid">
            {members.map((member) => {
              const canEdit = isAdmin || member.id === user.id;
              return editingId === member.id ? (
                <form key={member.id} className="member-card add-user-form" onSubmit={(e) => handleSave(e, member.id)}>
                  <div className="name-cell">
                    <Avatar
                      userId={member.id}
                      photoUpdatedAt={member.photoUpdatedAt}
                      firstName={member.firstName}
                      lastName={member.lastName}
                    />
                    <strong>
                      {member.firstName} {member.lastName}
                    </strong>
                  </div>
                  <label>
                    Photo
                    <input type="file" accept="image/*" onChange={(e) => setPhotoFile(e.target.files?.[0] ?? null)} />
                  </label>
                  {member.photoUpdatedAt && (
                    <button
                      type="button"
                      className="secondary small"
                      disabled={removingPhotoId === member.id}
                      onClick={() => handleRemovePhoto(member.id)}
                    >
                      {removingPhotoId === member.id ? "Removing..." : "Remove photo"}
                    </button>
                  )}
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
                    <Avatar
                      userId={member.id}
                      photoUpdatedAt={member.photoUpdatedAt}
                      firstName={member.firstName}
                      lastName={member.lastName}
                    />
                    <div>
                      <div>
                        <strong>
                          {member.firstName} {member.lastName}
                        </strong>
                      </div>
                      <div className="table-hint">{member.title || ROLE_LABELS[member.role]}</div>
                    </div>
                  </div>
                  <p className="table-hint">{member.email}</p>
                  {member.phone && <p className="table-hint">{member.phone}</p>}
                  {member.committees.length > 0 && (
                    <p className="table-hint">
                      {member.committees
                        .map((c) => (c.isChair ? `${c.committeeName} (Chair)` : c.committeeName))
                        .join(", ")}
                    </p>
                  )}
                  {member.bio && <p>{member.bio}</p>}
                  {canEdit && (
                    <button className="secondary small" onClick={() => startEditing(member)}>
                      Edit
                    </button>
                  )}
                </div>
              );
            })}
          </div>
        )}

        {isAdmin && revealedOnboard && (
          <section className="dashboard-section key-reveal">
            <h2>{revealedOnboard.orgName} is onboarded</h2>
            <p className="form-error">
              Copy this password now &mdash; it won&apos;t be shown again. Share it with {revealedOnboard.email} directly.
            </p>
            <div className="key-reveal-value">
              <code>{revealedOnboard.password}</code>
              <button type="button" className="secondary small" onClick={handleCopyOnboard}>
                {onboardCopied ? "Copied!" : "Copy"}
              </button>
            </div>
            <button type="button" className="secondary small" onClick={() => setRevealedOnboard(null)}>
              Done
            </button>
          </section>
        )}

        {isAdmin && (
          <section className="dashboard-section">
            <h2>Onboard a client</h2>
            <form className="add-user-form" onSubmit={handleOnboardClient}>
              <label>
                Organization name
                <input value={clientOrgName} onChange={(e) => setClientOrgName(e.target.value)} required />
              </label>
              <div className="field-row">
                <label>
                  Admin first name
                  <input value={clientFirstName} onChange={(e) => setClientFirstName(e.target.value)} required />
                </label>
                <label>
                  Admin last name
                  <input value={clientLastName} onChange={(e) => setClientLastName(e.target.value)} required />
                </label>
              </div>
              <label>
                Admin email
                <input type="email" value={clientEmail} onChange={(e) => setClientEmail(e.target.value)} required />
              </label>
              {onboardError && <p className="form-error">{onboardError}</p>}
              <button type="submit" disabled={onboarding}>
                {onboarding ? "Onboarding..." : "Onboard client"}
              </button>
            </form>
          </section>
        )}
      </main>
    </div>
  );
}
