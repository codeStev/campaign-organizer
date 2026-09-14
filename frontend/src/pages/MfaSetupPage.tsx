import { FormEvent, useEffect, useState } from 'react';
import { confirmTotpSetup, startTotpSetup } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

interface Props {
  /** PASSWORD-only token from login/register, not a real session token. */
  pendingToken: string;
  onAuthenticated: (fullToken: string) => void;
}

type Stage =
  | { kind: 'loading' }
  | { kind: 'error' }
  | { kind: 'enter-code'; secret: string; provisioningUri: string; qrCodeDataUri: string }
  | { kind: 'show-recovery-codes'; token: string; recoveryCodes: string[] };

/**
 * TOTP is the only enrollment method offered today (WebAuthn is a planned follow-up, ADR-0111)
 * — no method-choice step yet, setup starts immediately.
 */
export function MfaSetupPage({ pendingToken, onAuthenticated }: Props) {
  const [stage, setStage] = useState<Stage>({ kind: 'loading' });
  const [code, setCode] = useState('');
  const [confirmError, setConfirmError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);
  const [acknowledged, setAcknowledged] = useState(false);

  useEffect(() => {
    startTotpSetup(pendingToken)
      .then((start) => setStage({ kind: 'enter-code', ...start }))
      .catch(() => setStage({ kind: 'error' }));
  }, [pendingToken]);

  async function handleConfirm(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setConfirmError(null);
    try {
      const result = await confirmTotpSetup(pendingToken, code);
      setStage({ kind: 'show-recovery-codes', token: result.token, recoveryCodes: result.recoveryCodes });
    } catch {
      setConfirmError('Invalid code — check your authenticator app and try again');
    } finally {
      setBusy(false);
    }
  }

  if (stage.kind === 'loading') {
    return <p className="muted">Setting up two-factor authentication…</p>;
  }

  if (stage.kind === 'error') {
    return <p className="error">Couldn't start two-factor setup. Please refresh and try again.</p>;
  }

  if (stage.kind === 'show-recovery-codes') {
    return (
      <div className="card login">
        <h2>Save your recovery codes</h2>
        <p className="muted hint">
          Each code works once, to sign in if you lose your authenticator or to reset a forgotten
          password. Store them somewhere safe — they won't be shown again.
        </p>
        <ul className="recovery-codes">
          {stage.recoveryCodes.map((recoveryCode) => (
            <li key={recoveryCode}>
              <code>{recoveryCode}</code>
            </li>
          ))}
        </ul>
        <label htmlFor="ack-recovery-codes" className="checkbox-label">
          <input
            id="ack-recovery-codes"
            type="checkbox"
            checked={acknowledged}
            onChange={(e) => setAcknowledged(e.target.checked)}
          />
          I've saved these recovery codes
        </label>
        <Button type="button" disabled={!acknowledged} onClick={() => onAuthenticated(stage.token)}>
          Continue
        </Button>
      </div>
    );
  }

  return (
    <form className="card login" onSubmit={handleConfirm}>
      <h2>Set up two-factor authentication</h2>
      <p className="muted hint">
        Scan this QR code with an authenticator app (Google Authenticator, 1Password, Authy, ...).
      </p>
      <img src={stage.qrCodeDataUri} alt="Authenticator app QR code" width={200} height={200} />
      <p className="muted hint">
        Or enter this code manually: <code>{stage.secret}</code>
      </p>
      <label htmlFor="setup-code">6-digit code</label>
      <Input
        id="setup-code"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        autoFocus
        inputMode="numeric"
        maxLength={6}
        data-testid="mfa-setup-code"
      />
      {confirmError && <p className="error">{confirmError}</p>}
      <Button type="submit" disabled={busy || code.length === 0} data-testid="mfa-setup-confirm">
        {busy ? 'Confirming…' : 'Confirm'}
      </Button>
    </form>
  );
}
