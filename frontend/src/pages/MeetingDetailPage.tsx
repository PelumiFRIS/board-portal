import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
  addAgendaItem,
  deleteAgendaItem,
  getMeeting,
  updateAgendaItem,
  updateMeeting,
} from "../api/meetings";
import { downloadDocument } from "../api/documents";
import {
  castVote,
  closeResolution,
  createResolution,
  getResolution,
  openResolution,
} from "../api/resolutions";
import { extractErrorMessage } from "../api/client";
import type {
  AgendaItem,
  MeetingDetail as MeetingDetailType,
  ResolutionSummary,
  VoteChoice,
  VoteRecord,
} from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";

export function MeetingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [meeting, setMeeting] = useState<MeetingDetailType | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [newItemTitle, setNewItemTitle] = useState("");
  const [newItemDescription, setNewItemDescription] = useState("");
  const [addingItem, setAddingItem] = useState(false);

  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");

  const [minutesDraft, setMinutesDraft] = useState("");
  const [savingMinutes, setSavingMinutes] = useState(false);

  const [newResolutionTitle, setNewResolutionTitle] = useState("");
  const [newResolutionDescription, setNewResolutionDescription] = useState("");
  const [creatingResolution, setCreatingResolution] = useState(false);
  const [busyResolutionId, setBusyResolutionId] = useState<string | null>(null);
  const [expandedResolutionId, setExpandedResolutionId] = useState<string | null>(null);
  const [resolutionVotes, setResolutionVotes] = useState<Record<string, VoteRecord[]>>({});

  useEffect(() => {
    if (!id) return;
    setLoading(true);
    getMeeting(id)
      .then((data) => {
        setMeeting(data);
        setMinutesDraft(data.minutesContent ?? "");
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [id]);

  async function handleAddAgendaItem(event: FormEvent) {
    event.preventDefault();
    if (!id) return;
    setActionError(null);
    setAddingItem(true);
    try {
      const item = await addAgendaItem(id, { title: newItemTitle, description: newItemDescription || undefined });
      setMeeting((prev) => (prev ? { ...prev, agendaItems: [...prev.agendaItems, item] } : prev));
      setNewItemTitle("");
      setNewItemDescription("");
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setAddingItem(false);
    }
  }

  function startEditing(item: AgendaItem) {
    setEditingItemId(item.id);
    setEditTitle(item.title);
    setEditDescription(item.description ?? "");
  }

  async function handleSaveEdit(itemId: string) {
    if (!id) return;
    setActionError(null);
    try {
      const updated = await updateAgendaItem(id, itemId, { title: editTitle, description: editDescription || undefined });
      setMeeting((prev) =>
        prev
          ? { ...prev, agendaItems: prev.agendaItems.map((i) => (i.id === updated.id ? updated : i)) }
          : prev,
      );
      setEditingItemId(null);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  }

  async function handleDeleteAgendaItem(itemId: string) {
    if (!id) return;
    setActionError(null);
    try {
      await deleteAgendaItem(id, itemId);
      setMeeting((prev) => (prev ? { ...prev, agendaItems: prev.agendaItems.filter((i) => i.id !== itemId) } : prev));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  }

  async function handleSaveMinutes() {
    if (!id) return;
    setActionError(null);
    setSavingMinutes(true);
    try {
      const updated = await updateMeeting(id, { minutesContent: minutesDraft });
      setMeeting(updated);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSavingMinutes(false);
    }
  }

  async function handleDownload(documentId: string, fileName: string) {
    try {
      await downloadDocument(documentId, fileName);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  }

  function replaceResolution(updated: ResolutionSummary) {
    setMeeting((prev) =>
      prev
        ? { ...prev, resolutions: prev.resolutions.map((r) => (r.id === updated.id ? updated : r)) }
        : prev,
    );
  }

  async function handleCreateResolution(event: FormEvent) {
    event.preventDefault();
    if (!id) return;
    setActionError(null);
    setCreatingResolution(true);
    try {
      const resolution = await createResolution({
        meetingId: id,
        title: newResolutionTitle,
        description: newResolutionDescription || undefined,
      });
      setMeeting((prev) => (prev ? { ...prev, resolutions: [resolution, ...prev.resolutions] } : prev));
      setNewResolutionTitle("");
      setNewResolutionDescription("");
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setCreatingResolution(false);
    }
  }

  async function handleOpenResolution(resolutionId: string) {
    setActionError(null);
    setBusyResolutionId(resolutionId);
    try {
      replaceResolution(await openResolution(resolutionId));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyResolutionId(null);
    }
  }

  async function handleCloseResolution(resolutionId: string) {
    setActionError(null);
    setBusyResolutionId(resolutionId);
    try {
      replaceResolution(await closeResolution(resolutionId));
      setResolutionVotes((prev) => {
        const next = { ...prev };
        delete next[resolutionId];
        return next;
      });
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyResolutionId(null);
    }
  }

  async function handleCastVote(resolutionId: string, choice: VoteChoice) {
    setActionError(null);
    setBusyResolutionId(resolutionId);
    try {
      replaceResolution(await castVote(resolutionId, choice));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyResolutionId(null);
    }
  }

  async function handleToggleVotes(resolutionId: string) {
    if (expandedResolutionId === resolutionId) {
      setExpandedResolutionId(null);
      return;
    }
    setExpandedResolutionId(resolutionId);
    if (resolutionVotes[resolutionId]) return;
    try {
      const detail = await getResolution(resolutionId);
      setResolutionVotes((prev) => ({ ...prev, [resolutionId]: detail.votes }));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  }

  async function handleStatusChange(status: "COMPLETED" | "CANCELLED") {
    if (!id) return;
    setActionError(null);
    try {
      const updated = await updateMeeting(id, { status });
      setMeeting(updated);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <p>
          <Link to="/meetings">&larr; Back to meetings</Link>
        </p>

        {loading && <p>Loading meeting...</p>}
        {loadError && <p className="form-error">{loadError}</p>}

        {meeting && (
          <>
            <div className="page-header">
              <h1>{meeting.title}</h1>
              {meeting.description && <p>{meeting.description}</p>}
            </div>

            <section className="dashboard-section">
              <p>
                <strong>When:</strong> {new Date(meeting.scheduledStart).toLocaleString()}
                {meeting.scheduledEnd ? ` – ${new Date(meeting.scheduledEnd).toLocaleString()}` : ""}
              </p>
              <p>
                <strong>Location:</strong> {meeting.location ?? "—"}
              </p>
              <p>
                <strong>Status:</strong> <StatusBadge status={meeting.status} />
              </p>
              {actionError && <p className="form-error">{actionError}</p>}
              {isAdmin && meeting.status === "SCHEDULED" && (
                <div className="field-row">
                  <button onClick={() => handleStatusChange("COMPLETED")}>Mark completed</button>
                  <button className="secondary" onClick={() => handleStatusChange("CANCELLED")}>
                    Cancel meeting
                  </button>
                </div>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Agenda</h2>
              {meeting.agendaItems.length === 0 && (
                <div className="empty-state">
                  <p>No agenda items yet.</p>
                </div>
              )}
              {meeting.agendaItems.map((item) =>
                editingItemId === item.id ? (
                  <div key={item.id} className="add-user-form">
                    <label>
                      Title
                      <input value={editTitle} onChange={(e) => setEditTitle(e.target.value)} />
                    </label>
                    <label>
                      Description
                      <input value={editDescription} onChange={(e) => setEditDescription(e.target.value)} />
                    </label>
                    <div className="field-row">
                      <button onClick={() => handleSaveEdit(item.id)}>Save</button>
                      <button className="secondary" onClick={() => setEditingItemId(null)}>
                        Cancel
                      </button>
                    </div>
                  </div>
                ) : (
                  <div key={item.id} className="agenda-item-row">
                    <div className="agenda-item-body">
                      <strong>{item.title}</strong>
                      {item.description && <p>{item.description}</p>}
                    </div>
                    {isAdmin && (
                      <>
                        <button className="secondary small" onClick={() => startEditing(item)}>
                          Edit
                        </button>
                        <button className="secondary small" onClick={() => handleDeleteAgendaItem(item.id)}>
                          Remove
                        </button>
                      </>
                    )}
                  </div>
                ),
              )}

              {isAdmin && (
                <form className="add-user-form" onSubmit={handleAddAgendaItem}>
                  <label>
                    New agenda item
                    <input value={newItemTitle} onChange={(e) => setNewItemTitle(e.target.value)} required />
                  </label>
                  <label>
                    Description
                    <input value={newItemDescription} onChange={(e) => setNewItemDescription(e.target.value)} />
                  </label>
                  <button type="submit" disabled={addingItem}>
                    {addingItem ? "Adding..." : "Add agenda item"}
                  </button>
                </form>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Resolutions</h2>
              {meeting.resolutions.length === 0 && (
                <div className="empty-state">
                  <p>No resolutions on this meeting yet.</p>
                </div>
              )}
              {meeting.resolutions.map((resolution) => {
                const isBusy = busyResolutionId === resolution.id;
                const totalVotes = resolution.forCount + resolution.againstCount + resolution.abstainCount;
                return (
                  <div key={resolution.id} className="resolution-card">
                    <div className="resolution-card-header">
                      <strong>{resolution.title}</strong>
                      <div className="field-row">
                        <StatusBadge status={resolution.status} />
                        {resolution.outcome && <StatusBadge status={resolution.outcome} />}
                      </div>
                    </div>
                    {resolution.description && <p>{resolution.description}</p>}

                    {resolution.status === "DRAFT" && isAdmin && (
                      <button className="small" disabled={isBusy} onClick={() => handleOpenResolution(resolution.id)}>
                        {isBusy ? "Opening..." : "Open for voting"}
                      </button>
                    )}

                    {resolution.status === "OPEN" && (
                      <>
                        <div className="vote-tally">
                          <span>For: {resolution.forCount}</span>
                          <span>Against: {resolution.againstCount}</span>
                          <span>Abstain: {resolution.abstainCount}</span>
                        </div>
                        <div className="field-row">
                          <button
                            className={`small ${resolution.myVote === "FOR" ? "" : "secondary"}`}
                            disabled={isBusy}
                            onClick={() => handleCastVote(resolution.id, "FOR")}
                          >
                            Vote for
                          </button>
                          <button
                            className={`small ${resolution.myVote === "AGAINST" ? "" : "secondary"}`}
                            disabled={isBusy}
                            onClick={() => handleCastVote(resolution.id, "AGAINST")}
                          >
                            Vote against
                          </button>
                          <button
                            className={`small ${resolution.myVote === "ABSTAIN" ? "" : "secondary"}`}
                            disabled={isBusy}
                            onClick={() => handleCastVote(resolution.id, "ABSTAIN")}
                          >
                            Abstain
                          </button>
                          {isAdmin && (
                            <button className="secondary small" disabled={isBusy} onClick={() => handleCloseResolution(resolution.id)}>
                              {isBusy ? "Closing..." : "Close voting"}
                            </button>
                          )}
                        </div>
                      </>
                    )}

                    {resolution.status === "CLOSED" && (
                      <>
                        <div className="vote-tally">
                          <span>For: {resolution.forCount}</span>
                          <span>Against: {resolution.againstCount}</span>
                          <span>Abstain: {resolution.abstainCount}</span>
                        </div>
                        <button className="secondary small" onClick={() => handleToggleVotes(resolution.id)}>
                          {expandedResolutionId === resolution.id ? "Hide votes" : `Show votes (${totalVotes})`}
                        </button>
                        {expandedResolutionId === resolution.id && (
                          <ul className="vote-record-list">
                            {(resolutionVotes[resolution.id] ?? []).map((v) => (
                              <li key={v.voterId}>
                                {v.voterName} — {v.choice}
                              </li>
                            ))}
                          </ul>
                        )}
                      </>
                    )}
                  </div>
                );
              })}

              {isAdmin && (
                <form className="add-user-form" onSubmit={handleCreateResolution}>
                  <label>
                    New resolution
                    <input value={newResolutionTitle} onChange={(e) => setNewResolutionTitle(e.target.value)} required />
                  </label>
                  <label>
                    Description
                    <input value={newResolutionDescription} onChange={(e) => setNewResolutionDescription(e.target.value)} />
                  </label>
                  <button type="submit" disabled={creatingResolution}>
                    {creatingResolution ? "Creating..." : "Add resolution"}
                  </button>
                </form>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Documents</h2>
              {meeting.documents.length === 0 && (
                <div className="empty-state">
                  <p>No documents linked to this meeting.</p>
                </div>
              )}
              {meeting.documents.map((doc) => (
                <div key={doc.id} className="document-row">
                  <div>
                    <strong>{doc.title}</strong>{" "}
                    <span className="badge badge-category">{doc.category.replace("_", " ")}</span>
                  </div>
                  <button className="secondary small" onClick={() => handleDownload(doc.id, doc.fileName)}>
                    Download
                  </button>
                </div>
              ))}
              <p>
                <Link to="/documents">Manage documents &rarr;</Link>
              </p>
            </section>

            <section className="dashboard-section">
              <h2>Minutes</h2>
              {isAdmin ? (
                <>
                  <textarea
                    className="minutes-textarea"
                    value={minutesDraft}
                    onChange={(e) => setMinutesDraft(e.target.value)}
                    rows={6}
                    placeholder="Record minutes here..."
                  />
                  <button onClick={handleSaveMinutes} disabled={savingMinutes}>
                    {savingMinutes ? "Saving..." : "Save minutes"}
                  </button>
                </>
              ) : (
                <p>{meeting.minutesContent ?? "No minutes published yet."}</p>
              )}
            </section>
          </>
        )}
      </main>
    </div>
  );
}
