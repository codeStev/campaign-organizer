import { FormEvent, useState } from 'react';
import { recoverPassword } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

interface Props {
  onBackToLogin: () => void;
}

/**
 * Identical response whether or not the email/recovery-code pair was valid (anti-enumeration,
 * mirrors RegisterPage's handling of ADR-0110's registration contract) — this form never
 * branches UI on "did it really work".
 */
export function RecoverPasswordPage({ onBackToLogin }: Props) {
  const [email, setEmail] = useState('');
  const [recoveryCode, setRecoveryCode] = useState('');
  const [newPassword, setNewPassword] = useState('');
  const [done, setDone] = useState(false);
  const [busy, setBusy] = useState(false);

  async function handleSubmit(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    try {
      await recoverPassword(email, recoveryCode, newPassword);
      setDone(true);
    } finally {
      setBusy(false);
    }
  }

  if (done) {
    return (
      <div className="card login">
        <h2>Request processed</h2>
        <p>If that email and recovery code matched, your password has been changed. Log in to continue.</p>
        <Button type="button" onClick={onBackToLogin}>
          Go to sign in
        </Button>
      </div>
    );
  }

  return (
    <form className="card login" onSubmit={handleSubmit}>
      <h2>Reset your password</h2>
      <label htmlFor="recover-email">Email</label>
      <Input
        id="recover-email"
        type="email"
        value={email}
        onChange={(e) => setEmail(e.target.value)}
        autoFocus
        data-testid="recover-email"
      />
      <label htmlFor="recover-code">Recovery code</label>
      <Input
        id="recover-code"
        value={recoveryCode}
        onChange={(e) => setRecoveryCode(e.target.value)}
        data-testid="recover-code"
      />
      <label htmlFor="recover-new-password">New password</label>
      <Input
        id="recover-new-password"
        type="password"
        minLength={12}
        value={newPassword}
        onChange={(e) => setNewPassword(e.target.value)}
        data-testid="recover-new-password"
      />
      <p className="muted hint">At least 12 characters.</p>
      <Button
        type="submit"
        disabled={busy || email.length === 0 || recoveryCode.length === 0 || newPassword.length < 12}
        data-testid="recover-submit"
      >
        {busy ? 'Resetting…' : 'Reset password'}
      </Button>
      <Button type="button" variant="link" onClick={onBackToLogin}>
        Back to sign in
      </Button>
    </form>
  );
}
