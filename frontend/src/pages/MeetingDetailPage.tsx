import { useEffect, useRef, useState, type FormEvent } from "react";
import { Link, useParams } from "react-router-dom";
import {
  addAgendaItem,
  deleteAgendaItem,
  downloadMeetingIcs,
  exportMeetingRecord,
  getMattersArising,
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
import { createActionItem, deleteActionItem, updateActionItemStatus } from "../api/actionItems";
import {
  deleteMeetingRecording,
  downloadMeetingRecording,
  listMeetingRecordings,
  uploadMeetingRecording,
} from "../api/meetingRecordings";
import { listDirectory } from "../api/auth";
import { extractErrorMessage } from "../api/client";
import { STANDARD_AGENDA_ITEMS } from "../constants/agendaTemplates";
import type {
  ActionItemSummary,
  AgendaItem,
  MatterArisingItem,
  MeetingDetail as MeetingDetailType,
  MeetingRecordingSummary,
  ResolutionSummary,
  UserSummary,
  VoteChoice,
  VoteRecord,
} from "../api/types";
import { CommentThread } from "../components/CommentThread";
import { RecordingPlayer } from "../components/RecordingPlayer";
import { Sidebar } from "../components/Sidebar";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";

const MAX_RESOLUTION_BATCH_SIZE = 12;

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function formatDuration(totalSeconds: number): string {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return `${minutes}:${seconds.toString().padStart(2, "0")}`;
}

export function MeetingDetailPage() {
  const { id } = useParams<{ id: string }>();
  const { user } = useAuth();
  const canManage = user?.role === "ADMIN" || user?.role === "EXECUTIVE";

  const [meeting, setMeeting] = useState<MeetingDetailType | null>(null);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [newItemTitle, setNewItemTitle] = useState("");
  const [newItemDescription, setNewItemDescription] = useState("");
  const [addingItem, setAddingItem] = useState(false);
  const [selectedTemplateItems, setSelectedTemplateItems] = useState<string[]>([]);
  const [addingTemplateItems, setAddingTemplateItems] = useState(false);

  const [mattersArising, setMattersArising] = useState<MatterArisingItem[]>([]);
  const [loadingMattersArising, setLoadingMattersArising] = useState(true);
  const [mattersArisingError, setMattersArisingError] = useState<string | null>(null);
  const [addingToAgendaId, setAddingToAgendaId] = useState<string | null>(null);

  const [editingItemId, setEditingItemId] = useState<string | null>(null);
  const [editTitle, setEditTitle] = useState("");
  const [editDescription, setEditDescription] = useState("");

  const [minutesDraft, setMinutesDraft] = useState("");
  const [savingMinutes, setSavingMinutes] = useState(false);

  const [recordings, setRecordings] = useState<MeetingRecordingSummary[]>([]);
  const [loadingRecordings, setLoadingRecordings] = useState(true);
  const [recordingError, setRecordingError] = useState<string | null>(null);
  const [isRecording, setIsRecording] = useState(false);
  const [recordingSeconds, setRecordingSeconds] = useState(0);
  const [uploadingRecording, setUploadingRecording] = useState(false);
  const [deletingRecordingId, setDeletingRecordingId] = useState<string | null>(null);
  const [expandedRecordingId, setExpandedRecordingId] = useState<string | null>(null);
  const mediaRecorderRef = useRef<MediaRecorder | null>(null);
  const mediaStreamRef = useRef<MediaStream | null>(null);
  const recordedChunksRef = useRef<Blob[]>([]);
  const recordingTimerRef = useRef<number | null>(null);

  const [newResolutionTitle, setNewResolutionTitle] = useState("");
  const [queuedResolutionTitles, setQueuedResolutionTitles] = useState<string[]>([]);
  const [submittingResolutions, setSubmittingResolutions] = useState(false);
  const [busyResolutionId, setBusyResolutionId] = useState<string | null>(null);
  const [expandedResolutionId, setExpandedResolutionId] = useState<string | null>(null);
  const [resolutionVotes, setResolutionVotes] = useState<Record<string, VoteRecord[]>>({});

  const [orgMembers, setOrgMembers] = useState<UserSummary[]>([]);
  const [newActionItemTitle, setNewActionItemTitle] = useState("");
  const [newActionItemDescription, setNewActionItemDescription] = useState("");
  const [newActionItemAssigneeId, setNewActionItemAssigneeId] = useState("");
  const [newActionItemDueDate, setNewActionItemDueDate] = useState("");
  const [creatingActionItem, setCreatingActionItem] = useState(false);
  const [busyActionItemId, setBusyActionItemId] = useState<string | null>(null);

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

  useEffect(() => {
    if (!canManage) return;
    listDirectory()
      .then(setOrgMembers)
      .catch(() => undefined);
  }, [canManage]);

  useEffect(() => {
    if (!id) return;
    setLoadingMattersArising(true);
    getMattersArising(id)
      .then(setMattersArising)
      .catch((err) => setMattersArisingError(extractErrorMessage(err)))
      .finally(() => setLoadingMattersArising(false));
  }, [id]);

  useEffect(() => {
    if (!id) return;
    setLoadingRecordings(true);
    listMeetingRecordings(id)
      .then(setRecordings)
      .catch((err) => setRecordingError(extractErrorMessage(err)))
      .finally(() => setLoadingRecordings(false));
  }, [id]);

  useEffect(() => {
    return () => {
      if (recordingTimerRef.current) window.clearInterval(recordingTimerRef.current);
      mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
    };
  }, []);

  async function handleStartRecording() {
    setRecordingError(null);
    try {
      const stream = await navigator.mediaDevices.getUserMedia({ audio: true });
      const recorder = new MediaRecorder(stream);
      recordedChunksRef.current = [];
      recorder.ondataavailable = (event) => {
        if (event.data.size > 0) recordedChunksRef.current.push(event.data);
      };
      mediaRecorderRef.current = recorder;
      mediaStreamRef.current = stream;
      recorder.start();
      setIsRecording(true);
      setRecordingSeconds(0);
      recordingTimerRef.current = window.setInterval(() => {
        setRecordingSeconds((prev) => prev + 1);
      }, 1000);
    } catch {
      setRecordingError("Microphone access was denied or is unavailable.");
    }
  }

  async function handleStopRecording() {
    const recorder = mediaRecorderRef.current;
    if (!recorder || !id) return;
    if (recordingTimerRef.current) {
      window.clearInterval(recordingTimerRef.current);
      recordingTimerRef.current = null;
    }
    setIsRecording(false);

    const stopped = new Promise<void>((resolve) => {
      recorder.addEventListener("stop", () => resolve(), { once: true });
    });
    recorder.stop();
    await stopped;
    mediaStreamRef.current?.getTracks().forEach((track) => track.stop());
    mediaStreamRef.current = null;

    const blob = new Blob(recordedChunksRef.current, { type: recorder.mimeType || "audio/webm" });
    recordedChunksRef.current = [];
    setUploadingRecording(true);
    try {
      const fileName = `recording-${new Date().toISOString().replace(/[:.]/g, "-")}.webm`;
      const created = await uploadMeetingRecording(id, blob, fileName);
      setRecordings((prev) => [created, ...prev]);
    } catch (err) {
      setRecordingError(extractErrorMessage(err));
    } finally {
      setUploadingRecording(false);
    }
  }

  async function handleDownloadRecording(recordingId: string, fileName: string) {
    if (!id) return;
    setRecordingError(null);
    try {
      await downloadMeetingRecording(id, recordingId, fileName);
    } catch (err) {
      setRecordingError(extractErrorMessage(err));
    }
  }

  async function handleDeleteRecording(recordingId: string) {
    if (!id) return;
    setRecordingError(null);
    setDeletingRecordingId(recordingId);
    try {
      await deleteMeetingRecording(id, recordingId);
      setRecordings((prev) => prev.filter((r) => r.id !== recordingId));
      if (expandedRecordingId === recordingId) setExpandedRecordingId(null);
    } catch (err) {
      setRecordingError(extractErrorMessage(err));
    } finally {
      setDeletingRecordingId(null);
    }
  }

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

  function handleToggleTemplateItem(itemLabel: string) {
    setSelectedTemplateItems((prev) =>
      prev.includes(itemLabel) ? prev.filter((label) => label !== itemLabel) : [...prev, itemLabel],
    );
  }

  async function handleAddTemplateItems() {
    if (!id || selectedTemplateItems.length === 0) return;
    setActionError(null);
    setAddingTemplateItems(true);
    const remaining: string[] = [];
    for (const label of selectedTemplateItems) {
      try {
        const item = await addAgendaItem(id, { title: label });
        setMeeting((prev) => (prev ? { ...prev, agendaItems: [...prev.agendaItems, item] } : prev));
      } catch (err) {
        remaining.push(label);
        setActionError(extractErrorMessage(err));
      }
    }
    setSelectedTemplateItems(remaining);
    setAddingTemplateItems(false);
  }

  async function handleAddMatterToAgenda(matter: MatterArisingItem) {
    if (!id) return;
    setActionError(null);
    setAddingToAgendaId(matter.id);
    try {
      const item = await addAgendaItem(id, {
        title: matter.title,
        description: `Carried forward from "${matter.sourceMeetingTitle}"`,
      });
      setMeeting((prev) => (prev ? { ...prev, agendaItems: [...prev.agendaItems, item] } : prev));
      setMattersArising((prev) => prev.filter((m) => m.id !== matter.id));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setAddingToAgendaId(null);
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

  function handleQueueResolutionTitle(event: FormEvent) {
    event.preventDefault();
    setActionError(null);
    const trimmed = newResolutionTitle.trim();
    if (!trimmed) return;
    if (queuedResolutionTitles.length >= MAX_RESOLUTION_BATCH_SIZE) {
      setActionError(`You can queue up to ${MAX_RESOLUTION_BATCH_SIZE} resolutions at a time.`);
      return;
    }
    setQueuedResolutionTitles((prev) => [...prev, trimmed]);
    setNewResolutionTitle("");
  }

  function handleRemoveQueuedResolutionTitle(index: number) {
    setQueuedResolutionTitles((prev) => prev.filter((_, i) => i !== index));
  }

  async function handleSubmitResolutionBatch() {
    if (!id || queuedResolutionTitles.length === 0) return;
    setActionError(null);
    setSubmittingResolutions(true);
    const remaining: string[] = [];
    const errors: string[] = [];
    for (const queuedTitle of queuedResolutionTitles) {
      try {
        const resolution = await createResolution({ meetingId: id, title: queuedTitle });
        setMeeting((prev) => (prev ? { ...prev, resolutions: [resolution, ...prev.resolutions] } : prev));
      } catch (err) {
        remaining.push(queuedTitle);
        errors.push(`"${queuedTitle}": ${extractErrorMessage(err)}`);
      }
    }
    setQueuedResolutionTitles(remaining);
    setSubmittingResolutions(false);
    if (errors.length > 0) {
      setActionError(errors.join(" · "));
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

  async function handleCreateActionItem(event: FormEvent) {
    event.preventDefault();
    if (!id) return;
    setActionError(null);
    setCreatingActionItem(true);
    try {
      const item = await createActionItem({
        meetingId: id,
        title: newActionItemTitle,
        description: newActionItemDescription || undefined,
        assigneeId: newActionItemAssigneeId,
        dueDate: newActionItemDueDate || undefined,
      });
      setMeeting((prev) => (prev ? { ...prev, actionItems: [item, ...prev.actionItems] } : prev));
      setNewActionItemTitle("");
      setNewActionItemDescription("");
      setNewActionItemAssigneeId("");
      setNewActionItemDueDate("");
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setCreatingActionItem(false);
    }
  }

  async function handleToggleActionItemStatus(item: ActionItemSummary) {
    setActionError(null);
    setBusyActionItemId(item.id);
    try {
      const updated = await updateActionItemStatus(item.id, item.status === "OPEN" ? "DONE" : "OPEN");
      setMeeting((prev) =>
        prev ? { ...prev, actionItems: prev.actionItems.map((i) => (i.id === updated.id ? updated : i)) } : prev,
      );
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyActionItemId(null);
    }
  }

  async function handleDeleteActionItem(itemId: string) {
    setActionError(null);
    try {
      await deleteActionItem(itemId);
      setMeeting((prev) => (prev ? { ...prev, actionItems: prev.actionItems.filter((i) => i.id !== itemId) } : prev));
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

  async function handleDownloadIcs() {
    if (!id || !meeting) return;
    setActionError(null);
    try {
      await downloadMeetingIcs(id, meeting.title);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  }

  async function handleExportRecord() {
    if (!id) return;
    setActionError(null);
    try {
      await exportMeetingRecord(id);
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
                <strong>Status:</strong> <StatusBadge status={meeting.status} />{" "}
                <span className="badge badge-category">{meeting.meetingTypeName}</span>
              </p>
              {actionError && <p className="form-error">{actionError}</p>}
              <div className="field-row">
                <button className="secondary small" onClick={handleDownloadIcs}>
                  Add to calendar
                </button>
                <button className="secondary small" onClick={handleExportRecord}>
                  Download meeting record
                </button>
              </div>
              {canManage && meeting.status === "SCHEDULED" && (
                <div className="field-row">
                  <button onClick={() => handleStatusChange("COMPLETED")}>Mark completed</button>
                  <button className="secondary" onClick={() => handleStatusChange("CANCELLED")}>
                    Cancel meeting
                  </button>
                </div>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Agenda Setting</h2>
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
                    {canManage && (
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

              {canManage && (
                <>
                  <div className="add-user-form">
                    <label>Quick add from template</label>
                    <div className="recipient-picker">
                      {STANDARD_AGENDA_ITEMS.map((label) => (
                        <label key={label} className="recipient-picker-item">
                          <input
                            type="checkbox"
                            checked={selectedTemplateItems.includes(label)}
                            onChange={() => handleToggleTemplateItem(label)}
                          />
                          <span>{label}</span>
                        </label>
                      ))}
                    </div>
                    {selectedTemplateItems.length > 0 && (
                      <button
                        type="button"
                        className="secondary small"
                        disabled={addingTemplateItems}
                        onClick={handleAddTemplateItems}
                      >
                        {addingTemplateItems
                          ? "Adding..."
                          : `Add ${selectedTemplateItems.length} selected item${selectedTemplateItems.length === 1 ? "" : "s"}`}
                      </button>
                    )}
                  </div>

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
                </>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Matters Arising</h2>
              <p className="table-hint">
                Still-open action items from other meetings, so nothing gets lost between meetings.
              </p>
              {mattersArisingError && <p className="form-error">{mattersArisingError}</p>}
              {loadingMattersArising && <p>Loading matters arising...</p>}
              {!loadingMattersArising && mattersArising.length === 0 && (
                <div className="empty-state">
                  <p>Nothing outstanding from previous meetings.</p>
                </div>
              )}
              {!loadingMattersArising &&
                mattersArising.map((matter) => (
                  <div key={matter.id} className="resolution-card">
                    <div className="resolution-card-header">
                      <strong>{matter.title}</strong>
                    </div>
                    {matter.description && <p>{matter.description}</p>}
                    <p className="table-hint">
                      From "{matter.sourceMeetingTitle}" ({new Date(matter.sourceMeetingScheduledStart).toLocaleDateString()})
                      {" · "}Assigned to {matter.assigneeName}
                      {matter.dueDate ? ` · Due ${new Date(matter.dueDate).toLocaleDateString()}` : ""}
                    </p>
                    {canManage && (
                      <button
                        className="secondary small"
                        disabled={addingToAgendaId === matter.id}
                        onClick={() => handleAddMatterToAgenda(matter)}
                      >
                        {addingToAgendaId === matter.id ? "Adding..." : "Add to agenda"}
                      </button>
                    )}
                  </div>
                ))}
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

                    {resolution.status === "DRAFT" && canManage && (
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
                          {canManage && (
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

              {canManage && (
                <>
                  <form className="add-user-form" onSubmit={handleQueueResolutionTitle}>
                    <label>
                      Resolution title
                      <input
                        value={newResolutionTitle}
                        onChange={(e) => setNewResolutionTitle(e.target.value)}
                        required
                      />
                    </label>
                    <button type="submit" disabled={queuedResolutionTitles.length >= MAX_RESOLUTION_BATCH_SIZE}>
                      Add to batch
                    </button>
                  </form>

                  {queuedResolutionTitles.length > 0 && (
                    <>
                      <p className="table-hint">
                        {queuedResolutionTitles.length}/{MAX_RESOLUTION_BATCH_SIZE} queued
                      </p>
                      {queuedResolutionTitles.map((queuedTitle, index) => (
                        <div key={`${queuedTitle}-${index}`} className="agenda-item-row">
                          <div className="agenda-item-body">
                            <strong>{queuedTitle}</strong>
                          </div>
                          <button
                            className="secondary small"
                            onClick={() => handleRemoveQueuedResolutionTitle(index)}
                          >
                            Remove
                          </button>
                        </div>
                      ))}
                      <button disabled={submittingResolutions} onClick={handleSubmitResolutionBatch}>
                        {submittingResolutions
                          ? "Proposing..."
                          : `Propose ${queuedResolutionTitles.length} resolution${queuedResolutionTitles.length === 1 ? "" : "s"}`}
                      </button>
                    </>
                  )}
                </>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Action Items</h2>
              {meeting.actionItems.length === 0 && (
                <div className="empty-state">
                  <p>No action items on this meeting yet.</p>
                </div>
              )}
              {meeting.actionItems.map((item) => {
                const isBusy = busyActionItemId === item.id;
                const canToggle = canManage || item.assigneeId === user.id;
                return (
                  <div key={item.id} className="resolution-card">
                    <div className="resolution-card-header">
                      <strong>{item.title}</strong>
                      <StatusBadge status={item.status} />
                    </div>
                    {item.description && <p>{item.description}</p>}
                    <p className="table-hint">
                      Assigned to {item.assigneeName}
                      {item.dueDate ? ` · Due ${new Date(item.dueDate).toLocaleDateString()}` : ""}
                    </p>
                    <div className="field-row">
                      {canToggle && (
                        <button
                          className="secondary small"
                          disabled={isBusy}
                          onClick={() => handleToggleActionItemStatus(item)}
                        >
                          {isBusy ? "Saving..." : item.status === "OPEN" ? "Mark done" : "Reopen"}
                        </button>
                      )}
                      {canManage && (
                        <button className="secondary small" onClick={() => handleDeleteActionItem(item.id)}>
                          Delete
                        </button>
                      )}
                    </div>
                  </div>
                );
              })}

              {canManage && (
                <form className="add-user-form" onSubmit={handleCreateActionItem}>
                  <label>
                    New action item
                    <input value={newActionItemTitle} onChange={(e) => setNewActionItemTitle(e.target.value)} required />
                  </label>
                  <label>
                    Description
                    <input
                      value={newActionItemDescription}
                      onChange={(e) => setNewActionItemDescription(e.target.value)}
                    />
                  </label>
                  <div className="field-row">
                    <label>
                      Assignee
                      <select
                        value={newActionItemAssigneeId}
                        onChange={(e) => setNewActionItemAssigneeId(e.target.value)}
                        required
                      >
                        <option value="" disabled>
                          Select a member
                        </option>
                        {orgMembers.map((m) => (
                          <option key={m.id} value={m.id}>
                            {m.firstName} {m.lastName}
                          </option>
                        ))}
                      </select>
                    </label>
                    <label>
                      Due date (optional)
                      <input
                        type="date"
                        value={newActionItemDueDate}
                        onChange={(e) => setNewActionItemDueDate(e.target.value)}
                      />
                    </label>
                  </div>
                  <button type="submit" disabled={creatingActionItem}>
                    {creatingActionItem ? "Adding..." : "Add action item"}
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
              {canManage ? (
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

            <section className="dashboard-section">
              <h2>Recording</h2>
              <p className="table-hint">
                Record and keep an audio file of the meeting alongside your typed minutes.
              </p>
              {recordingError && <p className="form-error">{recordingError}</p>}
              {loadingRecordings && <p>Loading recordings...</p>}
              {!loadingRecordings && recordings.length === 0 && (
                <div className="empty-state">
                  <p>No recordings yet.</p>
                </div>
              )}
              {!loadingRecordings &&
                recordings.map((recording) => (
                  <div key={recording.id} className="resolution-card">
                    <div className="resolution-card-header">
                      <strong>{new Date(recording.createdAt).toLocaleString()}</strong>
                      <span className="table-hint">
                        {recording.recordedByName} &middot; {formatFileSize(recording.fileSize)}
                      </span>
                    </div>
                    {expandedRecordingId === recording.id ? (
                      <RecordingPlayer meetingId={id!} recordingId={recording.id} />
                    ) : (
                      <button className="secondary small" onClick={() => setExpandedRecordingId(recording.id)}>
                        Play
                      </button>
                    )}
                    <div className="field-row">
                      <button
                        className="secondary small"
                        onClick={() => handleDownloadRecording(recording.id, recording.fileName)}
                      >
                        Download
                      </button>
                      {canManage && (
                        <button
                          className="secondary small"
                          disabled={deletingRecordingId === recording.id}
                          onClick={() => handleDeleteRecording(recording.id)}
                        >
                          {deletingRecordingId === recording.id ? "Removing..." : "Delete"}
                        </button>
                      )}
                    </div>
                  </div>
                ))}

              {canManage && (
                <div className="field-row">
                  {!isRecording ? (
                    <button onClick={handleStartRecording} disabled={uploadingRecording}>
                      {uploadingRecording ? "Uploading..." : "Start recording"}
                    </button>
                  ) : (
                    <button className="secondary" onClick={handleStopRecording}>
                      <span className="recording-indicator">
                        <span className="recording-indicator-dot" />
                        Stop recording ({formatDuration(recordingSeconds)})
                      </span>
                    </button>
                  )}
                </div>
              )}
            </section>

            <section className="dashboard-section">
              <h2>Discussion</h2>
              <CommentThread entityType="MEETING" entityId={meeting.id} />
            </section>
          </>
        )}
      </main>
    </div>
  );
}
