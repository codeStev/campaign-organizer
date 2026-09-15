import { useEffect, useState } from 'react';
import { toast } from 'sonner';
import { accountSessionsApi, ApiError, AccountSessionSummary } from '../api/client';
import { Button } from '../components/ui/button';
import { ConfirmDialog } from '../components/ConfirmDialog';

interface Props {
  onAuthExpired: () => void;
}

/**
 * Self-service session/device tracking (ADR-0112) — a second, finer-grained revocation layer
 * alongside "log out everywhere". Revoking the caller's own current session immediately makes
 * its own token unusable, so that case routes through {@code onAuthExpired} instead of a plain
 * list refresh.
 */
export function SessionsPanel({ onAuthExpired }: Props) {
  const [sessions, setSessions] = useState<AccountSessionSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [revoking, setRevoking] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [confirmRevokeId, setConfirmRevokeId] = useState<string | null>(null);

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refresh() {
    setLoading(true);
    try {
      setSessions(await accountSessionsApi.list());
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

  async function handleRevoke(session: AccountSessionSummary) {
    setRevoking(session.id);
    setError(null);
    try {
      await accountSessionsApi.revoke(session.id);
      if (session.current) {
        // Our own token is now unusable — the next request would just 401 anyway.
        onAuthExpired();
        return;
      }
      toast.success('Session revoked');
      await refresh();
    } catch (err) {
      handleError(err);
    } finally {
      setRevoking(null);
    }
  }

  return (
    <div>
      {error && <p className="error">{error}</p>}
      {loading ? (
        <p className="muted">Loading…</p>
      ) : sessions.length === 0 ? (
        <p className="muted">No active sessions.</p>
      ) : (
        <ul className="settings-list">
          {sessions.map((session) => (
            <li key={session.id} className="settings-list-row">
              <div>
                <strong>{session.userAgent || 'Unknown device'}</strong>
                {session.current && ' · This device'}
                <p className="muted">
                  {session.ipAddress ? `${session.ipAddress} · ` : ''}
                  Signed in {new Date(session.createdAt).toLocaleString()}
                </p>
              </div>
              <Button
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => setConfirmRevokeId(session.id)}
                disabled={revoking === session.id}
              >
                {revoking === session.id ? 'Revoking…' : 'Revoke'}
              </Button>
            </li>
          ))}
        </ul>
      )}

      <ConfirmDialog
        open={confirmRevokeId !== null}
        onOpenChange={(open) => !open && setConfirmRevokeId(null)}
        title="Revoke this session?"
        description="That device will be signed out immediately."
        confirmLabel="Revoke"
        destructive
        onConfirm={() => {
          const session = sessions.find((s) => s.id === confirmRevokeId);
          setConfirmRevokeId(null);
          if (session) void handleRevoke(session);
        }}
      />
    </div>
  );
}
