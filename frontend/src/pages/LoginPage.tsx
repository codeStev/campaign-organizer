import { FormEvent, useState } from 'react';
import { login, ApiError, LoginResponse } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

interface Props {
  /** A correct password never grants full access on its own (ADR-0111) — the caller decides
   * whether to route into MFA setup or a challenge based on `result.status`. */
  onLoginResult: (result: LoginResponse) => void;
  onRegister: () => void;
  onForgotPassword: () => void;
}

export function LoginPage({ onLoginResult, onRegister, onForgotPassword }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await login(email, password);
      onLoginResult(result);
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
      <Button type="button" variant="link" onClick={onForgotPassword}>
        Forgot password?
      </Button>
    </form>
  );
}
