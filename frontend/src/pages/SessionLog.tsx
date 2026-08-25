import { FormEvent, useCallback, useEffect, useMemo, useState } from 'react';
import { sessionsApi, Session } from '../api/client';
import { SessionPacketView } from './SessionPacketView';
import { RecapView } from './RecapView';
import { MarkdownEditor } from '../components/MarkdownEditor';
import { renderMarkdown } from '../lib/markdown';
import { Button } from '../components/ui/button';
import { Input } from '../components/ui/input';

interface Props {
  worldId: string;
  campaignId: string;
  campaignName: string;
  onError: (err: unknown) => void;
}

interface Draft {
  id: string | null;
  title: string;
  sessionNumber: string;
  date: string;
  summary: string;
  notes: string;
}

const EMPTY: Draft = { id: null, title: '', sessionNumber: '', date: '', summary: '', notes: '' };

export function SessionLog({ worldId, campaignId, campaignName, onError }: Props) {
  const api = useMemo(() => sessionsApi(worldId, campaignId), [worldId, campaignId]);
  const [sessions, setSessions] = useState<Session[]>([]);
  const [loading, setLoading] = useState(true);
  const [draft, setDraft] = useState<Draft>(EMPTY);
  // Session whose print packet is open (null = none).
  const [packetSessionId, setPacketSessionId] = useState<string | null>(null);
  // FR-45: printable "story so far" recap (null = closed).
  const [recapOpen, setRecapOpen] = useState(false);

  const refresh = useCallback(async () => {
    try {
      setSessions(await api.list());
    } catch (err) {
      onError(err);
    } finally {
      setLoading(false);
    }
  }, [api, onError]);

  useEffect(() => {
    void refresh();
  }, [refresh]);

  async function save(e: FormEvent) {
    e.preventDefault();
    const body = {
      title: draft.title,
      sessionNumber: draft.sessionNumber ? Number(draft.sessionNumber) : null,
      date: draft.date || null,
      summary: draft.summary || null,
      notes: draft.notes || null,
    };
    try {
      if (draft.id) await api.update(draft.id, body);
      else await api.create(body);
      setDraft(EMPTY);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  async function remove(id: string) {
    try {
      await api.remove(id);
      if (draft.id === id) setDraft(EMPTY);
      await refresh();
    } catch (err) {
      onError(err);
    }
  }

  function edit(s: Session) {
    setDraft({
      id: s.id,
      title: s.title,
      sessionNumber: s.sessionNumber != null ? String(s.sessionNumber) : '',
      date: s.date ?? '',
      summary: s.summary ?? '',
      notes: s.notes ?? '',
    });
  }

  return (
    <section className="card">
      <h3 className="session-heading">
        Sessions
        <Button
          variant="link"
          onClick={() => setRecapOpen(true)}
          title="Print the story so far for the next session"
        >
          🖨 Recap
        </Button>
      </h3>
      <form className="session-form" onSubmit={save}>
        <div className="session-form-row">
          <Input
            type="number"
            className="num-input"
            placeholder="#"
            value={draft.sessionNumber}
            onChange={(e) => setDraft({ ...draft, sessionNumber: e.target.value })}
          />
          <Input
            type="date"
            value={draft.date}
            onChange={(e) => setDraft({ ...draft, date: e.target.value })}
          />
          <Input
            placeholder="Session title"
            value={draft.title}
            onChange={(e) => setDraft({ ...draft, title: e.target.value })}
            required
          />
        </div>
        <MarkdownEditor value={draft.summary} onChange={(summary) => setDraft({ ...draft, summary })} />
        <label className="sheet-article">
          <span className="muted">GM notes (private)</span>
          <MarkdownEditor value={draft.notes} onChange={(notes) => setDraft({ ...draft, notes })} />
        </label>
        <div className="editor-actions">
          <Button type="submit" disabled={!draft.title}>
            {draft.id ? 'Save session' : 'Add session'}
          </Button>
          {draft.id && (
            <Button type="button" variant="link" onClick={() => setDraft(EMPTY)}>
              Cancel
            </Button>
          )}
        </div>
      </form>

      <ol className="session-list">
        {sessions.map((s) => (
          <li key={s.id} className="session-item">
            <div className="session-meta">
              {s.sessionNumber != null && <span className="session-num">#{s.sessionNumber}</span>}
              {s.date && <span className="muted">{s.date}</span>}
            </div>
            <div className="session-body">
              <strong>{s.title}</strong>
              {s.summary && (
                <div
                  className="muted preview-body"
                  dangerouslySetInnerHTML={{ __html: renderMarkdown(s.summary) }}
                />
              )}
              <div className="editor-actions">
                <Button variant="link" onClick={() => edit(s)}>
                  Edit
                </Button>
                <Button
                  variant="link"
                  onClick={() => setPacketSessionId(s.id)}
                  title="Print a one-page prep packet for this session"
                >
                  🖨 Packet
                </Button>
                <Button
                  variant="link"
                  className="text-destructive hover:text-destructive"
                  onClick={() => remove(s.id)}
                >
                  Delete
                </Button>
              </div>
            </div>
          </li>
        ))}
        {loading && <li className="muted">Loading…</li>}
        {!loading && sessions.length === 0 && (
          <li className="muted">No sessions logged yet.</li>
        )}
      </ol>

      {packetSessionId && (
        <SessionPacketView
          worldId={worldId}
          campaignId={campaignId}
          sessionId={packetSessionId}
          onClose={() => setPacketSessionId(null)}
          onError={onError}
        />
      )}

      {recapOpen && (
        <RecapView
          worldId={worldId}
          campaignId={campaignId}
          campaignName={campaignName}
          onClose={() => setRecapOpen(false)}
          onError={onError}
        />
      )}
    </section>
  );
}
