import { useEffect, useState, type FormEvent } from "react";
import { createResource, deleteResource, downloadResource, listResources, updateResource } from "../api/resources";
import { extractErrorMessage } from "../api/client";
import type { ResourceCategory, ResourceSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

const CATEGORY_ORDER: ResourceCategory[] = [
  "GOVERNANCE_BEST_PRACTICES",
  "POLICIES_AND_PROCEDURES",
  "FAQ",
  "OTHER",
];

const CATEGORY_LABELS: Record<ResourceCategory, string> = {
  GOVERNANCE_BEST_PRACTICES: "Governance Best Practices",
  POLICIES_AND_PROCEDURES: "Policies & Procedures",
  FAQ: "FAQ",
  OTHER: "Other",
};

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function EmptyResourcesIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M12 6.042A8.967 8.967 0 006 3.75c-1.052 0-2.062.18-3 .512v14.25A8.987 8.987 0 016 18c2.305 0 4.408.867 6 2.292m0-14.25a8.966 8.966 0 016-2.292c1.052 0 2.062.18 3 .512v14.25A8.987 8.987 0 0018 18a8.967 8.967 0 00-6 2.292m0-14.25v14.25"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function ResourcesPage() {
  const { user } = useAuth();
  const canManage = user?.role === "ADMIN" || user?.role === "EXECUTIVE";

  const [resources, setResources] = useState<ResourceSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [newCategory, setNewCategory] = useState<ResourceCategory>("GOVERNANCE_BEST_PRACTICES");
  const [newTitle, setNewTitle] = useState("");
  const [newBody, setNewBody] = useState("");
  const [newFile, setNewFile] = useState<File | null>(null);
  const [creating, setCreating] = useState(false);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editCategory, setEditCategory] = useState<ResourceCategory>("GOVERNANCE_BEST_PRACTICES");
  const [editTitle, setEditTitle] = useState("");
  const [editBody, setEditBody] = useState("");
  const [savingEdit, setSavingEdit] = useState(false);

  const [busyId, setBusyId] = useState<string | null>(null);
  const [downloadingId, setDownloadingId] = useState<string | null>(null);

  useEffect(() => {
    listResources()
      .then(setResources)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setActionError(null);
    setCreating(true);
    try {
      const created = await createResource({
        category: newCategory,
        title: newTitle,
        body: newBody,
        file: newFile ?? undefined,
      });
      setResources((prev) => [...prev, created]);
      setNewCategory("GOVERNANCE_BEST_PRACTICES");
      setNewTitle("");
      setNewBody("");
      setNewFile(null);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setCreating(false);
    }
  }

  async function handleDownload(resource: ResourceSummary) {
    if (!resource.fileName) return;
    setActionError(null);
    setDownloadingId(resource.id);
    try {
      await downloadResource(resource.id, resource.fileName);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setDownloadingId(null);
    }
  }

  function startEditing(resource: ResourceSummary) {
    setActionError(null);
    setEditingId(resource.id);
    setEditCategory(resource.category);
    setEditTitle(resource.title);
    setEditBody(resource.body);
  }

  async function handleSaveEdit(event: FormEvent, resourceId: string) {
    event.preventDefault();
    setActionError(null);
    setSavingEdit(true);
    try {
      const updated = await updateResource(resourceId, {
        category: editCategory,
        title: editTitle,
        body: editBody,
      });
      setResources((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
      setEditingId(null);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSavingEdit(false);
    }
  }

  async function handleDelete(resourceId: string) {
    setActionError(null);
    setBusyId(resourceId);
    try {
      await deleteResource(resourceId);
      setResources((prev) => prev.filter((r) => r.id !== resourceId));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  if (!user) return null;

  const buckets = CATEGORY_ORDER.map((category) => ({
    category,
    items: resources.filter((r) => r.category === category).sort((a, b) => a.title.localeCompare(b.title)),
  })).filter((bucket) => bucket.items.length > 0);

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Resources</h1>
          <p>Onboarding materials and governance guidance for {user.organizationName}</p>
        </div>

        {loading && <p>Loading resources...</p>}
        {loadError && <p className="form-error">{loadError}</p>}
        {actionError && <p className="form-error">{actionError}</p>}

        {!loading && !loadError && buckets.length === 0 && (
          <section className="dashboard-section">
            <div className="empty-state">
              <EmptyResourcesIcon />
              <p>No resources have been added yet.</p>
            </div>
          </section>
        )}

        {!loading &&
          !loadError &&
          buckets.map(({ category, items }) => (
            <section key={category} className="dashboard-section">
              <h2>{CATEGORY_LABELS[category]}</h2>
              {items.map((resource) =>
                editingId === resource.id ? (
                  <form
                    key={resource.id}
                    className="add-user-form"
                    onSubmit={(e) => handleSaveEdit(e, resource.id)}
                  >
                    <label>
                      Category
                      <select
                        value={editCategory}
                        onChange={(e) => setEditCategory(e.target.value as ResourceCategory)}
                      >
                        {CATEGORY_ORDER.map((c) => (
                          <option key={c} value={c}>
                            {CATEGORY_LABELS[c]}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Title
                      <input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} required />
                    </label>
                    <label>
                      Body
                      <textarea
                        className="minutes-textarea"
                        rows={4}
                        value={editBody}
                        onChange={(e) => setEditBody(e.target.value)}
                        required
                      />
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
                  <div key={resource.id} className="resolution-card">
                    <strong>{resource.title}</strong>
                    <p className="resource-body">{resource.body}</p>
                    {resource.fileName && (
                      <p className="table-hint">
                        {resource.fileName}
                        {resource.fileSize != null ? ` · ${formatFileSize(resource.fileSize)}` : ""}
                      </p>
                    )}
                    <div className="field-row">
                      {resource.fileName && (
                        <button
                          className="secondary small"
                          disabled={downloadingId === resource.id}
                          onClick={() => handleDownload(resource)}
                        >
                          {downloadingId === resource.id ? "Downloading..." : "Download"}
                        </button>
                      )}
                      {canManage && (
                        <>
                          <button className="secondary small" onClick={() => startEditing(resource)}>
                            Edit
                          </button>
                          <button
                            className="secondary small"
                            disabled={busyId === resource.id}
                            onClick={() => handleDelete(resource.id)}
                          >
                            {busyId === resource.id ? "Deleting..." : "Delete"}
                          </button>
                        </>
                      )}
                    </div>
                  </div>
                ),
              )}
            </section>
          ))}

        {canManage && (
          <section className="dashboard-section">
            <h2>Add a resource</h2>
            <form className="add-user-form" onSubmit={handleCreate}>
              <label>
                Category
                <select value={newCategory} onChange={(e) => setNewCategory(e.target.value as ResourceCategory)}>
                  {CATEGORY_ORDER.map((c) => (
                    <option key={c} value={c}>
                      {CATEGORY_LABELS[c]}
                    </option>
                  ))}
                </select>
              </label>
              <label>
                Title
                <input value={newTitle} onChange={(e) => setNewTitle(e.target.value)} required />
              </label>
              <label>
                Body
                <textarea
                  className="minutes-textarea"
                  rows={4}
                  value={newBody}
                  onChange={(e) => setNewBody(e.target.value)}
                  required
                />
              </label>
              <label>
                Attach a file (optional)
                <input
                  type="file"
                  onChange={(e) => setNewFile(e.target.files?.[0] ?? null)}
                />
              </label>
              <button type="submit" disabled={creating}>
                {creating ? "Adding..." : "Add resource"}
              </button>
            </form>
          </section>
        )}
      </main>
    </div>
  );
}
