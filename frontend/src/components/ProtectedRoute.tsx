import type { ReactNode } from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

export function ProtectedRoute({
  children,
  requireAdmin = false,
  requireManagement = false,
}: {
  children: ReactNode;
  requireAdmin?: boolean;
  requireManagement?: boolean;
}) {
  const { user, loading } = useAuth();

  if (loading) {
    return <div className="page-status">Loading...</div>;
  }
  if (!user) {
    return <Navigate to="/login" replace />;
  }
  const isAdmin = user.role === "ADMIN";
  const canManage = isAdmin || user.role === "COMPANY_SECRETARY";
  if (requireAdmin && !isAdmin) {
    return <Navigate to="/dashboard" replace />;
  }
  if (requireManagement && !canManage) {
    return <Navigate to="/dashboard" replace />;
  }
  return <>{children}</>;
}
