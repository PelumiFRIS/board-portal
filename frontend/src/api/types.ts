export type Role = "ADMIN" | "BOARD_MEMBER" | "EXECUTIVE";
export type UserStatus = "ACTIVE" | "DISABLED";

export interface MemberCommitteeSummary {
  committeeId: string;
  committeeName: string;
  isChair: boolean;
}

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  status: UserStatus;
  organizationId: string;
  organizationName: string;
  title: string | null;
  phone: string | null;
  bio: string | null;
  committees: MemberCommitteeSummary[];
  photoUpdatedAt: string | null;
}

export interface UpdateUserProfilePayload {
  title?: string;
  phone?: string;
  bio?: string;
}

export interface CommitteeMemberDto {
  userId: string;
  firstName: string;
  lastName: string;
  isChair: boolean;
}

export interface CommitteeSummary {
  id: string;
  name: string;
  description: string | null;
  parentCommitteeId: string | null;
  parentCommitteeName: string | null;
  members: CommitteeMemberDto[];
  subCommittees: CommitteeSummary[];
}

export interface CreateCommitteePayload {
  name: string;
  description?: string;
  parentCommitteeId?: string;
}

export interface UpdateCommitteePayload {
  name?: string;
  description?: string;
}

export interface AuthResponse {
  accessToken: string;
  user: UserSummary;
}

export interface SignupPayload {
  organizationName: string;
  adminFirstName: string;
  adminLastName: string;
  adminEmail: string;
  adminPassword: string;
}

export interface LoginPayload {
  email: string;
  password: string;
}

export interface CreateUserPayload {
  firstName: string;
  lastName: string;
  email: string;
  password: string;
  role: Role;
}

export interface ApiErrorBody {
  message: string;
  fieldErrors?: string[];
}

export type MeetingStatus = "SCHEDULED" | "COMPLETED" | "CANCELLED";

export interface MeetingSummary {
  id: string;
  title: string;
  location: string | null;
  scheduledStart: string;
  scheduledEnd: string | null;
  status: MeetingStatus;
  committeeId: string | null;
}

export interface AgendaItem {
  id: string;
  position: number;
  title: string;
  description: string | null;
}

export interface MeetingDetail {
  id: string;
  title: string;
  description: string | null;
  location: string | null;
  scheduledStart: string;
  scheduledEnd: string | null;
  status: MeetingStatus;
  minutesContent: string | null;
  committeeId: string | null;
  agendaItems: AgendaItem[];
  documents: DocumentSummary[];
  resolutions: ResolutionSummary[];
  actionItems: ActionItemSummary[];
}

export interface CreateMeetingPayload {
  title: string;
  description?: string;
  location?: string;
  scheduledStart: string;
  scheduledEnd?: string;
  committeeId?: string;
}

export interface UpdateMeetingPayload {
  title?: string;
  description?: string;
  location?: string;
  scheduledStart?: string;
  scheduledEnd?: string;
  status?: MeetingStatus;
  minutesContent?: string;
  committeeId?: string;
}

export interface CreateAgendaItemPayload {
  title: string;
  description?: string;
  position?: number;
}

export interface UpdateAgendaItemPayload {
  title?: string;
  description?: string;
  position?: number;
}

export type DocumentCategory =
  | "BOARD_PACK"
  | "MINUTES"
  | "REPORT"
  | "POLICY"
  | "BYLAW"
  | "CHARTER"
  | "GOVERNANCE"
  | "OTHER";

export interface DocumentSummary {
  id: string;
  title: string;
  description: string | null;
  category: DocumentCategory;
  fileName: string;
  contentType: string;
  fileSize: number;
  meetingId: string | null;
  committeeId: string | null;
  createdAt: string;
  retentionUntil: string | null;
  signatureCount: number;
  signedByMe: boolean;
}

export interface DocumentSignature {
  userId: string;
  userName: string;
  signedAt: string;
}

export interface DocumentDetail extends DocumentSummary {
  signatures: DocumentSignature[];
}

export type ResolutionStatus = "DRAFT" | "OPEN" | "CLOSED";
export type ResolutionOutcome = "PASSED" | "FAILED";
export type VoteChoice = "FOR" | "AGAINST" | "ABSTAIN";

export interface ResolutionSummary {
  id: string;
  meetingId: string;
  title: string;
  description: string | null;
  status: ResolutionStatus;
  outcome: ResolutionOutcome | null;
  forCount: number;
  againstCount: number;
  abstainCount: number;
  myVote: VoteChoice | null;
  createdAt: string;
  openedAt: string | null;
  closedAt: string | null;
}

export interface VoteRecord {
  voterId: string;
  voterName: string;
  choice: VoteChoice;
  castAt: string;
}

export interface ResolutionDetail extends ResolutionSummary {
  votes: VoteRecord[];
}

export interface CreateResolutionPayload {
  meetingId: string;
  title: string;
  description?: string;
}

export type AuditAction =
  | "ORGANIZATION_SIGNUP"
  | "LOGIN"
  | "USER_CREATED"
  | "USER_UPDATED"
  | "MEETING_CREATED"
  | "MEETING_UPDATED"
  | "DOCUMENT_UPLOADED"
  | "DOCUMENT_DELETED"
  | "RESOLUTION_CREATED"
  | "RESOLUTION_OPENED"
  | "RESOLUTION_CLOSED"
  | "RESOLUTION_DELETED"
  | "VOTE_CAST"
  | "ACTION_ITEM_CREATED"
  | "ACTION_ITEM_STATUS_CHANGED"
  | "ACTION_ITEM_DELETED"
  | "COMMENT_POSTED"
  | "COMMENT_DELETED"
  | "COMMITTEE_CREATED"
  | "COMMITTEE_UPDATED"
  | "COMMITTEE_DELETED"
  | "COMMITTEE_MEMBERSHIP_CHANGED"
  | "FILING_CREATED"
  | "FILING_UPDATED"
  | "FILING_SUBMITTED"
  | "FILING_DELETED"
  | "CONFLICT_DECLARED"
  | "DOCUMENT_RETENTION_SET"
  | "DOCUMENT_SIGNED";

export type AuditEntityType =
  | "ORGANIZATION"
  | "AUTH"
  | "USER"
  | "MEETING"
  | "DOCUMENT"
  | "RESOLUTION"
  | "ACTION_ITEM"
  | "COMMENT"
  | "COMMITTEE"
  | "COMPLIANCE_FILING"
  | "CONFLICT_DECLARATION";

export interface AuditLogEntry {
  id: string;
  action: AuditAction;
  entityType: AuditEntityType;
  entityId: string | null;
  summary: string;
  actorName: string;
  createdAt: string;
}

export type ActionItemStatus = "OPEN" | "DONE";

export interface ActionItemSummary {
  id: string;
  meetingId: string;
  title: string;
  description: string | null;
  assigneeId: string;
  assigneeName: string;
  dueDate: string | null;
  status: ActionItemStatus;
  createdAt: string;
}

export interface CreateActionItemPayload {
  meetingId: string;
  title: string;
  description?: string;
  assigneeId: string;
  dueDate?: string;
}

export type CommentEntityType = "MEETING" | "RESOLUTION" | "DOCUMENT" | "ACTION_ITEM";

export interface Comment {
  id: string;
  entityType: CommentEntityType;
  entityId: string;
  authorId: string;
  authorName: string;
  body: string;
  createdAt: string;
}

export interface CreateCommentPayload {
  entityType: CommentEntityType;
  entityId: string;
  body: string;
}

export type FilingStatus = "PENDING" | "SUBMITTED";

export interface ComplianceFilingSummary {
  id: string;
  title: string;
  description: string | null;
  dueDate: string;
  status: FilingStatus;
  submittedAt: string | null;
  submittedByName: string | null;
  createdAt: string;
}

export interface CreateComplianceFilingPayload {
  title: string;
  description?: string;
  dueDate: string;
}

export interface UpdateComplianceFilingPayload {
  title?: string;
  description?: string;
  dueDate?: string;
}

export interface ConflictDeclarationSummary {
  id: string;
  userId: string;
  userName: string;
  hasConflict: boolean;
  details: string | null;
  declaredAt: string;
  declaredByName: string;
}

export interface CreateConflictDeclarationPayload {
  userId?: string;
  hasConflict: boolean;
  details?: string;
}
