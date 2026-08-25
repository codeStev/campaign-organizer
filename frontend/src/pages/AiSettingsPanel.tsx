import { useEffect, useState } from 'react';
import { aiSettingsApi, AiProviderSetting, ApiError } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

const PROVIDER_LABEL: Record<string, string> = {
  groq: 'Groq',
  openrouter: 'OpenRouter',
};

interface Props {
  onAuthExpired: () => void;
}

export function AiSettingsPanel({ onAuthExpired }: Props) {
  const [providers, setProviders] = useState<AiProviderSetting[]>([]);
  const [loading, setLoading] = useState(true);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [saved, setSaved] = useState(false);

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  async function refresh() {
    setLoading(true);
    try {
      setProviders(await aiSettingsApi.get());
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
    setError(err instanceof Error ? err.message : 'Something went wrong');
  }

  function setModel(providerId: string, model: string) {
    setSaved(false);
    setProviders((ps) => ps.map((p) => (p.providerId === providerId ? { ...p, model: model || null } : p)));
  }

  function move(index: number, dir: -1 | 1) {
    const to = index + dir;
    if (to < 0 || to >= providers.length) return;
    setSaved(false);
    setProviders((ps) => {
      const next = [...ps];
      [next[index], next[to]] = [next[to], next[index]];
      return next;
    });
  }

  async function save() {
    setSaving(true);
    setError(null);
    try {
      const result = await aiSettingsApi.update(
        providers.map((p) => ({ providerId: p.providerId, model: p.model })),
      );
      setProviders(result);
      setSaved(true);
    } catch (err) {
      handleError(err);
    } finally {
      setSaving(false);
    }
  }

  if (loading) return <p className="muted">Loading…</p>;

  return (
    <div className="card">
      <h3>AI providers</h3>
      <p className="muted">
        Order sets fallback priority — the first configured provider that answers is used. API
        keys are set via environment variables, not here.
      </p>
      {error && <p className="error">{error}</p>}
      <ul className="settings-provider-list">
        {providers.map((p, i) => (
          <li key={p.providerId} className="rel-row">
            <div className="settings-provider-main">
              <strong>{PROVIDER_LABEL[p.providerId] ?? p.providerId}</strong>{' '}
              <span className={`type-chip${p.configured ? ' active' : ''}`}>
                {p.configured ? 'Configured' : 'Not configured'}
              </span>
              <Input
                placeholder={p.defaultModel}
                value={p.model ?? ''}
                onChange={(e) => setModel(p.providerId, e.target.value)}
              />
            </div>
            <div>
              <Button type="button" variant="link" onClick={() => move(i, -1)} disabled={i === 0} title="Try earlier">
                ↑
              </Button>
              <Button
                type="button"
                variant="link"
                onClick={() => move(i, 1)}
                disabled={i === providers.length - 1}
                title="Try later"
              >
                ↓
              </Button>
            </div>
          </li>
        ))}
      </ul>
      <Button onClick={save} disabled={saving}>
        {saving ? 'Saving…' : saved ? 'Saved' : 'Save'}
      </Button>
    </div>
  );
}
