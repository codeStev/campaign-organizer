import { FormEvent, useState } from 'react';
import { login, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

interface Props {
  onLoggedIn: () => void;
  onRegister: () => void;
}

export function LoginPage({ onLoggedIn, onRegister }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      await login(email, password);
      onLoggedIn();
    } catch (err) {
      setError(err instanceof ApiError && err.status === 401 ? 'Invalid email or password' : 'Login failed');
    } finally {
      setBusy(false);
    }
  }

  return (
    <form className="card login" onSubmit={handleSubmit}>
      <h2>Sign in</h2>
      <label htmlFor="email">Email</label>
      <Input
        id="email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        autoFocus
        data-testid="login-email"
      />
      <label htmlFor="password">Password</label>
      <Input
        id="password"
        type="password"
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        data-testid="login-password"
      />
      {error && <p className="error">{error}</p>}
      <Button
        type="submit"
        disabled={busy || email.length === 0 || password.length === 0}
        data-testid="login-submit"
      >
        {busy ? 'Signing in…' : 'Sign in'}
      </Button>
      <Button type="button" variant="link" onClick={onRegister}>
        Create an account
      </Button>
    </form>
  );
}
