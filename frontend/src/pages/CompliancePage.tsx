import { useEffect, useState, type FormEvent } from "react";
import {
  createComplianceFiling,
  deleteComplianceFiling,
  listComplianceFilings,
  markFilingSubmitted,
  updateComplianceFiling,
} from "../api/complianceFilings";
import { extractErrorMessage } from "../api/client";
import type { ComplianceFilingSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";

function EmptyFilingsIcon() {
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

function filingStatusLabel(filing: ComplianceFilingSummary): "PENDING" | "SUBMITTED" | "OVERDUE" {
  if (filing.status === "SUBMITTED") return "SUBMITTED";
  const today = new Date().toISOString().slice(0, 10);
  return filing.dueDate < today ? "OVERDUE" : "PENDING";
}

export function CompliancePage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [filings, setFilings] = useState<ComplianceFilingSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [newTitle, setNewTitle] = useState("");
  const [newDescription, setNewDescription] = useState("");
  const [newDueDate, setNewDueDate] = useState("");
  const [creating, setCreating] = useState(false);

  const [editingId, setEditingId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");
  const [editDueDate, setEditDueDate] = useState("");
  const [savingEdit, setSavingEdit] = useState(false);

  const [busyId, setBusyId] = useState<string | null>(null);

  useEffect(() => {
    listComplianceFilings()
      .then(setFilings)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setActionError(null);
    setCreating(true);
    try {
      const created = await createComplianceFiling({
        title: newTitle,
        description: newDescription || undefined,
        dueDate: newDueDate,
      });
      setFilings((prev) => [...prev, created].sort((a, b) => a.dueDate.localeCompare(b.dueDate)));
      setNewTitle("");
      setNewDescription("");
      setNewDueDate("");
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setCreating(false);
    }
  }

  function startEditing(filing: ComplianceFilingSummary) {
    setActionError(null);
    setEditingId(filing.id);
    setEditTitle(filing.title);
    setEditDescription(filing.description ?? "");
    setEditDueDate(filing.dueDate);
  }

  async function handleSaveEdit(event: FormEvent, filingId: string) {
    event.preventDefault();
    setActionError(null);
    setSavingEdit(true);
    try {
      const updated = await updateComplianceFiling(filingId, {
        title: editTitle,
        description: editDescription,
        dueDate: editDueDate,
      });
      setFilings((prev) =>
        prev.map((f) => (f.id === updated.id ? updated : f)).sort((a, b) => a.dueDate.localeCompare(b.dueDate)),
      );
      setEditingId(null);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSavingEdit(false);
    }
  }

  async function handleMarkSubmitted(filingId: string) {
    setActionError(null);
    setBusyId(filingId);
    try {
      const updated = await markFilingSubmitted(filingId);
      setFilings((prev) => prev.map((f) => (f.id === updated.id ? updated : f)));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleDelete(filingId: string) {
    setActionError(null);
    setBusyId(filingId);
    try {
      await deleteComplianceFiling(filingId);
      setFilings((prev) => prev.filter((f) => f.id !== filingId));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Compliance</h1>
          <p>Regulatory filing deadlines for {user.organizationName}</p>
        </div>

        <section className="dashboard-section">
          {loading && <p>Loading filings...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {actionError && <p className="form-error">{actionError}</p>}

          {!loading && !loadError && filings.length === 0 && (
            <div className="empty-state">
              <EmptyFilingsIcon />
              <p>No filings on the calendar yet.</p>
            </div>
          )}

          {!loading &&
            !loadError &&
            filings.map((filing) => {
              const busy = busyId === filing.id;
              return (
                <div key={filing.id} className="resolution-card">
                  {editingId === filing.id ? (
                    <form className="add-user-form" onSubmit={(e) => handleSaveEdit(e, filing.id)}>
                      <label>
                        Title
                        <input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} required />
                      </label>
                      <label>
                        Description
                        <input value={editDescription} onChange={(e) => setEditDescription(e.target.value)} />
                      </label>
                      <label>
                        Due date
                        <input
                          type="date"
                          value={editDueDate}
                          onChange={(e) => setEditDueDate(e.target.value)}
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
                    <>
                      <div className="resolution-card-header">
                        <div>
                          <strong>{filing.title}</strong>
                          {filing.description && <p className="table-hint">{filing.description}</p>}
                        </div>
                        <StatusBadge status={filingStatusLabel(filing)} />
                      </div>
                      <p className="table-hint">
                        Due {new Date(filing.dueDate).toLocaleDateString()}
                        {filing.status === "SUBMITTED" &&
                          filing.submittedAt &&
                          ` · Submitted ${new Date(filing.submittedAt).toLocaleDateString()}${
                            filing.submittedByName ? ` by ${filing.submittedByName}` : ""
                          }`}
                      </p>
                      {isAdmin && (
                        <div className="field-row">
                          {filing.status !== "SUBMITTED" && (
                            <button
                              type="button"
                              className="secondary small"
                              disabled={busy}
                              onClick={() => handleMarkSubmitted(filing.id)}
                            >
                              Mark submitted
                            </button>
                          )}
                          <button className="secondary small" onClick={() => startEditing(filing)}>
                            Edit
                          </button>
                          <button
                            className="secondary small"
                            disabled={busy}
                            onClick={() => handleDelete(filing.id)}
                          >
                            Delete
                          </button>
                        </div>
                      )}
                    </>
                  )}
                </div>
              );
            })}

          {isAdmin && (
            <>
              <h3>Add a filing</h3>
              <form className="add-user-form" onSubmit={handleCreate}>
                <label>
                  Title
                  <input value={newTitle} onChange={(e) => setNewTitle(e.target.value)} required />
                </label>
                <label>
                  Description
                  <input value={newDescription} onChange={(e) => setNewDescription(e.target.value)} />
                </label>
                <label>
                  Due date
                  <input type="date" value={newDueDate} onChange={(e) => setNewDueDate(e.target.value)} required />
                </label>
                <button type="submit" disabled={creating}>
                  {creating ? "Adding..." : "Add filing"}
                </button>
              </form>
            </>
          )}
        </section>
      </main>
    </div>
  );
}
