import { FormEvent, useEffect, useState } from 'react';
import { toast } from 'sonner';
import { apiTokensApi, ApiError, ApiTokenScope, ApiTokenSummary } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { ConfirmDialog } from '../components/ConfirmDialog';

interface Props {
  onAuthExpired: () => void;
}

const SCOPES: { value: ApiTokenScope; label: string }[] = [
  { value: 'campaigns:read', label: 'List campaigns' },
  { value: 'glossary:read', label: "Read a campaign's glossary" },
  { value: 'sessions:write', label: 'Create sessions and write summaries' },
  { value: 'transcripts:write', label: 'Upload and delete transcripts' },
];

const EXPIRY_OPTIONS: { value: string; label: string; days: number | null }[] = [
  { value: 'never', label: 'Never expires', days: null },
  { value: '30', label: 'In 30 days', days: 30 },
  { value: '90', label: 'In 90 days', days: 90 },
  { value: '365', label: 'In 1 year', days: 365 },
];

function scopeLabel(scope: ApiTokenScope): string {
  return SCOPES.find((s) => s.value === scope)?.label ?? scope;
}

/**
 * Self-service API tokens for machine clients such as the DiscordWhispr recorder (ADR-0119). The
 * secret is shown exactly once, right after creation: only its hash is stored, so it can never be
 * displayed again. Changing the password or MFA signs every token out, since they all share the
 * account's token version.
 */
export function ApiTokensPanel({ onAuthExpired }: Props) {
  const [tokens, setTokens] = useState<ApiTokenSummary[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [creating, setCreating] = useState(false);
  const [name, setName] = useState('');
  const [scopes, setScopes] = useState<ApiTokenScope[]>([]);
  const [expiry, setExpiry] = useState('never');
  const [busy, setBusy] = useState(false);
  const [issuedSecret, setIssuedSecret] = useState<string | null>(null);
  const [revoking, setRevoking] = useState<string | null>(null);
  const [confirmRevokeId, setConfirmRevokeId] = useState<string | null>(null);

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refresh() {
    setLoading(true);
    try {
      setTokens(await apiTokensApi.list());
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

  function toggleScope(scope: ApiTokenScope, checked: boolean) {
    setScopes((current) => (checked ? [...current, scope] : current.filter((s) => s !== scope)));
  }

  function resetForm() {
    setCreating(false);
    setName('');
    setScopes([]);
    setExpiry('never');
  }

  async function handleCreate(event: FormEvent) {
    event.preventDefault();
    setBusy(true);
    setError(null);
    const days = EXPIRY_OPTIONS.find((o) => o.value === expiry)?.days ?? null;
    const expiresAt = days === null ? null : new Date(Date.now() + days * 86_400_000).toISOString();
    try {
      const created = await apiTokensApi.create({ name: name.trim(), scopes, expiresAt });
      setIssuedSecret(created.token);
      resetForm();
      await refresh();
    } catch (err) {
      handleError(err);
    } finally {
      setBusy(false);
    }
  }

  function copySecret() {
    if (!issuedSecret) return;
    navigator.clipboard
      .writeText(issuedSecret)
      .then(() => toast.success('Token copied'))
      .catch(() => toast.error('Could not copy the token'));
  }

  async function handleRevoke(id: string) {
    setRevoking(id);
    setError(null);
    try {
      await apiTokensApi.revoke(id);
      toast.success('Token revoked');
      await refresh();
    } catch (err) {
      handleError(err);
    } finally {
      setRevoking(null);
    }
  }

  return (
    <div>
      <p className="muted hint">
        For apps that act on your behalf, such as the DiscordWhispr recorder. A token can only do
        what you tick below, never manage your account. Changing your password or authenticator
        signs all tokens out.
      </p>
      {error && <p className="error">{error}</p>}

      {issuedSecret && (
        <div className="settings-list-row" role="status">
          <div>
            <strong>Copy your new token now</strong>
            <p className="muted">It won't be shown again.</p>
            <code>{issuedSecret}</code>
          </div>
          <div>
            <Button variant="outline" onClick={copySecret}>
              Copy
            </Button>
            <Button variant="link" onClick={() => setIssuedSecret(null)}>
              Done
            </Button>
          </div>
        </div>
      )}

      {loading ? (
        <p className="muted">Loading…</p>
      ) : tokens.length === 0 ? (
        <p className="muted">No API tokens.</p>
      ) : (
        <ul className="settings-list">
          {tokens.map((token) => (
            <li key={token.id} className="settings-list-row">
              <div>
                <strong>{token.name}</strong> · <code>{token.prefix}…</code>
                <p className="muted">{token.scopes.map(scopeLabel).join(', ')}</p>
                <p className="muted">
                  Created {new Date(token.createdAt).toLocaleDateString()}
                  {token.expiresAt && ` · expires ${new Date(token.expiresAt).toLocaleDateString()}`}
                  {' · '}
                  {token.lastUsedAt
                    ? `last used ${new Date(token.lastUsedAt).toLocaleString()}`
                    : 'never used'}
                </p>
              </div>
              <Button
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => setConfirmRevokeId(token.id)}
                disabled={revoking === token.id}
              >
                {revoking === token.id ? 'Revoking…' : 'Revoke'}
              </Button>
            </li>
          ))}
        </ul>
      )}

      {creating ? (
        <form onSubmit={handleCreate} className="flex flex-col items-start gap-2">
          <Input
            value={name}
            onChange={(e) => setName(e.target.value)}
            placeholder="Name, e.g. Discord recorder"
            aria-label="Token name"
            maxLength={100}
            required
            className="max-w-sm"
          />
          {SCOPES.map((scope) => (
            <label key={scope.value} className="checkbox-label">
              <input
                type="checkbox"
                checked={scopes.includes(scope.value)}
                onChange={(e) => toggleScope(scope.value, e.target.checked)}
              />
              {scope.label}
            </label>
          ))}
          <select value={expiry} onChange={(e) => setExpiry(e.target.value)} aria-label="Expiry">
            {EXPIRY_OPTIONS.map((option) => (
              <option key={option.value} value={option.value}>
                {option.label}
              </option>
            ))}
          </select>
          <div className="flex items-center gap-2">
            <Button type="submit" disabled={busy || name.trim() === '' || scopes.length === 0}>
              {busy ? 'Creating…' : 'Create token'}
            </Button>
            <Button type="button" variant="link" onClick={resetForm}>
              Cancel
            </Button>
          </div>
        </form>
      ) : (
        <Button variant="outline" onClick={() => setCreating(true)}>
          New token
        </Button>
      )}

      <ConfirmDialog
        open={confirmRevokeId !== null}
        onOpenChange={(open) => !open && setConfirmRevokeId(null)}
        title="Revoke this token?"
        description="Any app using it stops working immediately."
        confirmLabel="Revoke"
        destructive
        onConfirm={() => {
          const id = confirmRevokeId;
          setConfirmRevokeId(null);
          if (id) void handleRevoke(id);
        }}
      />
    </div>
  );
}
