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
