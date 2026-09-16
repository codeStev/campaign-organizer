import { useEffect, useState } from 'react';
import { foundryApi, ApiError, FoundryConnectionTestResult } from '../api/client';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';
import { Spinner } from '../components/ui/spinner';
import { toast } from 'sonner';

interface Props {
  worldId: string;
  onAuthExpired: () => void;
}

/** Status of the "Test connection" button: in-flight or its latest outcome. */
type TestStatus =
  | { phase: 'testing' }
  | { phase: 'done'; result: FoundryConnectionTestResult }
  | { phase: 'failed'; message: string };

/** Per-world Foundry VTT relay settings (ADR-0115). Bring-your-own, always —
 * there is no shared or default relay; every field here is entered by this
 * world's own user, pointing at a Foundry relay they self-host. */
export function NextFoundrySettingsPage({ worldId, onAuthExpired }: Props) {
  const api = foundryApi(worldId);

  const [loading, setLoading] = useState(true);
  const [configured, setConfigured] = useState(false);
  const [relayBaseUrl, setRelayBaseUrl] = useState('');
  const [clientId, setClientId] = useState('');
  const [apiKey, setApiKey] = useState('');
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);
  const [test, setTest] = useState<TestStatus | null>(null);

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [worldId]);

  async function refresh() {
    setLoading(true);
    setError(null);
    try {
      const connection = await api.get();
      setConfigured(connection.configured);
      setRelayBaseUrl(connection.relayBaseUrl);
      setClientId(connection.clientId);
    } catch (err) {
      if (err instanceof ApiError && err.status === 404) {
        // No connection saved yet for this world — a normal, expected state.
        setConfigured(false);
        setRelayBaseUrl('');
        setClientId('');
      } else {
        handleError(err);
      }
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

  async function save() {
    setSaving(true);
    setError(null);
    try {
      const saved = await api.put({ relayBaseUrl, clientId, apiKey });
      setConfigured(saved.configured);
      setRelayBaseUrl(saved.relayBaseUrl);
      setClientId(saved.clientId);
      setApiKey('');
      toast.success('Foundry connection saved');
    } catch (err) {
      handleError(err);
    } finally {
      setSaving(false);
    }
  }

  async function runTest() {
    setTest({ phase: 'testing' });
    try {
      const result = await api.test();
      setTest({ phase: 'done', result });
    } catch (err) {
      if (err instanceof ApiError && err.status === 401) {
        onAuthExpired();
        return;
      }
      setTest({ phase: 'failed', message: err instanceof Error ? err.message : 'Request failed' });
    }
  }

  if (loading) {
    return (
      <p className="muted loading-row">
        <Spinner /> Loading…
      </p>
    );
  }

  return (
    <div className="wiki-main">
      <div className="card">
        <h3>Foundry VTT</h3>
        <p className="muted">
          Push articles and handouts into your own self-hosted Foundry VTT session. There is no
          shared relay — connect your own{' '}
          <a href="https://github.com/ThreeHats/foundryvtt-rest-api" target="_blank" rel="noreferrer">
            foundryvtt-rest-api
          </a>{' '}
          relay below.
        </p>
        {error && <p className="error">{error}</p>}
        <span className={`type-chip${configured ? ' active' : ''}`}>
          {configured ? 'Configured' : 'Not configured'}
        </span>
        <label>
          Relay base URL
          <Input
            placeholder="https://your-relay.example.com"
            value={relayBaseUrl}
            onChange={(e) => setRelayBaseUrl(e.target.value)}
          />
        </label>
        <label>
          Client ID
          <Input
            placeholder="The Foundry session's client id from your relay"
            value={clientId}
            onChange={(e) => setClientId(e.target.value)}
          />
        </label>
        <label>
          API key
          <Input
            type="password"
            placeholder={configured ? '•••• configured — leave blank to keep it' : 'Your relay API key'}
            value={apiKey}
            onChange={(e) => setApiKey(e.target.value)}
          />
        </label>
        <div className="editor-actions">
          <Button onClick={() => void save()} disabled={saving}>
            {saving ? 'Saving…' : 'Save'}
          </Button>
          <Button type="button" variant="outline" onClick={() => void runTest()} disabled={test?.phase === 'testing'}>
            Test connection
          </Button>
        </div>
        {test && (
          <p className={test.phase === 'failed' || (test.phase === 'done' && !test.result.ok) ? 'error' : 'muted'}>
            {test.phase === 'testing' && 'Testing…'}
            {test.phase === 'failed' && `✗ ${test.message}`}
            {test.phase === 'done' &&
              (test.result.ok
                ? `✓ Connected (${test.result.connectedClientIds.length} session(s) on this relay)`
                : `✗ ${test.result.error ?? 'Not connected'}`)}
          </p>
        )}
      </div>
    </div>
  );
}
