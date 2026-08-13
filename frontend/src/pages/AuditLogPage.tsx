import { useEffect, useState } from "react";
import { listAuditLogs } from "../api/auditLogs";
import { extractErrorMessage } from "../api/client";
import type { AuditLogEntry } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

function EmptyAuditIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M12 8v4l2.5 2.5M21 12a9 9 0 11-9-9 9 9 0 019 9z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function AuditLogPage() {
  const { user } = useAuth();

  const [entries, setEntries] = useState<AuditLogEntry[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);

  useEffect(() => {
    listAuditLogs()
      .then(setEntries)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Audit trail</h1>
          <p>Recent activity across {user.organizationName}</p>
        </div>

        <section className="dashboard-section">
          {loading && <p>Loading activity...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {!loading && !loadError && entries.length === 0 && (
            <div className="empty-state">
              <EmptyAuditIcon />
              <p>No activity recorded yet.</p>
            </div>
          )}
          {!loading && !loadError && entries.length > 0 && (
            <table className="user-table">
              <thead>
                <tr>
                  <th>When</th>
                  <th>Who</th>
                  <th>What</th>
                </tr>
              </thead>
              <tbody>
                {entries.map((entry) => (
                  <tr key={entry.id}>
                    <td>{new Date(entry.createdAt).toLocaleString()}</td>
                    <td>{entry.actorName}</td>
                    <td>{entry.summary}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </section>
      </main>
    </div>
  );
}
