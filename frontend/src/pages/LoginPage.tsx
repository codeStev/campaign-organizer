import { FormEvent, useState } from 'react';
import { login, ApiError } from '../api/client';
import { Button } from '../components/ui/button';

interface Props {
  onLoggedIn: () => void;
}

export function LoginPage({ onLoggedIn }: Props) {
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(password);
      onLoggedIn();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401 ? 'Wrong password' : 'Login failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card login" onSubmit={handleSubmit}>
      <h2>Sign in</h2>
      <label htmlFor="password">Password</label>
      <input
        id="password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        autoFocus
      />
      {error && <p className="error">{error}</p>}
      <Button type="submit" disabled={busy || password.length === 0}>
        {busy ? 'Signing in…' : 'Sign in'}
      </Button>
    </form>
  );
}
