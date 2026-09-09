import { render, screen } from "@testing-library/react";
import { MemoryRouter, Route, Routes } from "react-router-dom";
import { describe, expect, it, vi } from "vitest";
import { useAuth } from "../context/AuthContext";
import type { Role, UserSummary } from "../api/types";
import { ProtectedRoute } from "./ProtectedRoute";

vi.mock("../context/AuthContext", () => ({
  useAuth: vi.fn(),
}));

const mockedUseAuth = vi.mocked(useAuth);

function baseUser(role: Role): UserSummary {
  return {
    id: "user-1",
    firstName: "Ada",
    lastName: "Test",
    email: "ada@uitest.local",
    role,
    status: "ACTIVE",
    organizationId: "org-1",
    organizationName: "Local UI Test Org",
    title: null,
    phone: null,
    bio: null,
    committees: [],
    photoUpdatedAt: null,
  };
}

function renderAt(path: string, ui: React.ReactNode) {
  return render(
    <MemoryRouter initialEntries={[path]}>
      <Routes>
        <Route path={path} element={ui} />
        <Route path="/login" element={<div>Login page</div>} />
        <Route path="/dashboard" element={<div>Dashboard page</div>} />
      </Routes>
    </MemoryRouter>,
  );
}

describe("ProtectedRoute", () => {
  it("shows a loading state while auth is resolving", () => {
    mockedUseAuth.mockReturnValue({ user: null, loading: true } as ReturnType<typeof useAuth>);
    renderAt("/audit", <ProtectedRoute>secret</ProtectedRoute>);
    expect(screen.getByText("Loading...")).toBeInTheDocument();
  });

  it("redirects to /login when there is no user", () => {
    mockedUseAuth.mockReturnValue({ user: null, loading: false } as ReturnType<typeof useAuth>);
    renderAt("/audit", <ProtectedRoute>secret</ProtectedRoute>);
    expect(screen.getByText("Login page")).toBeInTheDocument();
  });

  it("redirects a non-admin away from an admin-only route instead of rendering it", () => {
    mockedUseAuth.mockReturnValue({ user: baseUser("BOARD_MEMBER"), loading: false } as ReturnType<typeof useAuth>);
    renderAt("/integrations", <ProtectedRoute requireAdmin>Integrations</ProtectedRoute>);
    expect(screen.getByText("Dashboard page")).toBeInTheDocument();
    expect(screen.queryByText("Integrations")).toBeNull();
  });

  it("renders the route for an admin hitting an admin-only route", () => {
    mockedUseAuth.mockReturnValue({ user: baseUser("ADMIN"), loading: false } as ReturnType<typeof useAuth>);
    renderAt("/integrations", <ProtectedRoute requireAdmin>Integrations</ProtectedRoute>);
    expect(screen.getByText("Integrations")).toBeInTheDocument();
  });

  it("allows an executive into a management-only route", () => {
    mockedUseAuth.mockReturnValue({ user: baseUser("EXECUTIVE"), loading: false } as ReturnType<typeof useAuth>);
    renderAt("/audit", <ProtectedRoute requireManagement>Audit trail</ProtectedRoute>);
    expect(screen.getByText("Audit trail")).toBeInTheDocument();
  });

  it("redirects a board member away from a management-only route", () => {
    mockedUseAuth.mockReturnValue({ user: baseUser("BOARD_MEMBER"), loading: false } as ReturnType<typeof useAuth>);
    renderAt("/audit", <ProtectedRoute requireManagement>Audit trail</ProtectedRoute>);
    expect(screen.getByText("Dashboard page")).toBeInTheDocument();
  });
});
