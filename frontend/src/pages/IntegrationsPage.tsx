import { useEffect, useState, type FormEvent } from "react";
import { createApiKey, listApiKeys, revokeApiKey } from "../api/apiKeys";
import { extractErrorMessage } from "../api/client";
import type { ApiKeySummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

function EmptyKeysIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M15.75 5.25a3 3 0 013 3m3 0a6 6 0 01-7.029 5.912c-.563-.097-1.159.026-1.563.43L10.5 17.25H8.25v2.25H6v2.25H2.25v-2.818c0-.597.237-1.17.659-1.591l6.499-6.499c.404-.404.527-1 .43-1.563A6 6 0 1121.75 8.25z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function IntegrationsPage() {
  const { user } = useAuth();

  const [keys, setKeys] = useState<ApiKeySummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [newName, setNewName] = useState("");
  const [creating, setCreating] = useState(false);
  const [revealedKey, setRevealedKey] = useState<string | null>(null);
  const [copied, setCopied] = useState(false);

  const [busyId, setBusyId] = useState<string | null>(null);

  useEffect(() => {
    listApiKeys()
      .then(setKeys)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setActionError(null);
    setCreating(true);
    try {
      const created = await createApiKey({ name: newName });
      setKeys((prev) => [created.key, ...prev]);
      setRevealedKey(created.rawKey);
      setCopied(false);
      setNewName("");
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setCreating(false);
    }
  }

  async function handleRevoke(id: string) {
    setActionError(null);
    setBusyId(id);
    try {
      await revokeApiKey(id);
      setKeys((prev) => prev.filter((k) => k.id !== id));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setBusyId(null);
    }
  }

  async function handleCopy() {
    if (!revealedKey) return;
    await navigator.clipboard.writeText(revealedKey);
    setCopied(true);
  }

  if (!user) return null;

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Integrations</h1>
          <p>API keys for read-only programmatic access to {user.organizationName}&apos;s data</p>
        </div>

        {revealedKey && (
          <section className="dashboard-section key-reveal">
            <h2>Your new API key</h2>
            <p className="form-error">Copy this now &mdash; it won&apos;t be shown again.</p>
            <div className="key-reveal-value">
              <code>{revealedKey}</code>
              <button type="button" className="secondary small" onClick={handleCopy}>
                {copied ? "Copied!" : "Copy"}
              </button>
            </div>
            <button type="button" className="secondary small" onClick={() => setRevealedKey(null)}>
              Done
            </button>
          </section>
        )}

        <section className="dashboard-section">
          <h2>API keys</h2>
          {loading && <p>Loading API keys...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {actionError && <p className="form-error">{actionError}</p>}

          {!loading && !loadError && keys.length === 0 && (
            <div className="empty-state">
              <EmptyKeysIcon />
              <p>No API keys yet.</p>
            </div>
          )}

          {!loading &&
            !loadError &&
            keys.map((key) => (
              <div key={key.id} className="resolution-card">
                <div className="resolution-card-header">
                  <div>
                    <strong>{key.name}</strong>
                    <p className="table-hint">
                      <code>{key.keyPrefix}&hellip;</code>
                    </p>
                  </div>
                  <button
                    className="secondary small"
                    disabled={busyId === key.id}
                    onClick={() => handleRevoke(key.id)}
                  >
                    {busyId === key.id ? "Revoking..." : "Revoke"}
                  </button>
                </div>
                <p className="table-hint">
                  Created {new Date(key.createdAt).toLocaleDateString()}
                  {key.lastUsedAt
                    ? ` · Last used ${new Date(key.lastUsedAt).toLocaleDateString()}`
                    : " · Never used"}
                </p>
              </div>
            ))}
        </section>

        <section className="dashboard-section">
          <h2>Generate a new key</h2>
          <form className="add-user-form" onSubmit={handleCreate}>
            <label>
              Name
              <input
                value={newName}
                onChange={(e) => setNewName(e.target.value)}
                placeholder="e.g. Zapier sync"
                required
              />
            </label>
            <button type="submit" disabled={creating}>
              {creating ? "Generating..." : "Generate key"}
            </button>
          </form>
        </section>

        <section className="dashboard-section">
          <h2>API reference</h2>
          <p>
            Send the key in an <code>X-Api-Key</code> header. Every endpoint returns JSON scoped to your
            organization and is read-only.
          </p>
          <div className="table-scroll">
            <table className="user-table">
              <thead>
                <tr>
                  <th>Endpoint</th>
                  <th>Returns</th>
                </tr>
              </thead>
              <tbody>
                <tr>
                  <td>
                    <code>GET /api/v1/meetings</code>
                  </td>
                  <td>All meetings</td>
                </tr>
                <tr>
                  <td>
                    <code>GET /api/v1/resolutions</code>
                  </td>
                  <td>All resolutions</td>
                </tr>
                <tr>
                  <td>
                    <code>GET /api/v1/action-items</code>
                  </td>
                  <td>All action items</td>
                </tr>
                <tr>
                  <td>
                    <code>GET /api/v1/documents</code>
                  </td>
                  <td>Document metadata (latest version of each)</td>
                </tr>
              </tbody>
            </table>
          </div>
        </section>
      </main>
    </div>
  );
}
