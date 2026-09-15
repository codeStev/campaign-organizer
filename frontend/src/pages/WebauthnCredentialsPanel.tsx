import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { addWebauthnCredential, webauthnCredentialsApi, ApiError, WebAuthnCredentialSummary } from '../api/client';
import { Button } from '../components/ui/button';
import { ConfirmDialog } from '../components/ConfirmDialog';

interface Props {
  onAuthExpired: () => void;
}

/**
 * Self-service passkey management (ADR-0111 follow-up) — list/add/remove, mirroring
 * AiSettingsPanel's card-content shape. The backend refuses to remove an account's last
 * remaining credential while WebAuthn is its active MFA method (400), surfaced here as a
 * plain error rather than special-cased, since the message is already self-explanatory.
 */
export function WebauthnCredentialsPanel({ onAuthExpired }: Props) {
  const [credentials, setCredentials] = useState<WebAuthnCredentialSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [adding, setAdding] = useState(false);
  const [removing, setRemoving] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmRemoveId, setConfirmRemoveId] = useState<string | null>(null);

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refresh() {
    setLoading(true);
    try {
      setCredentials(await webauthnCredentialsApi.list());
    } catch (err) {
      handleError(err);
    } finally {
      setLoading(false);
    }
  }

  function handleError(err: unknown) {
    if (err instanceof ApiError && err.status === 401) {
      onAuthExpired();
      return;
    }
    setError(err instanceof ApiError ? err.message : 'Something went wrong');
  }

  async function handleAdd() {
    setAdding(true);
    setError(null);
    try {
      await addWebauthnCredential();
      toast.success('Passkey added');
      await refresh();
    } catch (err) {
      handleError(err);
    } finally {
      setAdding(false);
    }
  }

  async function handleRemove(id: string) {
    setRemoving(id);
    setError(null);
    try {
      await webauthnCredentialsApi.remove(id);
      toast.success('Passkey removed');
      await refresh();
    } catch (err) {
      handleError(err);
    } finally {
      setRemoving(null);
    }
  }

  return (
    <div>
      {error && <p className="error">{error}</p>}
      {loading ? (
        <p className="muted">Loading…</p>
      ) : credentials.length === 0 ? (
        <p className="muted">No passkeys registered yet.</p>
      ) : (
        <ul className="settings-list">
          {credentials.map((credential) => (
            <li key={credential.id} className="settings-list-row">
              <div>
                <strong>{credential.label || 'Passkey'}</strong>
                <p className="muted">
                  Added {new Date(credential.createdAt).toLocaleDateString()} · last used{' '}
                  {new Date(credential.lastUsedAt).toLocaleDateString()}
                </p>
              </div>
              <Button
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => setConfirmRemoveId(credential.id)}
                disabled={removing === credential.id}
              >
                {removing === credential.id ? 'Removing…' : 'Remove'}
              </Button>
            </li>
          ))}
        </ul>
      )}
      <Button variant="outline" onClick={() => void handleAdd()} disabled={adding}>
        {adding ? 'Follow your browser’s prompt…' : '+ Add a passkey'}
      </Button>

      <ConfirmDialog
        open={confirmRemoveId !== null}
        onOpenChange={(open) => !open && setConfirmRemoveId(null)}
        title="Remove this passkey?"
        description="You'll need a different passkey or your recovery codes to sign in without it."
        confirmLabel="Remove"
        destructive
        onConfirm={() => {
          const id = confirmRemoveId;
          setConfirmRemoveId(null);
          if (id) void handleRemove(id);
        }}
      />
    </div>
  );
}
