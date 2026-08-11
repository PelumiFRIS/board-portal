export type Role = "ADMIN" | "BOARD_MEMBER" | "EXECUTIVE";
export type UserStatus = "ACTIVE" | "DISABLED";

export interface UserSummary {
  id: string;
  firstName: string;
  lastName: string;
  email: string;
  role: Role;
  status: UserStatus;
  organizationId: string;
  organizationName: string;
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
  agendaItems: AgendaItem[];
  documents: DocumentSummary[];
}

export interface CreateMeetingPayload {
  title: string;
  description?: string;
  location?: string;
  scheduledStart: string;
  scheduledEnd?: string;
}

export interface UpdateMeetingPayload {
  title?: string;
  description?: string;
  location?: string;
  scheduledStart?: string;
  scheduledEnd?: string;
  status?: MeetingStatus;
  minutesContent?: string;
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
  createdAt: string;
}
