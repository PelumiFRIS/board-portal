import { useEffect, useState } from "react";
import { downloadAuditLogCsv, listAuditLogs } from "../api/auditLogs";
import { downloadResolutionsCsv } from "../api/resolutions";
import { downloadActionItemsCsv } from "../api/actionItems";
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
  const [exportError, setExportError] = useState<string | null>(null);
  const [exporting, setExporting] = useState(false);
  const [resolutionsExportError, setResolutionsExportError] = useState<string | null>(null);
  const [exportingResolutions, setExportingResolutions] = useState(false);
  const [actionItemsExportError, setActionItemsExportError] = useState<string | null>(null);
  const [exportingActionItems, setExportingActionItems] = useState(false);

  useEffect(() => {
    listAuditLogs()
      .then(setEntries)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function handleExport() {
    setExportError(null);
    setExporting(true);
    try {
      await downloadAuditLogCsv();
    } catch (err) {
      setExportError(extractErrorMessage(err));
    } finally {
      setExporting(false);
    }
  }

  async function handleExportResolutions() {
    setResolutionsExportError(null);
    setExportingResolutions(true);
    try {
      await downloadResolutionsCsv();
    } catch (err) {
      setResolutionsExportError(extractErrorMessage(err));
    } finally {
      setExportingResolutions(false);
    }
  }

  async function handleExportActionItems() {
    setActionItemsExportError(null);
    setExportingActionItems(true);
    try {
      await downloadActionItemsCsv();
    } catch (err) {
      setActionItemsExportError(extractErrorMessage(err));
    } finally {
      setExportingActionItems(false);
    }
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header page-header-with-actions">
          <div>
            <h1>Audit trail</h1>
            <p>Recent activity across {user.organizationName}</p>
          </div>
          <div className="field-row">
            <button className="secondary small" disabled={exporting} onClick={handleExport}>
              {exporting ? "Exporting..." : "Export Audit Trail CSV"}
            </button>
            <button className="secondary small" disabled={exportingResolutions} onClick={handleExportResolutions}>
              {exportingResolutions ? "Exporting..." : "Export Resolutions CSV"}
            </button>
            <button className="secondary small" disabled={exportingActionItems} onClick={handleExportActionItems}>
              {exportingActionItems ? "Exporting..." : "Export Action Items CSV"}
            </button>
          </div>
        </div>
        {exportError && <p className="form-error">{exportError}</p>}
        {resolutionsExportError && <p className="form-error">{resolutionsExportError}</p>}
        {actionItemsExportError && <p className="form-error">{actionItemsExportError}</p>}

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
