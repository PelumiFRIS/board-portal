import { useEffect, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
  addAgendaItem,
  deleteAgendaItem,
  getMeeting,
  updateAgendaItem,
  updateMeeting,
} from "../api/meetings";
import { extractErrorMessage } from "../api/client";
import type { AgendaItem, MeetingDetail as MeetingDetailType } from "../api/types";
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
