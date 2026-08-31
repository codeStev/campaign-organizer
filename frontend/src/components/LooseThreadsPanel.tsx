import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import {
  looseThreadsApi,
  LooseThread,
  LooseThreadStatus,
  LOOSE_THREAD_STATUSES,
} from '../api/client';
import { Button } from './ui/button';
import { Input } from './ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from './ui/select';
import { Spinner } from './ui/spinner';
import { toast } from 'sonner';

interface Props {
  worldId: string;
  campaignId: string;
  sessionId: string;
  onError: (err: unknown) => void;
  readOnly?: boolean;
}

function statusLabel(status: LooseThreadStatus): string {
  return status.charAt(0) + status.slice(1).toLowerCase();
}

export function LooseThreadsPanel({ worldId, campaignId, sessionId, onError, readOnly = false }: Props) {
  const api = useMemo(
    () => looseThreadsApi(worldId, campaignId, sessionId),
    [worldId, campaignId, sessionId],
  );
  const [threads, setThreads] = useState<LooseThread[]>([]);
  const [loading, setLoading] = useState(true);
  const [text, setText] = useState('');

  const refresh = useCallback(async () => {
    try {
      setThreads(await api.list());
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function addThread(e: FormEvent) {
    e.preventDefault();
    const trimmed = text.trim();
    if (!trimmed) return;
    try {
      await api.create({ text: trimmed });
      setText('');
      await refresh();
      toast.success('Loose thread added');
    } catch (err) {
      onError(err);
    }
  }

  async function setStatus(thread: LooseThread, status: LooseThreadStatus) {
    try {
      await api.update(thread.id, { text: thread.text, status });
      await refresh();
      toast.success(`Marked ${status.toLowerCase()}`);
    } catch (err) {
      onError(err);
    }
  }

  async function removeThread(thread: LooseThread) {
    try {
      await api.remove(thread.id);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  if (readOnly) {
    return (
      <>
        <strong className="muted">Loose threads</strong>
        {loading && (
          <p className="muted loading-row">
            <Spinner /> Loading…
          </p>
        )}
        {!loading && threads.length === 0 && <p className="muted">No loose threads yet.</p>}
        {threads.length > 0 && (
          <ul className="beat-list">
            {threads.map((t) => (
              <li key={t.id} className="beat-item">
                <div className="beat-row">
                  <span>{t.text}</span>
                  <span className={`loose-thread-status lt-${t.status.toLowerCase()}`}>
                    {statusLabel(t.status)}
                  </span>
                </div>
              </li>
            ))}
          </ul>
        )}
      </>
    );
  }

  return (
    <div className="loose-threads">
      <span className="muted">Loose threads</span>
      <ul className="beat-list">
        {threads.map((t) => (
          <li key={t.id} className="beat-item">
            <div className="beat-row">
              <span>{t.text}</span>
              <span className="bf-spacer" />
              <Select value={t.status} onValueChange={(v) => setStatus(t, v as LooseThreadStatus)}>
                <SelectTrigger>
                  <SelectValue />
                </SelectTrigger>
                <SelectContent>
                  {LOOSE_THREAD_STATUSES.map((s) => (
                    <SelectItem key={s} value={s}>
                      {statusLabel(s)}
                    </SelectItem>
                  ))}
                </SelectContent>
              </Select>
              <Button
                type="button"
                variant="link"
                className="text-destructive hover:text-destructive"
                onClick={() => removeThread(t)}
              >
                ✕
              </Button>
            </div>
          </li>
        ))}
        {loading && (
          <li className="muted loading-row">
            <Spinner /> Loading…
          </li>
        )}
        {!loading && threads.length === 0 && <li className="muted">No loose threads yet.</li>}
      </ul>
      <form className="beat-form" onSubmit={addThread}>
        <Input
          placeholder="Something happened… (Enter to log it)"
          value={text}
          onChange={(e) => setText(e.target.value)}
        />
        <Button type="submit" disabled={!text.trim()}>
          Add
        </Button>
      </form>
    </div>
  );
}
