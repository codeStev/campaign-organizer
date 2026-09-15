import { FormEvent, useState } from 'react';
import { toast } from 'sonner';
import { confirmTotpReEnrollment, startTotpReEnrollment, setToken, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

type Stage =
  | { kind: 'idle' }
  | { kind: 'loading' }
  | { kind: 'error' }
  | { kind: 'enter-code'; secret: string; provisioningUri: string; qrCodeDataUri: string };

/**
 * Self-service TOTP secret replacement for an account already on TOTP (ADR-0111 follow-up,
 * e.g. a new phone) — no method choice, no recovery-codes display (unlike MfaSetupPage's
 * enrollment flow), since neither applies to a replacement.
 */
export function TotpReEnrollmentPanel() {
  const [stage, setStage] = useState<Stage>({ kind: 'idle' });
  const [code, setCode] = useState('');
  const [confirmError, setConfirmError] = useState<string | null>(null);
  const [busy, setBusy] = useState(false);

  function start() {
    setStage({ kind: 'loading' });
    startTotpReEnrollment()
      .then((result) => setStage({ kind: 'enter-code', ...result }))
      .catch(() => setStage({ kind: 'error' }));
  }

  async function handleConfirm(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setConfirmError(null);
    try {
      const result = await confirmTotpReEnrollment(code);
      // The swap bumps tokenVersion server-side to invalidate any other outstanding token for
      // this account — must adopt the freshly-issued one or our own session goes stale next call.
      setToken(result.token);
      toast.success('Authenticator app replaced');
      setStage({ kind: 'idle' });
      setCode('');
    } catch (err) {
      setConfirmError(
        err instanceof ApiError && err.status === 400
          ? 'Invalid code — check your authenticator app and try again'
          : 'Something went wrong',
      );
    } finally {
      setBusy(false);
    }
  }

  if (stage.kind === 'idle' || stage.kind === 'error') {
    return (
      <div>
        {stage.kind === 'error' && <p className="error">Couldn't start replacement. Please try again.</p>}
        <p className="muted">Lost your authenticator or got a new phone? Replace it here.</p>
        <Button variant="outline" onClick={start}>
          Replace authenticator app
        </Button>
      </div>
    );
  }

  if (stage.kind === 'loading') {
    return <p className="muted">Setting up…</p>;
  }

  return (
    <form onSubmit={handleConfirm}>
      <p className="muted hint">
        Scan this QR code with your authenticator app (Google Authenticator, 1Password, Authy, ...).
        Your old code will stop working once this is confirmed.
      </p>
      <img src={stage.qrCodeDataUri} alt="Authenticator app QR code" width={200} height={200} />
      <p className="muted hint">
        Or enter this code manually: <code>{stage.secret}</code>
      </p>
      <label htmlFor="totp-reenroll-code">6-digit code</label>
      <Input
        id="totp-reenroll-code"
        value={code}
        onChange={(e) => setCode(e.target.value)}
        autoFocus
        inputMode="numeric"
        maxLength={6}
      />
      {confirmError && <p className="error">{confirmError}</p>}
      <Button type="submit" disabled={busy || code.length === 0}>
        {busy ? 'Confirming…' : 'Confirm'}
      </Button>
      <Button type="button" variant="link" onClick={() => setStage({ kind: 'idle' })} disabled={busy}>
        Cancel
      </Button>
    </form>
  );
}
