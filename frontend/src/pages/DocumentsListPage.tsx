import { useEffect, useState, type FormEvent } from "react";
import { deleteDocument, downloadDocument, listDocuments, uploadDocument } from "../api/documents";
import { extractErrorMessage } from "../api/client";
import { listMeetings } from "../api/meetings";
import type { DocumentCategory, DocumentSummary, MeetingSummary } from "../api/types";
import { Sidebar } from "../components/Sidebar";
import { useAuth } from "../context/AuthContext";

const CATEGORY_OPTIONS: DocumentCategory[] = [
  "BOARD_PACK",
  "MINUTES",
  "REPORT",
  "POLICY",
  "BYLAW",
  "CHARTER",
  "GOVERNANCE",
  "OTHER",
];

function formatFileSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / (1024 * 1024)).toFixed(1)} MB`;
}

function EmptyDocumentsIcon() {
  return (
    <svg viewBox="0 0 24 24" fill="none" stroke="currentColor">
      <path
        d="M19.5 14.25v-2.625a3.375 3.375 0 00-3.375-3.375h-1.5A1.125 1.125 0 0113.5 7.125v-1.5a3.375 3.375 0 00-3.375-3.375H8.25m0 12.75h7.5m-7.5 3H12M10.5 2.25H5.625c-.621 0-1.125.504-1.125 1.125v17.25c0 .621.504 1.125 1.125 1.125h12.75c.621 0 1.125-.504 1.125-1.125V11.25a9 9 0 00-9-9z"
        strokeWidth="1.5"
        strokeLinecap="round"
        strokeLinejoin="round"
      />
    </svg>
  );
}

export function DocumentsListPage() {
  const { user } = useAuth();
  const isAdmin = user?.role === "ADMIN";

  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);

  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState<DocumentCategory>("BOARD_PACK");
  const [meetingId, setMeetingId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    Promise.all([listDocuments(), listMeetings()])
      .then(([docs, mtgs]) => {
        setDocuments(docs);
        setMeetings(mtgs);
      })
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, []);

  async function handleUpload(event: FormEvent) {
    event.preventDefault();
    if (!file) {
      setActionError("Choose a file to upload.");
      return;
    }
    setActionError(null);
    setSubmitting(true);
    try {
      const created = await uploadDocument({
        file,
        title,
        description: description || undefined,
        category,
        meetingId: meetingId || undefined,
      });
      setDocuments((prev) => [created, ...prev]);
      setFile(null);
      setTitle("");
      setDescription("");
      setCategory("BOARD_PACK");
      setMeetingId("");
      (event.target as HTMLFormElement).reset();
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  async function handleDelete(doc: DocumentSummary) {
    setActionError(null);
    try {
      await deleteDocument(doc.id);
      setDocuments((prev) => prev.filter((d) => d.id !== doc.id));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    }
  }

  async function handleDownload(doc: DocumentSummary) {
    try {
      await downloadDocument(doc.id, doc.fileName);
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
          <h1>Documents</h1>
          <p>Board packs, policies, and governance documents for {user.organizationName}</p>
        </div>

        <section className="dashboard-section">
          {loading && <p>Loading documents...</p>}
          {loadError && <p className="form-error">{loadError}</p>}
          {actionError && <p className="form-error">{actionError}</p>}
          {!loading && !loadError && documents.length === 0 && (
            <div className="empty-state">
              <EmptyDocumentsIcon />
              <p>No documents uploaded yet.</p>
            </div>
          )}
          {!loading && !loadError && documents.length > 0 && (
            <table className="user-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Category</th>
                  <th>Size</th>
                  <th>Uploaded</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {documents.map((doc) => (
                  <tr key={doc.id}>
                    <td>{doc.title}</td>
                    <td>
                      <span className="badge badge-category">{doc.category.replace("_", " ")}</span>
                    </td>
                    <td>{formatFileSize(doc.fileSize)}</td>
                    <td>{new Date(doc.createdAt).toLocaleDateString()}</td>
                    <td>
                      <div className="field-row">
                        <button className="secondary small" onClick={() => handleDownload(doc)}>
                          Download
                        </button>
                        {isAdmin && (
                          <button className="secondary small" onClick={() => handleDelete(doc)}>
                            Delete
                          </button>
                        )}
                      </div>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}

          {isAdmin && (
            <>
              <h3>Upload a document</h3>
              <form className="add-user-form" onSubmit={handleUpload}>
                <label>
                  File
                  <input type="file" onChange={(e) => setFile(e.target.files?.[0] ?? null)} required />
                </label>
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
                    Category
                    <select value={category} onChange={(e) => setCategory(e.target.value as DocumentCategory)}>
                      {CATEGORY_OPTIONS.map((option) => (
                        <option key={option} value={option}>
                          {option.replace("_", " ")}
                        </option>
                      ))}
                    </select>
                  </label>
                  <label>
                    Linked meeting (optional)
                    <select value={meetingId} onChange={(e) => setMeetingId(e.target.value)}>
                      <option value="">None</option>
                      {meetings.map((m) => (
                        <option key={m.id} value={m.id}>
                          {m.title}
                        </option>
                      ))}
                    </select>
                  </label>
                </div>
                <button type="submit" disabled={submitting}>
                  {submitting ? "Uploading..." : "Upload document"}
                </button>
              </form>
            </>
          )}
        </section>
      </main>
    </div>
  );
}
