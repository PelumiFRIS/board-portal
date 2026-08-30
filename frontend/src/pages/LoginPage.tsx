import { useState, type FormEvent } from "react";
import { Link, useNavigate } from "react-router-dom";
import { extractErrorMessage, SESSION_EXPIRED_KEY } from "../api/client";
import { AuthHero } from "../components/AuthHero";
import { useAuth } from "../context/AuthContext";

export function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);
  const [submitting, setSubmitting] = useState(false);
  const [sessionExpired] = useState(() => {
    const expired = sessionStorage.getItem(SESSION_EXPIRED_KEY) === "1";
    if (expired) sessionStorage.removeItem(SESSION_EXPIRED_KEY);
    return expired;
  });

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setError(null);
    setSubmitting(true);
    try {
      await login({ email, password });
      navigate("/dashboard");
    } catch (err) {
      setError(extractErrorMessage(err));
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="auth-shell">
      <AuthHero />
      <div className="auth-page">
        <form className="auth-card" onSubmit={handleSubmit}>
          <img src="/logo.png" alt="FirstRegistrars" className="auth-logo" />
          <h1>Sign in</h1>
          <p className="auth-subtitle">Welcome back to the Board Portal.</p>

          {sessionExpired && (
            <p className="session-notice">Your session has expired. Please sign in again.</p>
          )}

          <label>
            Email
            <input type="email" value={email} onChange={(e) => setEmail(e.target.value)} required />
          </label>
          <label>
            Password
            <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
          </label>

          {error && <p className="form-error">{error}</p>}

          <button type="submit" disabled={submitting}>
            {submitting ? "Signing in..." : "Sign in"}
          </button>

          <p className="auth-footer">
            Setting up a new organization? <Link to="/signup">Create one</Link>
          </p>
        </form>
      </div>
    </div>
  );
}
