import { useCallback, useEffect, useState } from 'react';
import { accountsApi, Account, Role, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { toast } from 'sonner';
import { ConfirmDeleteDialog } from '../components/ConfirmDeleteDialog';
import { Spinner } from '../components/ui/spinner';

interface Props {
  onAuthExpired: () => void;
}

/**
 * Admin-only account roster (ADR-0109) — list/role/enable/disable/reset-
 * password/delete. Never shows another account's worlds or content; ADMIN
 * only grants rights over this roster itself.
 */
export function AccountsPage({ onAuthExpired }: Props) {
  const [accounts, setAccounts] = useState<Account[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [resettingId, setResettingId] = useState<string | null>(null);
  const [newPassword, setNewPassword] = useState('');

  const onError = useCallback(
    (err: unknown) => {
      if (err instanceof ApiError && err.status === 401) return onAuthExpired();
      setError(err instanceof Error ? err.message : 'Something went wrong');
    },
    [onAuthExpired],
  );

  const refresh = useCallback(() => {
    accountsApi
      .list()
      .then(setAccounts)
      .catch(onError)
      .finally(() => setLoading(false));
  }, [onError]);

  useEffect(() => {
    refresh();
  }, [refresh]);

  async function changeRole(account: Account, role: Role) {
    try {
      const updated = await accountsApi.updateRole(account.id, role);
      setAccounts((a) => a.map((x) => (x.id === updated.id ? updated : x)));
      toast.success(`${updated.email} is now ${role}`);
    } catch (err) {
      onError(err);
    }
  }

  async function toggleEnabled(account: Account) {
    try {
      const updated = account.enabled
        ? await accountsApi.disable(account.id)
        : await accountsApi.enable(account.id);
      setAccounts((a) => a.map((x) => (x.id === updated.id ? updated : x)));
      toast.success(updated.enabled ? `${updated.email} enabled` : `${updated.email} disabled`);
    } catch (err) {
      onError(err);
    }
  }

  async function resetPassword(account: Account) {
    const trimmed = newPassword.trim();
    if (trimmed.length < 12) return;
    try {
      await accountsApi.resetPassword(account.id, trimmed);
      setResettingId(null);
      setNewPassword('');
      toast.success(`Password reset for ${account.email}`);
    } catch (err) {
      onError(err);
    }
  }

  async function removeAccount(account: Account) {
    try {
      await accountsApi.remove(account.id);
      refresh();
    } catch (err) {
      onError(err);
    }
  }

  return (
    <section className="card">
      <h3>Accounts</h3>
      <p className="muted hint">
        Every account&apos;s worlds are private to it, even from other admins — this only manages the
        roster itself (ADR-0109).
      </p>
      {error && <p className="error">{error}</p>}
      <ul className="article-list">
        {accounts.map((a) => (
          <li key={a.id} className="rel-row">
            <span>
              <strong>{a.email}</strong>{' '}
              <small className="muted">
                — {a.role}
                {!a.enabled && ' · disabled'}
              </small>
            </span>
            <select
              value={a.role}
              onChange={(e) => void changeRole(a, e.target.value as Role)}
              aria-label={`Role for ${a.email}`}
            >
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
            <Button type="button" variant="link" onClick={() => void toggleEnabled(a)}>
              {a.enabled ? 'Disable' : 'Enable'}
            </Button>
            {resettingId === a.id ? (
              <>
                <Input
                  type="password"
                  placeholder="New password (min 12 chars)"
                  minLength={12}
                  value={newPassword}
                  onChange={(e) => setNewPassword(e.target.value)}
                  autoFocus
                />
                <Button
                  type="button"
                  disabled={newPassword.trim().length < 12}
                  onClick={() => void resetPassword(a)}
                >
                  Save
                </Button>
                <Button
                  type="button"
                  variant="link"
                  onClick={() => {
                    setResettingId(null);
                    setNewPassword('');
                  }}
                >
                  Cancel
                </Button>
              </>
            ) : (
              <Button type="button" variant="link" onClick={() => setResettingId(a.id)}>
                Reset password
              </Button>
            )}
            <ConfirmDeleteDialog
              trigger={
                <Button variant="link" className="text-destructive hover:text-destructive">
                  ✕
                </Button>
              }
              title="Delete account?"
              description={`This permanently deletes ${a.email} and everything it owns.`}
              onConfirm={() => removeAccount(a)}
            />
          </li>
        ))}
        {loading && (
          <li className="muted loading-row">
            <Spinner /> Loading…
          </li>
        )}
        {!loading && accounts.length === 0 && <li className="muted">No accounts yet.</li>}
      </ul>
    </section>
  );
}
