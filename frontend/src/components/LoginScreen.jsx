import { useState } from 'react';
import BrandMark from './BrandMark';
import { login } from '../api/client';

// The 3 seeded demo investors (Phase 4) — shown as hint chips since there's no
// self-registration. Clicking one fills the real email address rather than bypassing
// the lookup, so the login still genuinely exercises the backend's email resolution.
const DEMO_ACCOUNTS = [
  { label: 'Thabo', email: 'thabo.nkosi@example.com' },
  { label: 'Grace', email: 'grace.vandermerwe@example.com' },
  { label: 'Sipho', email: 'sipho.dlamini@example.com' },
];

export default function LoginScreen({ onLoginSuccess }) {
  const [email, setEmail] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState(null);

  async function handleSubmit(event) {
    event.preventDefault();
    setSubmitting(true);
    setError(null);

    try {
      const response = await login(email.trim());
      onLoginSuccess({ ...response, email: email.trim() });
    } catch (err) {
      setError(
        err.status === 404 ? 'No account found for that email.' : err.message,
      );
    } finally {
      setSubmitting(false);
    }
  }

  return (
    <div className="login-screen">
      <form className="login-card" onSubmit={handleSubmit} noValidate>
        <div className="login-brand">
          <BrandMark size={22} />
          <h1>Enviro365</h1>
        </div>
        <p className="subtitle">Withdrawal notice portal</p>

        {error && (
          <p className="error-banner" role="alert">
            {error}
          </p>
        )}

        <div className="form-field">
          <label htmlFor="login-email">Email</label>
          <input
            id="login-email"
            type="email"
            required
            placeholder="you@example.com"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
          />
        </div>

        <button type="submit" disabled={submitting || !email}>
          {submitting ? 'Logging in...' : 'Log in'}
        </button>

        <div className="demo-accounts">
          <p className="muted">Try a demo account</p>
          <div className="demo-chip-row">
            {DEMO_ACCOUNTS.map((account) => (
              <button
                key={account.email}
                type="button"
                className="demo-chip"
                onClick={() => setEmail(account.email)}
              >
                {account.label}
              </button>
            ))}
          </div>
        </div>
      </form>
    </div>
  );
}
