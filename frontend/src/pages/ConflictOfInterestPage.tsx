import { useEffect, useState, type FormEvent } from "react";
import { createDeclaration, listAllDeclarations, listMyDeclarations } from "../api/conflictDeclarations";
import { extractErrorMessage } from "../api/client";
import type { ConflictDeclarationSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

function EmptyDeclarationsIcon() {
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

function DeclarationRow({ declaration, showName }: { declaration: ConflictDeclarationSummary; showName: boolean }) {
  return (
    <div className="resolution-card">
      <div className="resolution-card-header">
        <div>
          {showName && <strong>{declaration.userName}</strong>}
          <p className="table-hint">{new Date(declaration.declaredAt).toLocaleString()}</p>
        </div>
        <span className={`badge ${declaration.hasConflict ? "badge-cancelled" : "badge-active"}`}>
          {declaration.hasConflict ? "Has conflict" : "No conflict"}
        </span>
      </div>
      {declaration.details && <p>{declaration.details}</p>}
      <p className="table-hint">Declared by {declaration.declaredByName}</p>
    </div>
  );
}

export function ConflictOfInterestPage() {
  const { user } = useAuth();
  const canManage = user?.role === "ADMIN" || user?.role === "EXECUTIVE";

  const [myDeclarations, setMyDeclarations] = useState<ConflictDeclarationSummary[]>([]);
  const [allDeclarations, setAllDeclarations] = useState<ConflictDeclarationSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [hasConflict, setHasConflict] = useState(false);
  const [details, setDetails] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    const requests: [Promise<ConflictDeclarationSummary[]>, Promise<ConflictDeclarationSummary[]>] = [
      listMyDeclarations(),
      canManage ? listAllDeclarations() : Promise.resolve([]),
    ];
    Promise.all(requests)
      .then(([mine, all]) => {
        setMyDeclarations(mine);
        setAllDeclarations(all);
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [canManage]);

  async function handleDeclare(event: FormEvent) {
    event.preventDefault();
    setActionError(null);
    setSubmitting(true);
    try {
      const created = await createDeclaration({ hasConflict, details: details || undefined });
      setMyDeclarations((prev) => [created, ...prev]);
      if (canManage) {
        setAllDeclarations((prev) => [created, ...prev]);
      }
      setHasConflict(false);
      setDetails("");
    } catch (err) {
      setActionError(extractErrorMessage(err));
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
          <h1>Conflicts of Interest</h1>
          <p>Declare and review conflicts of interest for {user.organizationName}</p>
        </div>

        <section className="dashboard-section">
          <h2>Declare a conflict</h2>
          {actionError && <p className="form-error">{actionError}</p>}
          <form className="add-user-form" onSubmit={handleDeclare}>
            <label>
              Do you have a conflict to declare?
              <select value={hasConflict ? "yes" : "no"} onChange={(e) => setHasConflict(e.target.value === "yes")}>
                <option value="no">No, nothing to declare</option>
                <option value="yes">Yes, I have a conflict to declare</option>
              </select>
            </label>
            {hasConflict && (
              <label>
                Details
                <input value={details} onChange={(e) => setDetails(e.target.value)} required />
              </label>
            )}
            <button type="submit" disabled={submitting}>
              {submitting ? "Submitting..." : "Submit declaration"}
            </button>
          </form>
        </section>

        <section className="dashboard-section">
          <h2>My declarations</h2>
          {loading && <p>Loading declarations...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {!loading && !loadError && myDeclarations.length === 0 && (
            <div className="empty-state">
              <EmptyDeclarationsIcon />
              <p>You haven&apos;t declared anything yet.</p>
            </div>
          )}
          {!loading &&
            !loadError &&
            myDeclarations.map((d) => <DeclarationRow key={d.id} declaration={d} showName={false} />)}
        </section>

        {canManage && (
          <section className="dashboard-section">
            <h2>Board-wide declarations</h2>
            {!loading && !loadError && allDeclarations.length === 0 && (
              <div className="empty-state">
                <EmptyDeclarationsIcon />
                <p>No declarations recorded yet.</p>
              </div>
            )}
            {!loading &&
              !loadError &&
              allDeclarations.map((d) => <DeclarationRow key={d.id} declaration={d} showName={true} />)}
          </section>
        )}
      </main>
    </div>
  );
}
