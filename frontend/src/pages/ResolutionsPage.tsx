import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { listMeetings } from "../api/meetings";
import {
  castVote,
  closeResolution,
  createResolution,
  deleteResolution,
  getResolution,
  listResolutions,
  openResolution,
} from "../api/resolutions";
import { extractErrorMessage } from "../api/client";
import type { MeetingSummary, ResolutionSummary, VoteChoice, VoteRecord } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";

function EmptyResolutionsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M9 12.75L11.25 15 15 9.75M21 12a9 9 0 11-18 0 9 9 0 0118 0z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function ResolutionsPage() {
  const { user } = useAuth();
  const canManage = user?.role === "ADMIN" || user?.role === "EXECUTIVE";

  const [resolutions, setResolutions] = useState<ResolutionSummary[]>([]);
  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [meetingId, setMeetingId] = useState("");
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [creating, setCreating] = useState(false);
  const [formError, setFormError] = useState<string | null>(null);

  const [busyId, setBusyId] = useState<string | null>(null);
  const [expandedId, setExpandedId] = useState<string | null>(null);
  const [votesById, setVotesById] = useState<Record<string, VoteRecord[]>>({});

  useEffect(() => {
    Promise.all([listResolutions(), listMeetings()])
      .then(([r, m]) => {
        setResolutions(r);
        setMeetings(m);
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  const meetingTitleById = new Map(meetings.map((m) => [m.id, m.title]));

  function replaceResolution(updated: ResolutionSummary) {
    setResolutions((prev) => prev.map((r) => (r.id === updated.id ? updated : r)));
  }

  async function handleCreateResolution(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    if (!meetingId) {
      setFormError("Pick a meeting for this resolution.");
      return;
    }
    setCreating(true);
    try {
      const resolution = await createResolution({ meetingId, title, description: description || undefined });
      setResolutions((prev) => [resolution, ...prev]);
      setMeetingId("");
      setTitle("");
      setDescription("");
    } catch (err) {
      setFormError(extractErrorMessage(err));
    } finally {
      setCreating(false);
    }
  }

  async function handleOpenResolution(id: string) {
    setActionError(null);
    setBusyId(id);
    try {
      replaceResolution(await openResolution(id));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleCloseResolution(id: string) {
    setActionError(null);
    setBusyId(id);
    try {
      replaceResolution(await closeResolution(id));
      setVotesById((prev) => {
        const next = { ...prev };
        delete next[id];
        return next;
      });
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleCastVote(id: string, choice: VoteChoice) {
    setActionError(null);
    setBusyId(id);
    try {
      replaceResolution(await castVote(id, choice));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleDeleteResolution(id: string) {
    setActionError(null);
    setBusyId(id);
    try {
      await deleteResolution(id);
      setResolutions((prev) => prev.filter((r) => r.id !== id));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleToggleVotes(id: string) {
    if (expandedId === id) {
      setExpandedId(null);
      return;
    }
    setExpandedId(id);
    if (votesById[id]) return;
    try {
      const detail = await getResolution(id);
      setVotesById((prev) => ({ ...prev, [id]: detail.votes }));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Resolutions</h1>
          <p>Every resolution across all meetings for {user.organizationName}</p>
        </div>

        <section className="dashboard-section">
          {loading && <p>Loading resolutions...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {actionError && <p className="form-error">{actionError}</p>}

          {!loading && !loadError && resolutions.length === 0 && (
            <div className="empty-state">
              <EmptyResolutionsIcon />
              <p>No resolutions have been proposed yet.</p>
            </div>
          )}

          {!loading &&
            !loadError &&
            resolutions.map((resolution) => {
              const isBusy = busyId === resolution.id;
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
                  <p className="table-hint">
                    <Link to={`/meetings/${resolution.meetingId}`}>
                      {meetingTitleById.get(resolution.meetingId) ?? "View meeting"} &rarr;
                    </Link>
                  </p>
                  {resolution.description && <p>{resolution.description}</p>}

                  {resolution.status === "DRAFT" && canManage && (
                    <div className="field-row">
                      <button className="small" disabled={isBusy} onClick={() => handleOpenResolution(resolution.id)}>
                        {isBusy ? "Opening..." : "Open for voting"}
                      </button>
                      <button
                        className="secondary small"
                        disabled={isBusy}
                        onClick={() => handleDeleteResolution(resolution.id)}
                      >
                        Delete draft
                      </button>
                    </div>
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
                          <button
                            className="secondary small"
                            disabled={isBusy}
                            onClick={() => handleCloseResolution(resolution.id)}
                          >
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
                        {expandedId === resolution.id ? "Hide votes" : `Show votes (${totalVotes})`}
                      </button>
                      {expandedId === resolution.id && (
                        <ul className="vote-record-list">
                          {(votesById[resolution.id] ?? []).map((v) => (
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
              <h3>Propose a resolution</h3>
              <form className="add-user-form" onSubmit={handleCreateResolution}>
                <label>
                  Meeting
                  <select value={meetingId} onChange={(e) => setMeetingId(e.target.value)} required>
                    <option value="" disabled>
                      — choose a meeting —
                    </option>
                    {meetings.map((m) => (
                      <option key={m.id} value={m.id}>
                        {m.title} ({new Date(m.scheduledStart).toLocaleDateString()})
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Title
                  <input value={title} onChange={(e) => setTitle(e.target.value)} required />
                </label>
                <label>
                  Description
                  <input value={description} onChange={(e) => setDescription(e.target.value)} />
                </label>
                {formError && <p className="form-error">{formError}</p>}
                <button type="submit" disabled={creating}>
                  {creating ? "Creating..." : "Add resolution"}
                </button>
              </form>
            </>
          )}
        </section>
      </main>
    </div>
  );
}
