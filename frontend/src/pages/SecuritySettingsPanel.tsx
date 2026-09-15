import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { getCurrentAccount, recoveryCodesApi, Account, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { ConfirmDialog } from '../components/ConfirmDialog';
import { RecoveryCodesList } from '../components/RecoveryCodesList';
import { WebauthnCredentialsPanel } from './WebauthnCredentialsPanel';
import { TotpReEnrollmentPanel } from './TotpReEnrollmentPanel';

interface Props {
  onAuthExpired: () => void;
}

/**
 * The Settings page's "Security" card content (ADR-0111 follow-up). Recovery codes are
 * method-agnostic and always shown; WebAuthn credential management and TOTP re-enrollment are
 * mutually exclusive per account (each requires the *other* method not be active, mirroring
 * "one MFA method, chosen once") — fetches the account once to decide which to render, rather
 * than each sub-panel guessing or both attempting and failing.
 */
export function SecuritySettingsPanel({ onAuthExpired }: Props) {
  const [account, setAccount] = useState<Account | null>(null);
  const [remaining, setRemaining] = useState<number | null>(null);
  const [newCodes, setNewCodes] = useState<string[] | null>(null);
  const [regenerating, setRegenerating] = useState(false);
  const [confirmRegenerateOpen, setConfirmRegenerateOpen] = useState(false);
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refresh() {
    try {
      const [accountResult, statusResult] = await Promise.all([getCurrentAccount(), recoveryCodesApi.status()]);
      setAccount(accountResult);
      setRemaining(statusResult.remaining);
    } catch (err) {
      handleError(err);
    }
  }

  function handleError(err: unknown) {
    if (err instanceof ApiError && err.status === 401) {
      onAuthExpired();
      return;
    }
    setError(err instanceof ApiError ? err.message : 'Something went wrong');
  }

  async function handleRegenerate() {
    setRegenerating(true);
    setError(null);
    try {
      const codes = await recoveryCodesApi.regenerate();
      setNewCodes(codes);
      setRemaining(codes.length);
    } catch (err) {
      handleError(err);
    } finally {
      setRegenerating(false);
    }
  }

  if (newCodes) {
    return (
      <div>
        <h3>Your new recovery codes</h3>
        <RecoveryCodesList
          codes={newCodes}
          continueLabel="Done"
          onContinue={() => {
            setNewCodes(null);
            toast.success('Recovery codes regenerated');
          }}
        />
      </div>
    );
  }

  return (
    <div>
      {error && <p className="error">{error}</p>}

      <div className="settings-list-row">
        <div>
          <strong>Recovery codes</strong>
          <p className="muted">{remaining === null ? 'Loading…' : `${remaining} unused codes remaining`}</p>
        </div>
        <Button variant="outline" onClick={() => setConfirmRegenerateOpen(true)} disabled={regenerating}>
          {regenerating ? 'Regenerating…' : 'Regenerate'}
        </Button>
      </div>

      {account?.mfaMethod === 'WEBAUTHN' && <WebauthnCredentialsPanel onAuthExpired={onAuthExpired} />}
      {account?.mfaMethod === 'TOTP' && <TotpReEnrollmentPanel />}

      <ConfirmDialog
        open={confirmRegenerateOpen}
        onOpenChange={setConfirmRegenerateOpen}
        title="Regenerate recovery codes?"
        description="Your existing recovery codes will stop working immediately. You'll get a fresh set of 10."
        confirmLabel="Regenerate"
        destructive
        onConfirm={() => {
          setConfirmRegenerateOpen(false);
          void handleRegenerate();
        }}
      />
    </div>
  );
}
