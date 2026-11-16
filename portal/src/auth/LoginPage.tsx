import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { apiErrorMessage } from '../api/client';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
  const [showPass, setShowPass] = useState(false);
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  async function onSubmit(e: React.FormEvent) {
    e.preventDefault();
    setError('');
    setLoading(true);
    try {
      await login(username, password);
      navigate('/');
    } catch (err) {
      setError(apiErrorMessage(err));
    } finally {
      setLoading(false);
    }
  }

  return (
    <div className="login-wrap login-wrap--pastel">
      <form className="lp-card" onSubmit={onSubmit}>
        <div className="lp-head">Welcome back</div>
        <div className="lp-sub">Sign in to the Operations Portal</div>
        {error && <div className="lp-error">{error}</div>}

        <label className="lp-label">Username</label>
        <div className="lp-field">
          <svg className="lp-ic" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 12a5 5 0 1 0-5-5 5 5 0 0 0 5 5Zm0 2c-4.42 0-8 2.24-8 5v1h16v-1c0-2.76-3.58-5-8-5Z" /></svg>
          <input value={username} onChange={(e) => setUsername(e.target.value)} placeholder="Enter your username" autoFocus autoComplete="username" />
        </div>

        <label className="lp-label">Password</label>
        <div className="lp-field">
          <svg className="lp-ic" viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M17 9V7a5 5 0 0 0-10 0v2a2 2 0 0 0-2 2v8a2 2 0 0 0 2 2h10a2 2 0 0 0 2-2v-8a2 2 0 0 0-2-2ZM9 7a3 3 0 0 1 6 0v2H9Zm3 10a2 2 0 1 1 2-2 2 2 0 0 1-2 2Z" /></svg>
          <input type={showPass ? 'text' : 'password'} value={password} onChange={(e) => setPassword(e.target.value)} placeholder="Enter your password" autoComplete="current-password" />
          <button type="button" className="lp-eye" onClick={() => setShowPass((s) => !s)} aria-label={showPass ? 'Hide password' : 'Show password'}>
            <svg viewBox="0 0 24 24" aria-hidden="true"><path fill="currentColor" d="M12 5c-5 0-9.27 3.11-11 7 1.73 3.89 6 7 11 7s9.27-3.11 11-7c-1.73-3.89-6-7-11-7Zm0 12a5 5 0 1 1 5-5 5 5 0 0 1-5 5Zm0-8a3 3 0 1 0 3 3 3 3 0 0 0-3-3Z" /></svg>
          </button>
        </div>

        <button className="lp-submit" disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}{!loading && <span aria-hidden="true"> →</span>}
        </button>
      </form>
    </div>
  );
}
