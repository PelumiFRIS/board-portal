import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { getMeeting, listMeetings } from "../api/meetings";
import { listActionItems, updateActionItemStatus } from "../api/actionItems";
import { extractErrorMessage } from "../api/client";
import type { AgendaItem, ActionItemSummary, MeetingSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

function EmptyStateIcon() {
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

interface AgendaGroup {
  meeting: MeetingSummary;
  items: AgendaItem[];
}

interface MatterGroup {
  meetingId: string;
  meetingTitle: string;
  items: ActionItemSummary[];
}

export function MattersArisingPage() {
  const { user } = useAuth();

  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [agendaGroups, setAgendaGroups] = useState<AgendaGroup[]>([]);
  const [loadingAgenda, setLoadingAgenda] = useState(true);
  const [agendaError, setAgendaError] = useState<string | null>(null);

  const [actionItems, setActionItems] = useState<ActionItemSummary[]>([]);
  const [loadingMatters, setLoadingMatters] = useState(true);
  const [mattersError, setMattersError] = useState<string | null>(null);
  const [busyId, setBusyId] = useState<string | null>(null);

  useEffect(() => {
    listMeetings()
      .then((all) => {
        setMeetings(all);
        const upcoming = all
          .filter((m) => m.status === "SCHEDULED")
          .sort((a, b) => new Date(a.scheduledStart).getTime() - new Date(b.scheduledStart).getTime());
        return Promise.all(upcoming.map((m) => getMeeting(m.id).then((detail) => ({ meeting: m, items: detail.agendaItems }))));
      })
      .then((groups) => setAgendaGroups(groups.filter((g) => g.items.length > 0)))
      .catch((err) => setAgendaError(extractErrorMessage(err)))
      .finally(() => setLoadingAgenda(false));

    listActionItems()
      .then(setActionItems)
      .catch((err) => setMattersError(extractErrorMessage(err)))
      .finally(() => setLoadingMatters(false));
  }, []);

  async function handleMarkDone(itemId: string) {
    setMattersError(null);
    setBusyId(itemId);
    try {
      const updated = await updateActionItemStatus(itemId, "DONE");
      setActionItems((prev) => prev.map((i) => (i.id === updated.id ? updated : i)));
    } catch (err) {
      setMattersError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  if (!user) return null;

  const meetingTitleById = new Map(meetings.map((m) => [m.id, m.title]));

  const openItems = actionItems.filter((i) => i.status === "OPEN");
  const matterGroups: MatterGroup[] = [];
  const groupIndexByMeeting = new Map<string, number>();
  for (const item of openItems) {
    let index = groupIndexByMeeting.get(item.meetingId);
    if (index === undefined) {
      index = matterGroups.length;
      groupIndexByMeeting.set(item.meetingId, index);
      matterGroups.push({
        meetingId: item.meetingId,
        meetingTitle: meetingTitleById.get(item.meetingId) ?? "Unknown meeting",
        items: [],
      });
    }
    matterGroups[index].items.push(item);
  }
  for (const group of matterGroups) {
    group.items.sort((a, b) => {
      if (!a.dueDate) return 1;
      if (!b.dueDate) return -1;
      return new Date(a.dueDate).getTime() - new Date(b.dueDate).getTime();
    });
  }

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Agenda &amp; Matters Arising</h1>
          <p>What's coming up and what's still outstanding, across every meeting</p>
        </div>

        <section className="dashboard-section">
          <h2>Upcoming agenda</h2>
          {loadingAgenda && <p>Loading agenda items...</p>}
          {agendaError && <p className="form-error">{agendaError}</p>}
          {!loadingAgenda && !agendaError && agendaGroups.length === 0 && (
            <div className="empty-state">
              <EmptyStateIcon />
              <p>No agenda items on any upcoming meeting yet.</p>
            </div>
          )}
          {!loadingAgenda &&
            agendaGroups.map(({ meeting, items }) => (
              <div key={meeting.id} className="resolution-card">
                <div className="resolution-card-header">
                  <strong>
                    <Link to={`/meetings/${meeting.id}`}>{meeting.title}</Link>
                  </strong>
                  <span className="table-hint">{new Date(meeting.scheduledStart).toLocaleDateString()}</span>
                </div>
                {items.map((item) => (
                  <div key={item.id} className="agenda-item-row">
                    <div className="agenda-item-body">
                      <strong>{item.title}</strong>
                      {item.description && <p>{item.description}</p>}
                    </div>
                  </div>
                ))}
              </div>
            ))}
        </section>

        <section className="dashboard-section">
          <h2>Open matters arising</h2>
          <p className="table-hint">Still-open action items from every meeting, so nothing gets lost.</p>
          {loadingMatters && <p>Loading matters arising...</p>}
          {mattersError && <p className="form-error">{mattersError}</p>}
          {!loadingMatters && !mattersError && matterGroups.length === 0 && (
            <div className="empty-state">
              <EmptyStateIcon />
              <p>Nothing outstanding right now.</p>
            </div>
          )}
          {!loadingMatters &&
            matterGroups.map((group) => (
              <div key={group.meetingId} className="resolution-card">
                <div className="resolution-card-header">
                  <strong>
                    <Link to={`/meetings/${group.meetingId}`}>{group.meetingTitle}</Link>
                  </strong>
                </div>
                {group.items.map((item) => (
                  <div key={item.id} className="agenda-item-row">
                    <div className="agenda-item-body">
                      <strong>{item.title}</strong>
                      {item.description && <p>{item.description}</p>}
                      <p className="table-hint">
                        Assigned to {item.assigneeName}
                        {item.dueDate ? ` · Due ${new Date(item.dueDate).toLocaleDateString()}` : ""}
                      </p>
                    </div>
                    <button className="small" disabled={busyId === item.id} onClick={() => handleMarkDone(item.id)}>
                      {busyId === item.id ? "Saving..." : "Mark done"}
                    </button>
                  </div>
                ))}
              </div>
            ))}
        </section>
      </main>
    </div>
  );
}
