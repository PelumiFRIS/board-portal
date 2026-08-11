import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { createMeeting, listMeetings } from "../api/meetings";
import { extractErrorMessage } from "../api/client";
import type { MeetingSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { StatusBadge } from "../components/StatusBadge";
import { useAuth } from "../context/AuthContext";

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
  const isAdmin = user?.role === "ADMIN";

  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [location, setLocation] = useState("");
  const [scheduledStart, setScheduledStart] = useState("");
  const [scheduledEnd, setScheduledEnd] = useState("");
  const [formError, setFormError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    listMeetings()
      .then(setMeetings)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

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
      });
      setMeetings((prev) => [created, ...prev]);
      setTitle("");
      setDescription("");
      setLocation("");
      setScheduledStart("");
      setScheduledEnd("");
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
          {loading && <p>Loading meetings...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {!loading && !loadError && meetings.length === 0 && (
            <div className="empty-state">
              <EmptyMeetingsIcon />
              <p>No meetings scheduled yet.</p>
            </div>
          )}
          {!loading && !loadError && meetings.length > 0 && (
            <table className="user-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Date</th>
                  <th>Location</th>
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
                    <td>
                      <StatusBadge status={m.status} />
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {isAdmin && (
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
