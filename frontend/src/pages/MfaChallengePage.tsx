import { FormEvent, useState } from 'react';
import { MfaMethod, verifyRecoveryCode, verifyTotpChallenge } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

interface Props {
  /** PASSWORD-only token from login, not a real session token. */
  pendingToken: string;
  method: MfaMethod;
  onAuthenticated: (fullToken: string) => void;
  /** A consumed recovery code resets the account's MFA method — routes back into setup. */
  onNeedsSetup: (pendingToken: string) => void;
}

export function MfaChallengePage({ pendingToken, method, onAuthenticated, onNeedsSetup }: Props) {
  const [code, setCode] = useState('');
  const [recoveryCode, setRecoveryCode] = useState('');
  const [recoveryMode, setRecoveryMode] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  async function handleVerifyCode(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await verifyTotpChallenge(pendingToken, code);
      onAuthenticated(result.token);
    } catch {
      setError('Invalid code');
    } finally {
      setBusy(false);
    }
  }

  async function handleVerifyRecoveryCode(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    try {
      const result = await verifyRecoveryCode(pendingToken, recoveryCode);
      onNeedsSetup(result.token);
    } catch {
      setError('Invalid recovery code');
    } finally {
      setBusy(false);
    }
  }

  if (method !== 'TOTP') {
    return (
      <div className="card login">
        <p>This authentication method isn't supported yet.</p>
      </div>
    );
  }

  if (recoveryMode) {
    return (
      <form className="card login" onSubmit={handleVerifyRecoveryCode}>
        <h2>Use a recovery code</h2>
        <p className="muted hint">
          This will sign you in but immediately require setting up a new authenticator — treat
          your old one as compromised or lost.
        </p>
        <label htmlFor="recovery-code">Recovery code</label>
        <Input
          id="recovery-code"
          value={recoveryCode}
          onChange={(e) => setRecoveryCode(e.target.value)}
          autoFocus
          data-testid="recovery-code"
        />
        {error && <p className="error">{error}</p>}
        <Button type="submit" disabled={busy || recoveryCode.length === 0}>
          {busy ? 'Verifying…' : 'Continue'}
        </Button>
        <Button
          type="button"
          variant="link"
          onClick={() => {
            setRecoveryMode(false);
            setError(null);
          }}
        >
          Back to authenticator code
        </Button>
      </form>
    );
  }

  return (
    <form className="card login" onSubmit={handleVerifyCode}>
      <h2>Enter your authenticator code</h2>
      <label htmlFor="totp-code">6-digit code</label>
      <Input
        id="totp-code"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        autoFocus
        inputMode="numeric"
        maxLength={6}
        data-testid="mfa-challenge-code"
      />
      {error && <p className="error">{error}</p>}
      <Button type="submit" disabled={busy || code.length === 0} data-testid="mfa-challenge-submit">
        {busy ? 'Verifying…' : 'Continue'}
      </Button>
      <Button
        type="button"
        variant="link"
        onClick={() => {
          setRecoveryMode(true);
          setError(null);
        }}
      >
        Use a recovery code instead
      </Button>
    </form>
  );
}
