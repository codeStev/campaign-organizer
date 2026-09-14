import { FormEvent, useState } from 'react';
import { registerAccount } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

interface Props {
  onBackToLogin: () => void;
}

/**
 * The backend deliberately never says whether an email was already taken
 * (ADR-0110, anti-enumeration) — so this form always shows the same message
 * on submit and never branches UI on "did it really work".
 */
export function RegisterPage({ onBackToLogin }: Props) {
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [message, setMessage] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      const accepted = await registerAccount(email, password);
      setMessage(accepted);
    } finally {
      setBusy(false);
    }
  }

  if (message) {
    return (
      <div className="card login">
        <h2>Check your account</h2>
        <p>{message}</p>
        <Button type="button" onClick={onBackToLogin}>
          Go to sign in
        </Button>
      </div>
    );
  }

  return (
    <form className="card login" onSubmit={handleSubmit}>
      <h2>Create an account</h2>
      <label htmlFor="register-email">Email</label>
      <Input
        id="register-email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        autoFocus
        data-testid="register-email"
      />
      <label htmlFor="register-password">Password</label>
      <Input
        id="register-password"
        type="password"
        minLength={12}
        value={password}
        onChange={(e) => setPassword(e.target.value)}
        data-testid="register-password"
      />
      <p className="muted hint">At least 12 characters.</p>
      <Button
        type="submit"
        disabled={busy || email.length === 0 || password.length < 12}
        data-testid="register-submit"
      >
        {busy ? 'Creating…' : 'Create account'}
      </Button>
      <Button type="button" variant="link" onClick={onBackToLogin}>
        Back to sign in
      </Button>
    </form>
  );
}
