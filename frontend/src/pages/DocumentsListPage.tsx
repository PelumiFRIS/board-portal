import { Fragment, useEffect, useState, type FormEvent } from "react";
import {
  deleteDocument,
  downloadDocument,
  getDocument,
  listDocuments,
  listDocumentVersions,
  signDocument,
  updateDocumentRetention,
  uploadDocument,
  uploadNewVersion,
} from "../api/documents";
import { extractErrorMessage } from "../api/client";
import { listMeetings } from "../api/meetings";
import { listCommittees } from "../api/committees";
import type { CommitteeSummary, DocumentCategory, DocumentDetail, DocumentSummary, MeetingSummary } from "../api/types";
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
  const canManage = user?.role === "ADMIN" || user?.role === "EXECUTIVE";

  const [documents, setDocuments] = useState<DocumentSummary[]>([]);
  const [meetings, setMeetings] = useState<MeetingSummary[]>([]);
  const [committees, setCommittees] = useState<CommitteeSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [loadError, setLoadError] = useState<string | null>(null);
  const [actionError, setActionError] = useState<string | null>(null);
  const [committeeFilter, setCommitteeFilter] = useState("");

  const [file, setFile] = useState<File | null>(null);
  const [title, setTitle] = useState("");
  const [description, setDescription] = useState("");
  const [category, setCategory] = useState<DocumentCategory>("BOARD_PACK");
  const [meetingId, setMeetingId] = useState("");
  const [committeeId, setCommitteeId] = useState("");
  const [submitting, setSubmitting] = useState(false);

  const flatCommittees = committees.flatMap((c) => [c, ...c.subCommittees]);

  const [editingRetentionId, setEditingRetentionId] = useState<string | null>(null);
  const [retentionDraft, setRetentionDraft] = useState("");
  const [savingRetention, setSavingRetention] = useState(false);

  const [signingId, setSigningId] = useState<string | null>(null);
  const [expandedSignaturesId, setExpandedSignaturesId] = useState<string | null>(null);
  const [signatureDetail, setSignatureDetail] = useState<DocumentDetail | null>(null);
  const [loadingSignatures, setLoadingSignatures] = useState(false);

  const [expandedVersionsId, setExpandedVersionsId] = useState<string | null>(null);
  const [versionList, setVersionList] = useState<DocumentSummary[]>([]);
  const [loadingVersions, setLoadingVersions] = useState(false);
  const [newVersionFile, setNewVersionFile] = useState<File | null>(null);
  const [uploadingVersionId, setUploadingVersionId] = useState<string | null>(null);

  useEffect(() => {
    Promise.all([listMeetings(), listCommittees()])
      .then(([mtgs, cmts]) => {
        setMeetings(mtgs);
        setCommittees(cmts);
      })
      .catch((err) => setLoadError(extractErrorMessage(err)));
  }, []);

  useEffect(() => {
    setLoading(true);
    listDocuments({ committeeId: committeeFilter || undefined })
      .then(setDocuments)
      .catch((err) => setLoadError(extractErrorMessage(err)))
      .finally(() => setLoading(false));
  }, [committeeFilter]);

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
        committeeId: committeeId || undefined,
      });
      if (!committeeFilter || committeeFilter === created.committeeId) {
        setDocuments((prev) => [created, ...prev]);
      }
      setFile(null);
      setTitle("");
      setDescription("");
      setCategory("BOARD_PACK");
      setMeetingId("");
      setCommitteeId("");
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

  function startEditingRetention(doc: DocumentSummary) {
    setActionError(null);
    setEditingRetentionId(doc.id);
    setRetentionDraft(doc.retentionUntil ?? "");
  }

  async function handleSaveRetention(docId: string) {
    setActionError(null);
    setSavingRetention(true);
    try {
      const updated = await updateDocumentRetention(docId, retentionDraft || null);
      setDocuments((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
      setEditingRetentionId(null);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSavingRetention(false);
    }
  }

  async function handleSign(doc: DocumentSummary) {
    setActionError(null);
    setSigningId(doc.id);
    try {
      const updated = await signDocument(doc.id);
      setDocuments((prev) => prev.map((d) => (d.id === updated.id ? updated : d)));
      if (expandedSignaturesId === doc.id) {
        const detail = await getDocument(doc.id);
        setSignatureDetail(detail);
      }
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setSigningId(null);
    }
  }

  async function handleToggleSignatures(doc: DocumentSummary) {
    if (expandedSignaturesId === doc.id) {
      setExpandedSignaturesId(null);
      setSignatureDetail(null);
      return;
    }
    setActionError(null);
    setExpandedSignaturesId(doc.id);
    setLoadingSignatures(true);
    try {
      const detail = await getDocument(doc.id);
      setSignatureDetail(detail);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setLoadingSignatures(false);
    }
  }

  async function handleToggleVersions(doc: DocumentSummary) {
    if (expandedVersionsId === doc.id) {
      setExpandedVersionsId(null);
      setVersionList([]);
      setNewVersionFile(null);
      return;
    }
    setActionError(null);
    setExpandedVersionsId(doc.id);
    setLoadingVersions(true);
    try {
      setVersionList(await listDocumentVersions(doc.id));
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setLoadingVersions(false);
    }
  }

  async function handleUploadNewVersion(doc: DocumentSummary) {
    if (!newVersionFile) return;
    setActionError(null);
    setUploadingVersionId(doc.id);
    try {
      const updated = await uploadNewVersion(doc.id, newVersionFile);
      setDocuments((prev) => prev.map((d) => (d.id === doc.id ? updated : d)));
      setExpandedVersionsId(updated.id);
      setVersionList(await listDocumentVersions(updated.id));
      setNewVersionFile(null);
    } catch (err) {
      setActionError(extractErrorMessage(err));
    } finally {
      setUploadingVersionId(null);
    }
  }

  if (!user) return null;

  const today = new Date().toISOString().slice(0, 10);

  return (
    <div className="app-shell">
      <Sidebar />
      <main className="main-content">
        <div className="page-header">
          <h1>Documents</h1>
          <p>Board packs, policies, and governance documents for {user.organizationName}</p>
        </div>

        <section className="dashboard-section">
          <div className="field-row">
            <label>
              Filter by committee
              <select value={committeeFilter} onChange={(e) => setCommitteeFilter(e.target.value)}>
                <option value="">All documents</option>
                {flatCommittees.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.parentCommitteeId ? `— ${c.name}` : c.name}
                  </option>
                ))}
              </select>
            </label>
          </div>

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
            <div className="table-scroll">
            <table className="user-table">
              <thead>
                <tr>
                  <th>Title</th>
                  <th>Category</th>
                  <th>Committee</th>
                  <th>Size</th>
                  <th>Uploaded</th>
                  <th>Retention</th>
                  <th>Sign-off</th>
                  <th></th>
                </tr>
              </thead>
              <tbody>
                {documents.map((doc) => {
                  const isPastRetention = Boolean(doc.retentionUntil) && doc.retentionUntil! < today;
                  return (
                    <Fragment key={doc.id}>
                      <tr>
                        <td>
                          {doc.title}
                          {doc.versionNumber > 1 && <span className="badge badge-category"> v{doc.versionNumber}</span>}
                        </td>
                        <td>
                          <span className="badge badge-category">{doc.category.replace("_", " ")}</span>
                        </td>
                        <td>{flatCommittees.find((c) => c.id === doc.committeeId)?.name ?? "—"}</td>
                        <td>{formatFileSize(doc.fileSize)}</td>
                        <td>{new Date(doc.createdAt).toLocaleDateString()}</td>
                        <td>
                          {editingRetentionId === doc.id ? (
                            <div className="field-row">
                              <input
                                type="date"
                                value={retentionDraft}
                                onChange={(e) => setRetentionDraft(e.target.value)}
                              />
                              <button
                                className="secondary small"
                                disabled={savingRetention}
                                onClick={() => handleSaveRetention(doc.id)}
                              >
                                {savingRetention ? "Saving..." : "Save"}
                              </button>
                              <button className="secondary small" onClick={() => setEditingRetentionId(null)}>
                                Cancel
                              </button>
                            </div>
                          ) : (
                            <div className="field-row">
                              {doc.retentionUntil ? (
                                <span className="table-hint">{new Date(doc.retentionUntil).toLocaleDateString()}</span>
                              ) : (
                                <span className="table-hint">&mdash;</span>
                              )}
                              {isPastRetention && <span className="badge badge-cancelled">Review overdue</span>}
                              {canManage && (
                                <button className="secondary small" onClick={() => startEditingRetention(doc)}>
                                  Edit
                                </button>
                              )}
                            </div>
                          )}
                        </td>
                        <td>
                          <div className="field-row">
                            <button className="secondary small" onClick={() => handleToggleSignatures(doc)}>
                              {doc.signatureCount} signed
                            </button>
                            {doc.signedByMe ? (
                              <span className="table-hint">Signed &#10003;</span>
                            ) : (
                              <button
                                className="small"
                                disabled={signingId === doc.id}
                                onClick={() => handleSign(doc)}
                              >
                                {signingId === doc.id ? "Signing..." : "Sign off"}
                              </button>
                            )}
                          </div>
                        </td>
                        <td>
                          <div className="field-row">
                            <button className="secondary small" onClick={() => handleDownload(doc)}>
                              Download
                            </button>
                            <button className="secondary small" onClick={() => handleToggleVersions(doc)}>
                              Version history
                            </button>
                            {canManage && (
                              <button className="secondary small" onClick={() => handleDelete(doc)}>
                                Delete
                              </button>
                            )}
                          </div>
                        </td>
                      </tr>
                      {expandedVersionsId === doc.id && (
                        <tr>
                          <td colSpan={8}>
                            {loadingVersions && <p className="table-hint">Loading versions...</p>}
                            {!loadingVersions && (
                              <ul className="vote-record-list">
                                {versionList.map((v) => (
                                  <li key={v.id}>
                                    v{v.versionNumber} &middot; {formatFileSize(v.fileSize)} &middot;{" "}
                                    {new Date(v.createdAt).toLocaleString()}{" "}
                                    <button className="secondary small" onClick={() => handleDownload(v)}>
                                      Download
                                    </button>
                                  </li>
                                ))}
                              </ul>
                            )}
                            {canManage && (
                              <div className="field-row">
                                <input
                                  type="file"
                                  onChange={(e) => setNewVersionFile(e.target.files?.[0] ?? null)}
                                />
                                <button
                                  className="secondary small"
                                  disabled={!newVersionFile || uploadingVersionId === doc.id}
                                  onClick={() => handleUploadNewVersion(doc)}
                                >
                                  {uploadingVersionId === doc.id ? "Uploading..." : "Upload new version"}
                                </button>
                              </div>
                            )}
                          </td>
                        </tr>
                      )}
                      {expandedSignaturesId === doc.id && (
                        <tr>
                          <td colSpan={8}>
                            {loadingSignatures && <p className="table-hint">Loading signers...</p>}
                            {!loadingSignatures && signatureDetail && signatureDetail.signatures.length === 0 && (
                              <p className="table-hint">No one has signed yet.</p>
                            )}
                            {!loadingSignatures && signatureDetail && signatureDetail.signatures.length > 0 && (
                              <ul className="vote-record-list">
                                {signatureDetail.signatures.map((s) => (
                                  <li key={s.userId}>
                                    {s.userName} &middot; {new Date(s.signedAt).toLocaleString()}
                                  </li>
                                ))}
                              </ul>
                            )}
                          </td>
                        </tr>
                      )}
                    </Fragment>
                  );
                })}
              </tbody>
            </table>
            </div>
          )}

          {canManage && (
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
                  <label>
                    Committee (optional)
                    <select value={committeeId} onChange={(e) => setCommitteeId(e.target.value)}>
                      <option value="">None</option>
                      {flatCommittees.map((c) => (
                        <option key={c.id} value={c.id}>
                          {c.parentCommitteeId ? `— ${c.name}` : c.name}
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
