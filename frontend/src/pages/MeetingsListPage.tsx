import { useEffect, useState, type FormEvent } from "react";
import { Link } from "react-router-dom";
import { createMeeting, listMeetings } from "../api/meetings";
import { extractErrorMessage } from "../api/client";
import type { MeetingSummary } from "../api/types";
import { AppHeader } from "../components/AppHeader";
import { useAuth } from "../context/AuthContext";

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
    <div className="dashboard">
      <AppHeader />

      <section className="dashboard-section">
        <h2>Meetings</h2>
        {loading && <p>Loading meetings...</p>}
        {loadError && <p className="form-error">{loadError}</p>}
        {!loading && !loadError && meetings.length === 0 && <p>No meetings scheduled yet.</p>}
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
                  <td>{m.status}</td>
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
    </div>
  );
}
