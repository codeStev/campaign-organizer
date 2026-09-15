import { FormEvent, useState } from 'react';
import { confirmTotpSetup, enrollWebauthn, startTotpSetup } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { RecoveryCodesList } from '../components/RecoveryCodesList';

interface Props {
  /** PASSWORD-only token from login/register, not a real session token. */
  pendingToken: string;
  onAuthenticated: (fullToken: string) => void;
}

type Stage =
  | { kind: 'choose-method' }
  | { kind: 'totp-loading' }
  | { kind: 'totp-error' }
  | { kind: 'totp-enter-code'; secret: string; provisioningUri: string; qrCodeDataUri: string }
  | { kind: 'webauthn-prompt' }
  | { kind: 'webauthn-error'; message: string }
  | { kind: 'show-recovery-codes'; token: string; recoveryCodes: string[] };

export function MfaSetupPage({ pendingToken, onAuthenticated }: Props) {
  const [stage, setStage] = useState<Stage>({ kind: 'choose-method' });
  const [code, setCode] = useState('');
  const [confirmError, setConfirmError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function chooseTotp() {
    setStage({ kind: 'totp-loading' });
    startTotpSetup(pendingToken)
      .then((start) => setStage({ kind: 'totp-enter-code', ...start }))
      .catch(() => setStage({ kind: 'totp-error' }));
  }

  async function chooseWebauthn() {
    setStage({ kind: 'webauthn-prompt' });
    try {
      const result = await enrollWebauthn(pendingToken);
      setStage({ kind: 'show-recovery-codes', token: result.token, recoveryCodes: result.recoveryCodes });
    } catch {
      setStage({
        kind: 'webauthn-error',
        message: "Couldn't create a passkey — the prompt may have been cancelled. Try again.",
      });
    }
  }

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

  if (stage.kind === 'choose-method') {
    return (
      <div className="card login">
        <h2>Set up two-factor authentication</h2>
        <p className="muted hint">Every account needs a second factor — choose how to verify it's you.</p>
        <Button type="button" onClick={chooseTotp} data-testid="mfa-choose-totp">
          Use an authenticator app
        </Button>
        <Button type="button" onClick={chooseWebauthn} data-testid="mfa-choose-webauthn">
          Use a passkey or security key
        </Button>
      </div>
    );
  }

  if (stage.kind === 'totp-loading') {
    return <p className="muted">Setting up two-factor authentication…</p>;
  }

  if (stage.kind === 'totp-error') {
    return <p className="error">Couldn't start two-factor setup. Please refresh and try again.</p>;
  }

  if (stage.kind === 'webauthn-prompt') {
    return <p className="muted">Follow your browser's prompt to create a passkey…</p>;
  }

  if (stage.kind === 'webauthn-error') {
    return (
      <div className="card login">
        <p className="error">{stage.message}</p>
        <Button type="button" onClick={chooseWebauthn}>
          Try again
        </Button>
        <Button type="button" onClick={() => setStage({ kind: 'choose-method' })}>
          Choose a different method
        </Button>
      </div>
    );
  }

  if (stage.kind === 'show-recovery-codes') {
    return (
      <div className="card login">
        <h2>Save your recovery codes</h2>
        <RecoveryCodesList codes={stage.recoveryCodes} onContinue={() => onAuthenticated(stage.token)} />
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
