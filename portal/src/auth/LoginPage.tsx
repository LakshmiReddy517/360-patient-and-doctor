import { useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from './AuthContext';
import { apiErrorMessage } from '../api/client';

export default function LoginPage() {
  const { login } = useAuth();
  const navigate = useNavigate();
  const [username, setUsername] = useState('admin');
  const [password, setPassword] = useState('admin123');
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
    <div className="login-wrap">
      <form className="login-card" onSubmit={onSubmit}>
        <h2>360° Patient Care</h2>
        <p className="sub">Command Centre — sign in to continue</p>
        {error && <div className="error-text">{error}</div>}
        <div className="field">
          <label>Username</label>
          <input className="input" value={username} onChange={(e) => setUsername(e.target.value)} autoFocus />
        </div>
        <div className="field">
          <label>Password</label>
          <input className="input" type="password" value={password} onChange={(e) => setPassword(e.target.value)} />
        </div>
        <button className="btn btn-primary" style={{ width: '100%', justifyContent: 'center' }} disabled={loading}>
          {loading ? 'Signing in…' : 'Sign in'}
        </button>
        <div className="demo-hint">
          <strong>Demo accounts</strong><br />
          admin / admin123 &nbsp;·&nbsp; finance / finance123<br />
          agent / agent123 &nbsp;·&nbsp; patient / patient123
        </div>
        <div style={{ textAlign: 'center', marginTop: 16, fontSize: 11, color: 'var(--text-muted)' }}>
          Developed by <strong>pvalr</strong>
        </div>
      </form>
    </div>
  );
}
