import { Navigate, Route, Routes } from "react-router-dom";
import { AuthProvider } from "./context/AuthContext";
import { ProtectedRoute } from "./components/ProtectedRoute";
import { LoginPage } from "./pages/LoginPage";
import { SignupPage } from "./pages/SignupPage";
import { DashboardPage } from "./pages/DashboardPage";
import { MeetingsListPage } from "./pages/MeetingsListPage";
import { MeetingDetailPage } from "./pages/MeetingDetailPage";
import { DocumentsListPage } from "./pages/DocumentsListPage";
import { AuditLogPage } from "./pages/AuditLogPage";
import { DirectoryPage } from "./pages/DirectoryPage";
import { CommitteesPage } from "./pages/CommitteesPage";
import { CompliancePage } from "./pages/CompliancePage";
import { ConflictOfInterestPage } from "./pages/ConflictOfInterestPage";

function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route path="/login" element={<LoginPage />} />
        <Route path="/signup" element={<SignupPage />} />
        <Route
          path="/dashboard"
          element={
            <ProtectedRoute>
              <DashboardPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/meetings"
          element={
            <ProtectedRoute>
              <MeetingsListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/meetings/:id"
          element={
            <ProtectedRoute>
              <MeetingDetailPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/documents"
          element={
            <ProtectedRoute>
              <DocumentsListPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/audit"
          element={
            <ProtectedRoute>
              <AuditLogPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/directory"
          element={
            <ProtectedRoute>
              <DirectoryPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/committees"
          element={
            <ProtectedRoute>
              <CommitteesPage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/compliance"
          element={
            <ProtectedRoute>
              <CompliancePage />
            </ProtectedRoute>
          }
        />
        <Route
          path="/conflicts"
          element={
            <ProtectedRoute>
              <ConflictOfInterestPage />
            </ProtectedRoute>
          }
        />
        <Route path="*" element={<Navigate to="/dashboard" replace />} />
      </Routes>
    </AuthProvider>
  );
}

export default App;
