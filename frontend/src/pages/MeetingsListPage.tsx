import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import {
  buildCalendarFeedUrl,
  createMeeting,
  getCalendarToken,
  listMeetings,
  regenerateCalendarToken,
} from "../api/meetings";
import { listCommittees } from "../api/committees";
import { extractErrorMessage } from "../api/client";
import type { CommitteeSummary, MeetingSummary, MeetingType } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";

const MEETING_TYPE_LABELS: Record<MeetingType, string> = {
  AGM: "Annual General Meeting (AGM)",
  EGM: "Extra-Ordinary General Meeting (EGM)",
  COM: "Court-Ordered Meeting (COM)",
};

function EmptyMeetingsIcon() {
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

export function MeetingsListPage() {
  const { user } = useAuth();
  const canManage = user?.role === "ADMIN" || user?.role === "EXECUTIVE";

  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [committees, setCommittees] = useState<CommitteeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [committeeFilter, setCommitteeFilter] = useState("");

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [scheduledStart, setScheduledStart] = useState("");
  const [scheduledEnd, setScheduledEnd] = useState("");
  const [committeeId, setCommitteeId] = useState("");
  const [meetingType, setMeetingType] = useState<MeetingType | "">("");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  const flatCommittees = committees.flatMap((c) => [c, ...c.subCommittees]);

  const [feedUrl, setFeedUrl] = useState<string | null>(null);
  const [feedError, setFeedError] = useState<string | null>(null);
  const [feedLoading, setFeedLoading] = useState(false);
  const [copied, setCopied] = useState(false);

  useEffect(() => {
    listCommittees()
      .then(setCommittees)
      .catch((err) => setLoadError(extractErrorMessage(err)));
  }, []);

  useEffect(() => {
    setLoading(true);
    listMeetings({ committeeId: committeeFilter || undefined })
      .then(setMeetings)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [committeeFilter]);

  async function handleShowFeedLink() {
    setFeedError(null);
    setFeedLoading(true);
    try {
      const { token } = await getCalendarToken();
      setFeedUrl(buildCalendarFeedUrl(token));
    } catch (err) {
      setFeedError(extractErrorMessage(err));
    } finally {
      setFeedLoading(false);
    }
  }

  async function handleCopyFeedLink() {
    if (!feedUrl) return;
    await navigator.clipboard.writeText(feedUrl);
    setCopied(true);
    setTimeout(() => setCopied(false), 2000);
  }

  async function handleRegenerateFeedLink() {
    setFeedError(null);
    setFeedLoading(true);
    try {
      const { token } = await regenerateCalendarToken();
      setFeedUrl(buildCalendarFeedUrl(token));
      setCopied(false);
    } catch (err) {
      setFeedError(extractErrorMessage(err));
    } finally {
      setFeedLoading(false);
    }
  }

  async function handleSchedule(event: FormEvent) {
    event.preventDefault();
    setFormError(null);
    setSubmitting(true);
    try {
      const created = await createMeeting({
        title,
        description: description || undefined,
        location: location || undefined,
        scheduledStart: new Date(scheduledStart).toISOString(),
        scheduledEnd: scheduledEnd ? new Date(scheduledEnd).toISOString() : undefined,
        committeeId: committeeId || undefined,
        meetingType: meetingType || undefined,
      });
      if (!committeeFilter || committeeFilter === created.committeeId) {
        setMeetings((prev) => [created, ...prev]);
      }
      setTitle("");
      setDescription("");
      setLocation("");
      setScheduledStart("");
      setScheduledEnd("");
      setCommitteeId("");
      setMeetingType("");
    } catch (err) {
      setFormError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Meetings</h1>
          <p>Board and committee meetings for {user.organizationName}</p>
        </div>

        <section className="dashboard-section">
          <div className="field-row">
            <label>
              Filter by committee
              <select value={committeeFilter} onChange={(e) => setCommitteeFilter(e.target.value)}>
                <option value="">All meetings</option>
                {flatCommittees.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.parentCommitteeId ? `— ${c.name}` : c.name}
                  </option>
                ))}
              </select>
            </label>
          </div>

          {loading && <p>Loading meetings...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {!loading && !loadError && meetings.length === 0 && (
            <div className="empty-state">
              <EmptyMeetingsIcon />
              <p>No meetings scheduled yet.</p>
            </div>
          )}
          {!loading && !loadError && meetings.length > 0 && (
            <div className="table-scroll">
            <table className="user-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Date</th>
                  <th>Location</th>
                  <th>Committee</th>
                  <th>Type</th>
                  <th>Status</th>
                </tr>
              </thead>
              <tbody>
                {meetings.map((m) => (
                  <tr key={m.id}>
                    <td>
                      <Link to={`/meetings/${m.id}`}>{m.title}</Link>
                    </td>
                    <td>{new Date(m.scheduledStart).toLocaleString()}</td>
                    <td>{m.location ?? "—"}</td>
                    <td>{flatCommittees.find((c) => c.id === m.committeeId)?.name ?? "—"}</td>
                    <td>
                      {m.meetingType ? <span className="badge badge-category">{m.meetingType}</span> : "—"}
                    </td>
                    <td>
                      <StatusBadge status={m.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
            </div>
          )}

          <h3>Subscribe to calendar</h3>
          <p className="table-hint">
            Get a personal link you can add to Google Calendar, Outlook, or Apple Calendar as a subscription — new
            meetings show up automatically. Treat the link like a password; anyone with it can see your meeting
            schedule.
          </p>
          {feedError && <p className="form-error">{feedError}</p>}
          {!feedUrl && (
            <button className="secondary small" disabled={feedLoading} onClick={handleShowFeedLink}>
              {feedLoading ? "Loading..." : "Get calendar link"}
            </button>
          )}
          {feedUrl && (
            <div className="field-row">
              <input value={feedUrl} readOnly onFocus={(e) => e.target.select()} />
              <button className="secondary small" onClick={handleCopyFeedLink}>
                {copied ? "Copied!" : "Copy link"}
              </button>
              <button className="secondary small" disabled={feedLoading} onClick={handleRegenerateFeedLink}>
                Regenerate link
              </button>
            </div>
          )}

          {canManage && (
            <>
              <h3>Schedule a meeting</h3>
              <form className="add-user-form" onSubmit={handleSchedule}>
                <label>
                  Title
                  <input value={title} onChange={(e) => setTitle(e.target.value)} required />
                </label>
                <label>
                  Description
                  <input value={description} onChange={(e) => setDescription(e.target.value)} />
                </label>
                <div className="field-row">
                  <label>
                    Location
                    <input value={location} onChange={(e) => setLocation(e.target.value)} />
                  </label>
                  <label>
                    Start
                    <input
                      type="datetime-local"
                      value={scheduledStart}
                      onChange={(e) => setScheduledStart(e.target.value)}
                      required
                    />
                  </label>
                  <label>
                    End (optional)
                    <input
                      type="datetime-local"
                      value={scheduledEnd}
                      onChange={(e) => setScheduledEnd(e.target.value)}
                    />
                  </label>
                </div>
                <label>
                  Committee (optional)
                  <select value={committeeId} onChange={(e) => setCommitteeId(e.target.value)}>
                    <option value="">Full board</option>
                    {flatCommittees.map((c) => (
                      <option key={c.id} value={c.id}>
                        {c.parentCommitteeId ? `— ${c.name}` : c.name}
                      </option>
                    ))}
                  </select>
                </label>
                <label>
                  Meeting type (optional)
                  <select
                    value={meetingType}
                    onChange={(e) => setMeetingType(e.target.value as MeetingType | "")}
                  >
                    <option value="">Ordinary meeting</option>
                    {(Object.keys(MEETING_TYPE_LABELS) as MeetingType[]).map((type) => (
                      <option key={type} value={type}>
                        {MEETING_TYPE_LABELS[type]}
                      </option>
                    ))}
                  </select>
                </label>
                {formError && <p className="form-error">{formError}</p>}
                <button type="submit" disabled={submitting}>
                  {submitting ? "Scheduling..." : "Schedule meeting"}
                </button>
              </form>
            </>
          )}
        </section>
      </main>
    </div>
  );
}
